"""Envia un archivo de Python a la ventana de Blender abierta con bridge_server.py y muestra su salida."""
import json
import socket
import sys


def amj_main():
    token_file, script = sys.argv[1], sys.argv[2]
    with open(token_file, encoding="utf-8") as f:
        token = f.read().strip()
    with open(script, encoding="utf-8") as f:
        code = f.read()
    with socket.create_connection(("127.0.0.1", 9876), timeout=30) as s:
        s.settimeout(590)
        s.sendall((json.dumps({"token": token, "code": code, "name": script}) + "\n").encode("utf-8"))
        data = b""
        while not data.endswith(b"\n"):
            chunk = s.recv(1 << 20)
            if not chunk:
                break
            data += chunk
    reply = json.loads(data)
    sys.stdout.write(reply["output"])
    sys.exit(0 if reply["ok"] else 1)


amj_main()
