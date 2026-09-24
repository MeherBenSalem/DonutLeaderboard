#!/usr/bin/env python3
"""Re-capture DonutLeaderboard GUIs if PNGs missing; sync to artifacts."""
import hashlib
import json
import os
import socket
import sqlite3
import subprocess
import time
import uuid
from datetime import datetime, timezone, timedelta
from pathlib import Path

ROOT = Path("/workspace")
DOCS = ROOT / "docs" / "images"
LOG = ROOT / "build" / "screenshot-attempt.log"
SERVER = ROOT / "build" / "screenshot-server"
JAR = ROOT / "build" / "libs" / "DonutLeaderboard-1.0.0.jar"
PAPER = ROOT / "build" / "smoke-servers" / "paper-1.20.1.jar"
PORT_FILE = SERVER / "port.txt"
REQUIRED = [
    "category-menu.png",
    "leaderboard-populated.png",
    "leaderboard-weekly.png",
    "leaderboard-page2.png",
]


def log(msg: str) -> None:
    line = f"[recapture] {datetime.now(timezone.utc).isoformat()} {msg}\n"
    LOG.parent.mkdir(parents=True, exist_ok=True)
    with LOG.open("a") as f:
        f.write(line)
    print(line, end="")


def missing_names() -> list[str]:
    return [n for n in REQUIRED if not (DOCS / n).is_file() or (DOCS / n).stat().st_size < 500]


def free_port() -> int:
    s = socket.socket()
    s.bind(("127.0.0.1", 0))
    p = s.getsockname()[1]
    s.close()
    return p


def weekly_start_ms() -> int:
    now = datetime.now(timezone.utc)
    monday = now - timedelta(days=now.weekday())
    monday = monday.replace(hour=0, minute=0, second=0, microsecond=0)
    return int(monday.timestamp() * 1000)


def seed_db(db: Path) -> None:
    period_start = weekly_start_ms()
    now_ms = int(time.time() * 1000)
    conn = sqlite3.connect(db)
    cur = conn.cursor()
    cache_entries = []
    for i in range(30):
        u = uuid.uuid4()
        name = f"RankBot{i:02d}"
        val = float(1000 - i)
        cur.execute(
            "INSERT OR REPLACE INTO dl_scores (player_uuid, category_id, period_type, period_start, value, updated_at) "
            "VALUES (?,?,?,?,?,?)",
            (str(u), "kills", "WEEKLY", period_start, val, now_ms),
        )
        cache_entries.append({"name": name, "uuid": str(u)})
    conn.commit()
    conn.close()
    uc = SERVER / "usercache.json"
    existing = json.loads(uc.read_text()) if uc.is_file() else []
    by_uuid = {e["uuid"]: e for e in existing if "uuid" in e}
    for e in cache_entries:
        by_uuid[e["uuid"]] = e
    uc.write_text(json.dumps(list(by_uuid.values()), indent=2))


def ensure_server() -> int:
    if PORT_FILE.is_file():
        port = int(PORT_FILE.read_text().strip())
    else:
        port = 25566
    latest = SERVER / "logs" / "latest.log"
    if latest.is_file() and "Done" in latest.read_text():
        return port
    log("Starting Paper screenshot server...")
    SERVER.mkdir(parents=True, exist_ok=True)
    (SERVER / "plugins").mkdir(exist_ok=True)
    subprocess.run(["cp", str(JAR), str(SERVER / "plugins" / JAR.name)], check=True)
    subprocess.run(["cp", str(PAPER), str(SERVER / "server.jar")], check=True)
    port = free_port()
    PORT_FILE.write_text(str(port))
    (SERVER / "eula.txt").write_text("eula=true\n")
    (SERVER / "server.properties").write_text(
        f"server-port={port}\nonline-mode=false\nmax-players=20\nmotd=Screenshots\n"
    )
    pipe = SERVER / "cmd.pipe"
    if not pipe.exists():
        os.mkfifo(pipe)
    subprocess.Popen(
        f'cd "{SERVER}" && tail -f cmd.pipe | java -Xms512M -Xmx1024M -jar server.jar nogui >> server.log 2>&1',
        shell=True,
        executable="/bin/bash",
    )
    for _ in range(180):
        if latest.is_file() and "Done" in latest.read_text():
            log(f"Server ready on port {port}")
            db = SERVER / "plugins" / "DonutLeaderboard" / "leaderboard.db"
            if db.is_file():
                seed_db(db)
                with pipe.open("w") as f:
                    f.write("lb reload\n")
            return port
        time.sleep(2)
    raise RuntimeError("Server boot timeout")


def shot_chat(wid: str, env: dict, command: str, out: Path, resize: str = "400x") -> bool:
    subprocess.run(["xdotool", "windowactivate", "--sync", wid], env=env, check=False)
    for _ in range(2):
        subprocess.run(["xdotool", "key", "Escape"], env=env, check=False)
        time.sleep(0.2)
    if command:
        subprocess.run(["xdotool", "key", "t"], env=env, check=False)
        time.sleep(0.2)
        subprocess.run(["xdotool", "type", "--delay", "12", command], env=env, check=False)
        subprocess.run(["xdotool", "key", "Return"], env=env, check=False)
        time.sleep(3.5)
    raw = out.with_name("_raw_" + out.name)
    subprocess.run(["scrot", "-u", str(raw)], env=env, check=False)
    if not raw.is_file():
        return False
    subprocess.run(["convert", str(raw), "-resize", resize, str(out)], check=False)
    return out.is_file() and out.stat().st_size > 500


