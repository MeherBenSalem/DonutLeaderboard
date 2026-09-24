#!/usr/bin/env python3
import subprocess, pathlib
out = pathlib.Path("/workspace/build/portablemc-help.txt")
env = {"PATH": "/home/ubuntu/.local/bin:/usr/bin:/bin"}
r = subprocess.run(["portablemc", "--help"], capture_output=True, text=True, env=env)
out.write_text(r.stdout + r.stderr)
