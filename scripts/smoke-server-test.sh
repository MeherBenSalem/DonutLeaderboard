#!/usr/bin/env bash
# Boots real Paper/Purpur/Folia servers with DonutLeaderboard and verifies clean enable.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SMOKE_ROOT="${SMOKE_ROOT:-$ROOT/build/smoke-servers}"
JAR="$(ls "$ROOT"/build/libs/DonutLeaderboard-*.jar 2>/dev/null | head -1)"
REPORT="$ROOT/build/smoke-report.txt"
ENABLED="${DONUT_LEADERBOARD_SMOKE_ENABLED:-true}"
UA="DonutLeaderboard-smoke/1.0 (Nightbeam Studio)"

declare -A RESULT=()
declare -A LOG_EXCERPT=()

log() {
  echo "[smoke] $*" >> "$REPORT"
  echo "[smoke] $*" >&2
}

mkdir -p "$SMOKE_ROOT"
: > "$REPORT"

if [[ "$ENABLED" != "true" ]]; then
  log "Smoke disabled (DONUT_LEADERBOARD_SMOKE_ENABLED=$ENABLED)."
  exit 0
fi

if [[ -z "$JAR" ]]; then
  log "FAIL: plugin jar missing. Run ./gradlew shadowJar first."
  exit 1
fi

ensure_java() {
  local major="$1"
  case "$major" in
    17)
      if [[ -x /usr/lib/jvm/java-17-openjdk-amd64/bin/java ]]; then
        echo /usr/lib/jvm/java-17-openjdk-amd64/bin/java
        return
      fi
      log "Installing OpenJDK 17..."
      sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-17-jdk >&2
      echo /usr/lib/jvm/java-17-openjdk-amd64/bin/java
      ;;
    21)
      echo "$(command -v java)"
      ;;
    25)
      local j25="$SMOKE_ROOT/jdks/temurin-25"
      if [[ -x "$j25/bin/java" ]]; then
        echo "$j25/bin/java"
        return
      fi
      mkdir -p "$SMOKE_ROOT/jdks"
      local archive="$SMOKE_ROOT/jdks/temurin25.tar.gz"
      log "Downloading Temurin JDK 25..."
      curl -fsSL -A "$UA" "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk" -o "$archive"
      tar -xzf "$archive" -C "$SMOKE_ROOT/jdks"
      local extracted
      extracted="$(find "$SMOKE_ROOT/jdks" -maxdepth 1 -type d -name 'jdk-*' | head -1)"
      rm -rf "$j25"
      mv "$extracted" "$j25"
      echo "$j25/bin/java"
      ;;
    *)
      echo "java"
      ;;
  esac
}

paper_url() {
  local version="$1"
  python3 - <<PY
import json, urllib.request
version = "$version"
ua = "$UA"
req = urllib.request.Request(f"https://fill.papermc.io/v3/projects/paper/versions/{version}/builds", headers={"User-Agent": ua})
builds = json.load(urllib.request.urlopen(req))
build = builds[-1]
dl = build["downloads"].get("server:default") or next(iter(build["downloads"].values()))
print(dl["url"])
PY
}

folia_url() {
  local version="$1"
  python3 - <<PY
import json, urllib.request
version = "$version"
ua = "$UA"
req = urllib.request.Request(f"https://fill.papermc.io/v3/projects/folia/versions/{version}/builds", headers={"User-Agent": ua})
builds = json.load(urllib.request.urlopen(req))
build = builds[-1]
dl = build["downloads"].get("server:default") or next(iter(build["downloads"].values()))
print(dl["url"])
PY
}

purpur_download_url() {
  local version="$1"
  python3 - <<PY
import json, urllib.request
version = "$version"
req = urllib.request.Request(f"https://api.purpurmc.org/v2/purpur/{version}", headers={"User-Agent": "$UA"})
meta = json.load(urllib.request.urlopen(req))
build = meta["builds"]["latest"]
print(f"https://api.purpurmc.org/v2/purpur/{version}/{build}/download")
PY
}

download_jar() {
  local url="$1"
  local dest="$2"
  if [[ -f "$dest" && "$(stat -c%s "$dest")" -gt 1000000 ]]; then
    return
  fi
  curl -fsSL -A "$UA" "$url" -o "$dest"
}

plugin_log_ok() {
  local logfile="$1"
  if grep -Ei "\[DonutLeaderboard\].*(ERROR|SEVERE|WARN|Exception)" "$logfile"; then
    return 1
  fi
  grep -qi "DonutLeaderboard enabled" "$logfile"
}

