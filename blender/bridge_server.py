"""
Puente local para trabajar sobre una ventana de Blender abierta.

Se arranca con:
  blender --python blender/bridge_server.py -- --token-file <archivo>

Solo escucha en 127.0.0.1 y exige el token aleatorio guardado en ese archivo. El codigo recibido se ejecuta en el hilo
principal de Blender, de uno en uno, y la salida se devuelve al cliente (blender/bridge_send.py).
"""
import contextlib
import io
import json
import secrets
import socket
import sys
import traceback

import bpy

PORT = 9876


def option(name):
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    return argv[argv.index(name) + 1] if name in argv else None


TOKEN = secrets.token_hex(24)
with open(option("--token-file"), "w", encoding="utf-8") as f:
    f.write(TOKEN)

server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(("127.0.0.1", PORT))
server.listen(1)
server.setblocking(False)
namespace = {"bpy": bpy}


def serve(conn):
    conn.settimeout(30)
    data = b""
    while not data.endswith(b"\n"):
        chunk = conn.recv(1 << 20)
        if not chunk:
            return
        data += chunk
    request = json.loads(data)
    if not secrets.compare_digest(str(request.get("token", "")), TOKEN):
        conn.sendall(b'{"ok": false, "output": "token incorrecto"}\n')
        return
    out = io.StringIO()
    ok = True
    try:
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
            exec(compile(request["code"], request.get("name", "<puente>"), "exec"), namespace)
    except Exception:
        ok = False
        out.write(traceback.format_exc())
    conn.sendall((json.dumps({"ok": ok, "output": out.getvalue()}) + "\n").encode("utf-8"))


def poll():
    try:
        conn, _ = server.accept()
    except (BlockingIOError, OSError):
        return 0.15
    with conn:
        try:
            serve(conn)
        except Exception:
            traceback.print_exc()
    return 0.05


bpy.app.timers.register(poll, first_interval=0.5, persistent=True)
print("Puente de Blender activo en 127.0.0.1:%d" % PORT)
