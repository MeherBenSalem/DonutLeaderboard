#!/usr/bin/env python3
"""Store-quality DonutLeaderboard GUI captures (real Minecraft client)."""
from __future__ import annotations

import hashlib
import json
import os
import re
import sqlite3
import subprocess
import time
import uuid
from datetime import datetime, timezone, timedelta
from pathlib import Path

ROOT = Path("/workspace")
DOCS = ROOT / "docs" / "images"
ART = Path("/opt/cursor/artifacts/screenshots")
LOG = ROOT / "build/screenshot-attempt.log"
SERVER = ROOT / "build" / "screenshot-server"
JAR = ROOT / "build/libs/DonutLeaderboard-1.0.0.jar"
PAPER = ROOT / "build/smoke-servers/paper-1.20.1.jar"
PAPER.parent.mkdir(parents=True, exist_ok=True)
OPTIONS = Path.home() / ".minecraft/options.txt"
PORT_FILE = SERVER / "port.txt"
VIEWER_UUID = "bb77495a-a740-3169-a238-69654c8bd2c1"
VIEWER_NAME = "TestPlayer"

OUTPUTS = (
    "category-menu.png",
    "leaderboard-top.png",
    "leaderboard-weekly.png",
    "leaderboard-page2.png",
)

RESOLUTION = "1920x1080"
DISPLAY = ":99"


def log(msg: str) -> None:
    line = f"[store-capture] {datetime.now(timezone.utc).isoformat()} {msg}\n"
    LOG.parent.mkdir(parents=True, exist_ok=True)
    with LOG.open("a") as f:
        f.write(line)
    print(line, end="")


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def ensure_paper_jar() -> None:
    if PAPER.is_file() and PAPER.stat().st_size > 1_000_000:
        return
    log("Downloading Paper 1.20.1...")
    import urllib.request

    ua = "DonutLeaderboard-screenshots/1.0"
    req = urllib.request.Request(
        "https://fill.papermc.io/v3/projects/paper/versions/1.20.1/builds",
        headers={"User-Agent": ua},
    )
    builds = json.loads(urllib.request.urlopen(req).read())
    build = builds[-1]
    dl = build["downloads"].get("server:default") or next(iter(build["downloads"].values()))
    url = dl["url"]
    req = urllib.request.Request(url, headers={"User-Agent": ua})
    with urllib.request.urlopen(req) as resp, PAPER.open("wb") as out:
        out.write(resp.read())
    log(f"Paper jar ready ({PAPER.stat().st_size} bytes)")


def patch_options() -> None:
    OPTIONS.parent.mkdir(parents=True, exist_ok=True)
    lines: list[str] = []
    if OPTIONS.is_file():
        lines = OPTIONS.read_text().splitlines()
    keys = {
        "tutorialStep": "none",
        "hideTutorial": "true",
        "guiScale": "3",
        "autoJump": "false",
        "toggleCrouch": "false",
    }
    out: dict[str, str] = {}
    for line in lines:
        if ":" in line and not line.strip().startswith("#"):
            k, _, v = line.partition(":")
            out[k.strip()] = v.strip()
    out.update(keys)
    OPTIONS.write_text("\n".join(f"{k}:{v}" for k, v in sorted(out.items())) + "\n")
    log(f"Patched {OPTIONS} (tutorialStep:none, guiScale:3)")


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
    cur.execute("DELETE FROM dl_scores WHERE category_id = 'kills' AND period_type = 'WEEKLY'")
    cache: list[dict] = []
    for i in range(55):
        u = uuid.uuid4()
        names = ["Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Ghost", "Hotel"]
        name = f"{names[i % len(names)]}{i:02d}"
        val = float(2000 - i * 17.5)
        cur.execute(
            "INSERT INTO dl_scores (player_uuid, category_id, period_type, period_start, value, updated_at) "
            "VALUES (?,?,?,?,?,?)",
            (str(u), "kills", "WEEKLY", period_start, val, now_ms),
        )
        cache.append({"name": name, "uuid": str(u)})
    cur.execute(
        "INSERT OR REPLACE INTO dl_scores (player_uuid, category_id, period_type, period_start, value, updated_at) "
        "VALUES (?,?,?,?,?,?)",
        (VIEWER_UUID, "kills", "WEEKLY", period_start, 3.0, now_ms),
    )
    conn.commit()
    conn.close()
    uc = SERVER / "usercache.json"
    existing = json.loads(uc.read_text()) if uc.is_file() else []
    by_uuid = {e["uuid"]: e for e in existing if "uuid" in e}
    by_uuid[VIEWER_UUID] = {"name": VIEWER_NAME, "uuid": VIEWER_UUID}
    for e in cache:
        by_uuid[e["uuid"]] = e
    uc.write_text(json.dumps(list(by_uuid.values()), indent=2))
    log("Seeded 55 weekly kills rows + viewer at rank 56")