find_free_port() {
  python3 - <<'PY'
import socket
s = socket.socket()
s.bind(("127.0.0.1", 0))
print(s.getsockname()[1])
s.close()
PY
}

run_server() {
  local id="$1"
  local java_bin="$2"
  local server_jar="$3"
  local extra_config="${4:-}"
  local work="$SMOKE_ROOT/$id"
  rm -rf "$work"
  local port
  port="$(find_free_port)"
  mkdir -p "$work/plugins"
  cp "$JAR" "$work/plugins/"
  if [[ -n "$extra_config" && -f "$extra_config" ]]; then
    mkdir -p "$work/plugins/DonutLeaderboard"
    cp "$extra_config" "$work/plugins/DonutLeaderboard/config.yml"
  fi
  cp "$server_jar" "$work/server.jar"
  echo "eula=true" > "$work/eula.txt"
  cat > "$work/server.properties" <<EOF
server-port=$port
online-mode=false
enable-rcon=true
rcon.port=$((port + 10000))
rcon.password=smokepass
max-players=5
motd=DonutLeaderboard smoke $id
EOF

  local logfile="$work/server.log"
  : > "$logfile"
  rm -f "$work/cmd.pipe"
  mkfifo "$work/cmd.pipe"
  ( cd "$work" && tail -f cmd.pipe | "$java_bin" -Xms512M -Xmx1024M -jar server.jar nogui ) >>"$logfile" 2>&1 &
  local pid=$!
  echo "$pid" > "$work/server.pid"

  local ok=0
  for _ in $(seq 1 360); do
    if grep -qE "Done \(|Done!" "$logfile" 2>/dev/null; then
      ok=1
      break
    fi
    if ! kill -0 "$pid" 2>/dev/null; then
      break
    fi
    sleep 2
  done

  if [[ "$ok" -ne 1 ]]; then
    RESULT[$id]="FAIL (timeout/boot)"
    LOG_EXCERPT[$id]="$(tail -n 12 "$logfile")"
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    return
  fi

  if ! plugin_log_ok "$logfile"; then
    RESULT[$id]="FAIL (plugin enable/log)"
    LOG_EXCERPT[$id]="$(grep -i DonutLeaderboard "$logfile" | tail -n 6)"
    printf 'stop\n' > "$work/cmd.pipe" || true
    sleep 5
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
    return
  fi

  printf 'lb\n' > "$work/cmd.pipe" || true
  sleep 2
  printf 'lb reload\n' > "$work/cmd.pipe" || true
  sleep 4

  if ! plugin_log_ok "$logfile"; then
    RESULT[$id]="FAIL (console commands)"
    LOG_EXCERPT[$id]="$(grep -Ei 'DonutLeaderboard|Exception' "$logfile" | tail -n 10)"
  else
    RESULT[$id]="PASS"
    LOG_EXCERPT[$id]="$(grep -i DonutLeaderboard "$logfile" | tail -n 3)"
  fi

  printf 'stop\n' > "$work/cmd.pipe" || true
  sleep 10
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
    wait "$pid" 2>/dev/null || true
  fi
}

JAVA17="$(ensure_java 17)"
JAVA21="$(ensure_java 21)"
JAVA25="$(ensure_java 25)"

log "Using Java17=$JAVA17"
log "Using Java21=$JAVA21"
log "Using Java25=$JAVA25"

PAPER1201_URL="$(paper_url 1.20.1)"
PAPER1211_URL="$(paper_url 1.21.11)"
PAPER263_URL="$(paper_url 26.3)"
FOLIA262_URL="$(folia_url 26.2)"
PURPUR263_URL="$(purpur_download_url 26.3)"

download_jar "$PAPER1201_URL" "$SMOKE_ROOT/paper-1.20.1.jar"
download_jar "$PAPER1211_URL" "$SMOKE_ROOT/paper-1.21.11.jar"
download_jar "$PAPER263_URL" "$SMOKE_ROOT/paper-26.3.jar"
download_jar "$FOLIA262_URL" "$SMOKE_ROOT/folia-26.2.jar"
download_jar "$PURPUR263_URL" "$SMOKE_ROOT/purpur-26.3.jar"

SQLITE_CFG="$SMOKE_ROOT/config-sqlite.yml"
cp "$ROOT/src/main/resources/config.yml" "$SQLITE_CFG"

