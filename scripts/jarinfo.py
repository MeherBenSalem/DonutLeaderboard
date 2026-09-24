import hashlib, pathlib
p = pathlib.Path("/workspace/build/libs/DonutLeaderboard-1.0.0.jar")
if not p.exists():
    print("MISSING")
else:
    data = p.read_bytes()
    print("size", len(data))
    print("sha256", hashlib.sha256(data).hexdigest())