def click_gui_slot(wid: str, env: dict, slot: int) -> None:
    """Rough click for 6x9 chest GUI centered on 1280x720."""
    col = slot % 9
    row = slot // 9
    # Empirical offsets for portablemc 1280x720 window 2097159-style layout
    x = 640 + (col - 4) * 36
    y = 360 + (row - 2) * 36
    subprocess.run(["xdotool", "windowactivate", "--sync", wid], env=env, check=False)
    subprocess.run(["xdotool", "mousemove", "--window", wid, str(x), str(y)], env=env, check=False)
    subprocess.run(["xdotool", "click", "1"], env=env, check=False)


def run_capture(port: int) -> None:
    env = os.environ.copy()
    env["PATH"] = "/home/ubuntu/.local/bin:" + env.get("PATH", "")
    env["DISPLAY"] = ":99"
    subprocess.run(["pkill", "-f", "Xvfb :99"], stderr=subprocess.DEVNULL)
    subprocess.Popen(["Xvfb", ":99", "-screen", "0", "1280x720x24"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(2)
    DOCS.mkdir(parents=True, exist_ok=True)
    ART.mkdir(parents=True, exist_ok=True)
    mc = subprocess.Popen(
        [
            "portablemc",
            "start",
            "1.20.1",
            "-u",
            "TestPlayer",
            "-s",
            "127.0.0.1",
            "-p",
            str(port),
            "--resolution",
            "1280x720",
        ],
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    wid = None
    for _ in range(180):
        r = subprocess.run(["xdotool", "search", "--name", "Minecraft"], capture_output=True, text=True, env=env)
        if r.stdout.strip():
            wid = r.stdout.strip().split()[0]
            break
        time.sleep(2)
    if not wid:
        log("FAIL: no Minecraft window")
        mc.terminate()
        raise RuntimeError("No Minecraft window")
    log(f"Minecraft window {wid}")
    time.sleep(15)
    if not (DOCS / "category-menu.png").is_file() or (DOCS / "category-menu.png").stat().st_size < 500:
        subprocess.run(["xdotool", "windowactivate", "--sync", wid], env=env, check=False)
        subprocess.run(["xdotool", "key", "t"], env=env, check=False)
        time.sleep(0.3)
        subprocess.run(["xdotool", "type", "--delay", "15", "/lb"], env=env, check=False)
        subprocess.run(["xdotool", "key", "Return"], env=env, check=False)
        time.sleep(3)
        raw = DOCS / "_raw-menu.png"
        subprocess.run(["scrot", "-u", str(raw)], env=env, check=False)
        if raw.is_file():
            subprocess.run(
                ["convert", str(raw), "-resize", "400x", str(DOCS / "category-menu.png")],
                check=False,
            )
            log(f"Wrote category-menu.png sha256={hashlib.sha256((DOCS/'category-menu.png').read_bytes()).hexdigest()[:16]}")

    if not (DOCS / "leaderboard-populated.png").is_file():
        shot_chat(wid, env, "/lb", DOCS / "_tmp_menu.png")
        time.sleep(0.5)
        click_gui_slot(wid, env, 11)  # kills category
        time.sleep(2.0)
        click_gui_slot(wid, env, 47)  # -> Daily
        time.sleep(0.4)
        click_gui_slot(wid, env, 47)  # -> Weekly
        time.sleep(1.5)
        subprocess.run(["xdotool", "windowactivate", "--sync", wid], env=env, check=False)
        raw_p = DOCS / "_raw_populated.png"
        subprocess.run(["scrot", "-u", str(raw_p)], env=env, check=False)
        if raw_p.is_file():
            subprocess.run(["convert", str(raw_p), "-resize", "400x", str(DOCS / "leaderboard-populated.png")], check=False)
            log("Wrote leaderboard-populated.png")
    if not (DOCS / "leaderboard-weekly.png").is_file():
        src = DOCS / "leaderboard-populated.png"
        if src.is_file():
            subprocess.run(["cp", str(src), str(DOCS / "leaderboard-weekly.png")], check=False)
            log("Wrote leaderboard-weekly.png")
    if not (DOCS / "leaderboard-page2.png").is_file():
        shot_chat(wid, env, "/lb", DOCS / "_tmp_menu2.png")
        time.sleep(0.5)
        click_gui_slot(wid, env, 11)
        time.sleep(1.5)
        click_gui_slot(wid, env, 47)
        time.sleep(0.3)
        click_gui_slot(wid, env, 47)
        time.sleep(0.8)
        click_gui_slot(wid, env, 53)
        time.sleep(1.5)
        raw2 = DOCS / "_raw_page2.png"
        subprocess.run(["scrot", "-u", str(raw2)], env=env, check=False)
        if raw2.is_file():
            subprocess.run(["convert", str(raw2), "-resize", "400x", str(DOCS / "leaderboard-page2.png")], check=False)
            log("Wrote leaderboard-page2.png")

    mc.terminate()


def main() -> int:
    missing = missing_names()
    if not missing:
        log("All required PNGs present")
        return 0
    log(f"Missing PNGs: {missing}")
    try:
        port = ensure_server()
        db = SERVER / "plugins" / "DonutLeaderboard" / "leaderboard.db"
        if db.is_file():
            seed_db(db)
        run_capture(port)
    except Exception as ex:
        log(f"Capture failed: {ex}")
        return 1
    missing = missing_names()
    if missing:
        log(f"Still missing: {missing}")
    return 0 if not missing_names() else 1


if __name__ == "__main__":
    raise SystemExit(main())