run_server "paper-1.20.1" "$JAVA17" "$SMOKE_ROOT/paper-1.20.1.jar"
run_server "paper-1.21.11" "$JAVA21" "$SMOKE_ROOT/paper-1.21.11.jar"
run_server "paper-26.3" "$JAVA25" "$SMOKE_ROOT/paper-26.3.jar"
run_server "purpur-26.3" "$JAVA25" "$SMOKE_ROOT/purpur-26.3.jar"
run_server "folia-26.2" "$JAVA25" "$SMOKE_ROOT/folia-26.2.jar"
run_server "paper-26.3-sqlite" "$JAVA25" "$SMOKE_ROOT/paper-26.3.jar" "$SQLITE_CFG"

MYSQL_CFG="$SMOKE_ROOT/config-mysql.yml"
MARIADB_PORT_FILE="$SMOKE_ROOT/mariadb.port"
rm -f "$MARIADB_PORT_FILE"
log "Starting embedded MariaDB for MySQL smoke..."
( cd "$ROOT" && ./gradlew -q embeddedMariaDb --args="$MARIADB_PORT_FILE" ) >>"$REPORT" 2>&1 &
MARIADB_PID=$!
mysql_ok=0
for _ in $(seq 1 120); do
  if [[ -s "$MARIADB_PORT_FILE" ]]; then
    mysql_ok=1
    break
  fi
  if ! kill -0 "$MARIADB_PID" 2>/dev/null; then
    break
  fi
  sleep 1
done
if [[ "$mysql_ok" -eq 1 ]]; then
  MYSQL_PORT="$(tr -d '[:space:]' < "$MARIADB_PORT_FILE")"
  cat > "$MYSQL_CFG" <<EOF
config-version: 2
storage:
  type: MYSQL
  sqlite:
    file: leaderboard.db
  mysql:
    host: 127.0.0.1
    port: $MYSQL_PORT
    database: donut_leaderboard
    username: root
    password: ""
    parameters: useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8
cache:
  refresh-seconds: 120
periods:
  timezone: UTC
messages:
  prefix: "<gold><bold>Leaderboard</bold> <dark_gray>» "
bstats:
  enabled: false
gui:
  rows: 6
  menu-title: "<gold><bold>Leaderboards"
  board-title: "<gold>%category% <gray>(%period%)"
  entries-per-page: 21
  show-viewer-rank-slot: 48
  period-toggle-slot: 47
  filler:
    material: GRAY_STAINED_GLASS_PANE
    name: " "
  entry-slots: [10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34]
categories:
  builtin:
    balance: { enabled: true, slot: 10 }
    kills: { enabled: true, slot: 11 }
    deaths: { enabled: true, slot: 12 }
    playtime: { enabled: true, slot: 13 }
    blocks_mined: { enabled: true, slot: 14 }
    mob_kills: { enabled: true, slot: 15 }
    animals_bred: { enabled: true, slot: 16 }
    fish_caught: { enabled: true, slot: 19 }
    damage_dealt: { enabled: true, slot: 20 }
  custom:
    example_tokens: { enabled: false, name: Tokens, placeholder: "%someplugin_tokens%", icon: EMERALD, slot: 22 }
EOF
  run_server "paper-26.3-mysql" "$JAVA25" "$SMOKE_ROOT/paper-26.3.jar" "$MYSQL_CFG"
else
  RESULT["paper-26.3-mysql"]="FAIL (embedded MariaDB)"
  LOG_EXCERPT["paper-26.3-mysql"]="Could not start MariaDB4j; see gradle log in report"
fi
kill "$MARIADB_PID" 2>/dev/null || true
wait "$MARIADB_PID" 2>/dev/null || true

log ""
log "=== Smoke matrix results ==="
fail=0
for id in paper-1.20.1 paper-1.21.11 paper-26.3 purpur-26.3 folia-26.2 paper-26.3-sqlite paper-26.3-mysql; do
  log "| $id | ${RESULT[$id]:-UNKNOWN} |"
  while IFS= read -r line; do
    log "  log> $line"
  done <<< "${LOG_EXCERPT[$id]:-}"
  if [[ "${RESULT[$id]:-}" != PASS ]]; then
    fail=1
  fi
done

if [[ "$fail" -ne 0 ]]; then
  log "Smoke FAILED"
  exit 1
fi
log "Smoke PASSED"
exit 0