def server_cmd(line: str) -> None:
    pipe = SERVER / "cmd.pipe"
    if not pipe.exists():
        return
    with pipe.open("w") as f:
        f.write(line.strip() + "\n")


def server_as_player(command: str) -> None:
    server_cmd(f"execute as {VIEWER_NAME} run {command}")


def server_reload() -> None:
    server_cmd("lb refresh kills weekly")
    server_cmd("lb refresh kills weekly")


def ensure_server() -> int:
    latest = SERVER / "logs" / "latest.log"
    if latest.is_file() and "Done" in latest.read_text() and PORT_FILE.is_file():
        port = int(PORT_FILE.read_text().strip())
        subprocess.run(["cp", str(JAR), str(SERVER / "plugins" / JAR.name)], check=False)
        db = SERVER / "plugins" / "DonutLeaderboard" / "leaderboard.db"
        if db.is_file():
            seed_db(db)
            server_reload()
        return port

    log("Booting Paper screenshot server...")
    SERVER.mkdir(parents=True, exist_ok=True)
    (SERVER / "plugins").mkdir(exist_ok=True)
    subprocess.run(["cp", str(JAR), str(SERVER / "plugins" / JAR.name)], check=True)
    if not PAPER.is_file():
        ensure_paper_jar()
    subprocess.run(["cp", str(PAPER), str(SERVER / "server.jar")], check=True)
    import socket

    s = socket.socket()
    s.bind(("127.0.0.1", 0))
    port = s.getsockname()[1]
    s.close()
    PORT_FILE.write_text(str(port))
    (SERVER / "eula.txt").write_text("eula=true\n")
    (SERVER / "server.properties").write_text(
        f"server-port={port}\nonline-mode=false\nmax-players=30\nmotd=Screenshots\n"
    )
    pipe = SERVER / "cmd.pipe"
    if not pipe.exists():
        os.mkfifo(pipe)
    subprocess.Popen(
        f'cd "{SERVER}" && tail -f cmd.pipe | java -Xms768M -Xmx1536M -jar server.jar nogui >> server.log 2>&1',
        shell=True,
        executable="/bin/bash",
    )
    for _ in range(240):
        if latest.is_file() and "Done" in latest.read_text():
            db = SERVER / "plugins/DonutLeaderboard/leaderboard.db"
            if db.is_file():
                seed_db(db)
                time.sleep(2)
                server_reload()
                time.sleep(3)
            log(f"Server ready on {port}")
            return port
        time.sleep(2)
    raise RuntimeError("Server boot timeout")


def window_geometry(wid: str, env: dict) -> tuple[int, int, int, int]:
    r = subprocess.run(
        ["xdotool", "getwindowgeometry", "--shell", wid],
        capture_output=True,
        text=True,
        env=env,
    )
    vals = dict(re.findall(r"^(X|Y|WIDTH|HEIGHT)=(.+)$", r.stdout, re.M))
    return int(vals.get("X", 0)), int(vals.get("Y", 0)), int(vals["WIDTH"]), int(vals["HEIGHT"])


def click_slot(wid: str, env: dict, slot: int) -> None:
    _, _, w, h = window_geometry(wid, env)
    gui_scale = 1
    for candidate in (3, 2, 1):
        if 298 * candidate <= h * 0.92:
            gui_scale = candidate
            break
    x_size = 176 * gui_scale
    y_size = 298 * gui_scale
    left = (w - x_size) // 2
    top = (h - y_size) // 2
    col = slot % 9
    row = slot // 9
    slot_px = 18 * gui_scale
    x = left + 8 * gui_scale + col * slot_px + slot_px // 2
    y = top + 8 * gui_scale + row * slot_px + slot_px // 2
    subprocess.run(["xdotool", "windowfocus", wid], env=env, check=False)
    subprocess.run(["xdotool", "mousemove", "--window", wid, str(int(x)), str(int(y))], env=env, check=False)
    time.sleep(0.05)
    subprocess.run(["xdotool", "click", "1"], env=env, check=False)


