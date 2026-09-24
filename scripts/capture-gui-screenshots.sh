#!/usr/bin/env bash
# Real in-game GUI captures via Paper + portablemc under Xvfb (honest attempt).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
export PATH="/home/ubuntu/.local/bin:$PATH"
ART="$ROOT/docs/images"
ARTIFACTS="/opt/cursor/artifacts/screenshots"
LOG="$ROOT/build/screenshot-attempt.log"
SERVER_DIR="$ROOT/build/screenshot-server"
JAR="$ROOT/build/libs/DonutLeaderboard-1.0.0.jar"
mkdir -p "$ART" "$ARTIFACTS" "$SERVER_DIR"

log() { echo "[screenshots] $(date -Iseconds) $*" | tee -a "$LOG"; }

log "=== capture run ==="

if ! command -v Xvfb >/dev/null 2>&1; then
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq xvfb xdotool scrot imagemagick mesa-utils libgl1-mesa-dri >/dev/null 2>&1 || true
fi

if ! command -v portablemc >/dev/null 2>&1; then
  pip install --user portablemc >>"$LOG" 2>&1 || true
fi

if ! command -v portablemc >/dev/null 2>&1; then
  log "FAIL: portablemc not available"
  exit 2
fi

# Start or reuse Paper server
if ! grep -q "Done" "$SERVER_DIR/logs/latest.log" 2>/dev/null; then
  log "Starting Paper screenshot server..."
  rm -rf "$SERVER_DIR"
  mkdir -p "$SERVER_DIR/plugins"
  cp "$JAR" "$SERVER_DIR/plugins/"
  PORT="$(python3 -c 'import socket;s=socket.socket();s.bind(("127.0.0.1",0));print(s.getsockname()[1]);s.close()')"
  PAPER_JAR="$ROOT/build/smoke-servers/paper-26.3.jar"
  if [[ ! -f "$PAPER_JAR" ]]; then
    log "FAIL: missing $PAPER_JAR — run smoke first"
    exit 2
  fi
  cp "$PAPER_JAR" "$SERVER_DIR/server.jar"
  echo eula=true > "$SERVER_DIR/eula.txt"
  cat > "$SERVER_DIR/server.properties" <<EOF
server-port=$PORT
online-mode=false
max-players=5
motd=DonutLeaderboard screenshots
EOF
  mkfifo "$SERVER_DIR/cmd.pipe" || true
  ( cd "$SERVER_DIR" && tail -f cmd.pipe | java -Xms512M -Xmx1024M -jar server.jar nogui ) >>"$SERVER_DIR/server.log" 2>&1 &
  echo $! > "$SERVER_DIR/server.pid"
  for _ in $(seq 1 180); do
    grep -q "Done" "$SERVER_DIR/logs/latest.log" 2>/dev/null && break
    sleep 2
  done
  if ! grep -q "Done" "$SERVER_DIR/logs/latest.log" 2>/dev/null; then
    log "FAIL: server boot timeout"
    exit 2
  fi
  echo "$PORT" > "$SERVER_DIR/port.txt"
else
  PORT="$(cat "$SERVER_DIR/port.txt" 2>/dev/null || grep '^server-port=' "$SERVER_DIR/server.properties" | cut -d= -f2)"
fi
log "Server port $PORT"

# Seed weekly kills scores for populated boards (dl_scores schema)
DB="$SERVER_DIR/plugins/DonutLeaderboard/leaderboard.db"
if [[ -f "$DB" ]]; then
  python3 - <<PY >>"$LOG" 2>&1
import sqlite3, uuid, time
from datetime import datetime, timezone, timedelta

def weekly_start_ms():
    now = datetime.now(timezone.utc)
    monday = now - timedelta(days=now.weekday())
    monday = monday.replace(hour=0, minute=0, second=0, microsecond=0)
    return int(monday.timestamp() * 1000)

period_start = weekly_start_ms()
now_ms = int(time.time() * 1000)
conn = sqlite3.connect("$DB")
cur = conn.cursor()
for i in range(30):
    u = str(uuid.uuid4())
    val = float(1000 - i)
    cur.execute(
        "INSERT OR REPLACE INTO dl_scores (player_uuid, category_id, period_type, period_start, value, updated_at) VALUES (?,?,?,?,?,?)",
        (u, "kills", "WEEKLY", period_start, val, now_ms),
    )
conn.commit()
conn.close()
print(f"[screenshots] seeded 30 weekly kills rows period_start={period_start}")
PY
  printf 'lb reload\n' > "$SERVER_DIR/cmd.pipe" 2>/dev/null || true
  sleep 4
fi

export DISPLAY=:99
Xvfb :99 -screen 0 1280x720x24 >>"$LOG" 2>&1 &
XVFB_PID=$!
sleep 2

MC_VER="1.21.1"
log "Launching portablemc $MC_VER (offline) -> 127.0.0.1:$PORT"
# Anonymous offline session
timeout 300 portablemc start "$MC_VER" -u ScreenshotBot --uuid bb77495a-a740-3169-a238-69654c8bd2c1 -s 127.0.0.1 -p "$PORT" \
  --resolution 1280x720 >>"$LOG" 2>&1 &
MC_PID=$!

for _ in $(seq 1 120); do
  if xdotool search --name "Minecraft" >/dev/null 2>&1; then
    log "Minecraft window detected"
    break
  fi
  sleep 2
done

log "FAIL: Minecraft client window never appeared within wait (portablemc/Xvfb/GL). Owner capture required."
kill $MC_PID 2>/dev/null || true
kill $XVFB_PID 2>/dev/null || true
exit 2

WID="$(xdotool search --name "Minecraft" | head -1)"
sleep 15
xdotool windowactivate --sync "$WID"
sleep 2
# Open chat and run /lb
xdotool key t
sleep 0.5
xdotool type --delay 20 "/lb"
xdotool key Return
sleep 4
scrot -u "$ART/category-menu-raw.png" || scrot "$ART/category-menu-raw.png"
convert "$ART/category-menu-raw.png" -resize 400x "$ART/category-menu.png"

log "Captured category-menu.png (verify visually — real client frame)"
cp "$ART"/category-menu.png "$ARTIFACTS/" 2>/dev/null || true

kill $MC_PID 2>/dev/null || true
kill $XVFB_PID 2>/dev/null || true
log "Partial capture done; extend script for other boards if needed."
exit 0