def resume_if_paused(wid: str, env: dict) -> None:
    _, _, w, h = window_geometry(wid, env)
    subprocess.run(["xdotool", "windowfocus", wid], env=env, check=False)
    subprocess.run(["xdotool", "mousemove", "--window", wid, str(w // 2), str(h // 3)], env=env, check=False)
    subprocess.run(["xdotool", "click", "1"], env=env, check=False)
    time.sleep(1.0)


def dismiss_tutorial(wid: str, env: dict) -> None:
    return


def chat_cmd(wid: str, env: dict, cmd: str) -> None:
    subprocess.run(["xdotool", "windowfocus", wid], env=env, check=False)
    subprocess.run(["xdotool", "key", "t"], env=env, check=False)
    time.sleep(0.35)
    subprocess.run(["xclip", "-selection", "clipboard"], input=cmd, text=True, check=False)
    subprocess.run(["xdotool", "key", "ctrl+v"], env=env, check=False)
    time.sleep(0.15)
    subprocess.run(["xdotool", "key", "Return"], env=env, check=False)
    time.sleep(3.5)


def capture_window(wid: str, env: dict, raw: Path) -> bool:
    subprocess.run(["xdotool", "windowfocus", wid], env=env, check=False)
    time.sleep(0.45)
    subprocess.run(["import", "-window", wid, str(raw)], env=env, check=False)
    return raw.is_file() and raw.stat().st_size > 5000


def crop_chest(raw: Path, out: Path) -> None:
    r = subprocess.run(
        ["identify", "-format", "%w %h", str(raw)],
        capture_output=True,
        text=True,
        check=False,
    )
    if r.returncode != 0:
        return
    w, h = map(int, r.stdout.split())
    gui_scale = 1
    for candidate in (3, 2, 1):
        if 298 * candidate <= h * 0.92:
            gui_scale = candidate
            break
    cw = 176 * gui_scale
    ch = 222 * gui_scale
    cx = (w - cw) // 2
    cy = (h - 298 * gui_scale) // 2
    geom = f"{cw}x{ch}+{cx}+{cy}"
    subprocess.run(
        ["convert", str(raw), "-crop", geom, "+repage", "-filter", "Lanczos", "-resize", "520x", str(out)],
        check=False,
    )


def crop_period_row(raw: Path, out: Path) -> None:
    r = subprocess.run(
        ["identify", "-format", "%w %h", str(raw)],
        capture_output=True,
        text=True,
        check=False,
    )
    if r.returncode != 0:
        return
    w, h = map(int, r.stdout.split())
    gui_scale = 1
    for candidate in (3, 2, 1):
        if 298 * candidate <= h * 0.92:
            gui_scale = candidate
            break
    cw = 176 * gui_scale
    ch = 18 * gui_scale
    cx = (w - cw) // 2
    cy = (h - 298 * gui_scale) // 2 + 8 * gui_scale + 5 * 18 * gui_scale
    geom = f"{cw}x{ch}+{cx}+{cy}"
    subprocess.run(
        ["convert", str(raw), "-crop", geom, "+repage", "-filter", "Lanczos", "-resize", "520x", str(out)],
        check=False,
    )


def run_capture(port: int) -> None:
    env = os.environ.copy()
    env["PATH"] = "/home/ubuntu/.local/bin:" + env.get("PATH", "")
    env["DISPLAY"] = DISPLAY
    subprocess.run(["pkill", "-f", f"Xvfb {DISPLAY}"], stderr=subprocess.DEVNULL)
    subprocess.Popen(
        ["Xvfb", DISPLAY, "-screen", "0", f"{RESOLUTION}x24"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    time.sleep(2)

    DOCS.mkdir(parents=True, exist_ok=True)
    for old in (
        "leaderboard-populated.png",
        "category-menu-alt.png",
        "blocks-broken-leaderboard.png",
    ):
        p = DOCS / old
        if p.is_file():
            p.unlink()

    mc = subprocess.Popen(
        [
            "portablemc",
            "start",
            "1.20.1",
            "-u",
            VIEWER_NAME,
            "-i",
            VIEWER_UUID,
            "-s",
            "127.0.0.1",
            "-p",
            str(port),
            "--resolution",
            RESOLUTION,
        ],
        env=env,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    wid = None
    for _ in range(240):
        r = subprocess.run(
            ["xdotool", "search", "--name", "Minecraft"],
            capture_output=True,
            text=True,
            env=env,
        )
        if r.stdout.strip():
            wid = r.stdout.strip().splitlines()[0]
            break
        time.sleep(2)
    if not wid:
        mc.terminate()
        raise RuntimeError("Minecraft window not found")

    log(f"Client window {wid}")
    time.sleep(20)
    resume_if_paused(wid, env)
    for key in ("w", "a", "s", "d", "space"):
        subprocess.run(["xdotool", "key", key], env=env, check=False)
        time.sleep(0.12)

    raw_dir = DOCS / "_raw"
    raw_dir.mkdir(exist_ok=True)

    server_as_player("lb")
    time.sleep(2.0)
    raw_menu = raw_dir / "menu.png"
    if not capture_window(wid, env, raw_menu):
        raise RuntimeError("Failed menu capture")
    crop_chest(raw_menu, DOCS / "category-menu.png")
    log(f"category-menu.png {sha(DOCS / 'category-menu.png')[:12]}")

    server_as_player("lb open kills weekly")
    time.sleep(2.0)
    raw_top = raw_dir / "top.png"
    if not capture_window(wid, env, raw_top):
        raise RuntimeError("Failed top capture")
    if sha(raw_top) == sha(raw_menu):
        raise RuntimeError("Leaderboard did not open (top raw identical to menu)")
    if raw_top.stat().st_size < 400_000:
        raise RuntimeError(f"Leaderboard capture too small ({raw_top.stat().st_size} bytes)")
    mean = subprocess.run(
        ["convert", str(raw_top), "-crop", "40x40+492+180", "-format", "%[mean]", "info:"],
        capture_output=True,
        text=True,
        check=False,
    )
    try:
        if float(mean.stdout.strip()) < 25_000:
            raise RuntimeError("Leaderboard capture does not look like an open GUI")
    except ValueError:
        pass
    crop_chest(raw_top, DOCS / "leaderboard-top.png")
    crop_period_row(raw_top, DOCS / "leaderboard-weekly.png")
    log(f"leaderboard-top.png {sha(DOCS / 'leaderboard-top.png')[:12]}")
    log(f"leaderboard-weekly.png {sha(DOCS / 'leaderboard-weekly.png')[:12]}")

    click_slot(wid, env, 53)
    time.sleep(1.0)
    raw_p2 = raw_dir / "page2.png"
    if not capture_window(wid, env, raw_p2):
        raise RuntimeError("Failed page2 capture")
    if sha(raw_p2) == sha(raw_top):
        raise RuntimeError("Pagination click failed (page2 raw identical to top)")
    crop_chest(raw_p2, DOCS / "leaderboard-page2.png")
    log(f"leaderboard-page2.png {sha(DOCS / 'leaderboard-page2.png')[:12]}")

    mc.terminate()


def verify_outputs() -> None:
    paths = [DOCS / n for n in OUTPUTS]
    for p in paths:
        if not p.is_file() or p.stat().st_size < 800:
            raise RuntimeError(f"Missing or tiny: {p.name}")
    hashes = [sha(p) for p in paths]
    if len(set(hashes)) != 4:
        raise RuntimeError(f"SHA-256 collision among outputs: {dict(zip(OUTPUTS, hashes))}")
    for name, h in zip(OUTPUTS, hashes):
        log(f"verify {name} sha256={h}")


def sync_artifacts() -> None:
    ART.mkdir(parents=True, exist_ok=True)
    for name in OUTPUTS:
        subprocess.run(["cp", str(DOCS / name), str(ART / name)], check=True)


def main() -> int:
    patch_options()
    ensure_paper_jar()
    try:
        port = ensure_server()
        run_capture(port)
        verify_outputs()
        sync_artifacts()
    except Exception as ex:
        log(f"FAIL: {ex}")
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
