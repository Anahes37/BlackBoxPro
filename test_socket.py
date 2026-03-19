import socket, json, uuid, sys, time

def write_varint(sock, value):
    while True:
        if value & ~0x7F == 0:
            sock.send(bytes([value]))
            return
        sock.send(bytes([value & 0x7F | 0x80]))
        value >>= 7

def read_varint(sock):
    result, shift = 0, 0
    while True:
        b = sock.recv(1)
        if not b:
            raise EOFError()
        b = b[0]
        result |= (b & 0x7F) << shift
        if b & 0x80 == 0:
            return result
        shift += 7

def send_command(sock, action, params=None):
    msg = json.dumps({"id": str(uuid.uuid4()), "action": action, "params": params or {}, "delay": 0})
    data = msg.encode("utf-8")
    write_varint(sock, len(data))
    sock.send(data)
    print(f"-> {action}")

def read_response(sock):
    length = read_varint(sock)
    data = b""
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            raise EOFError()
        data += chunk
    resp = json.loads(data.decode("utf-8"))
    status = resp.get("status", "?")
    msg = resp.get("message", "")
    print(f"<- [{status}] {msg}")
    if resp.get("data"):
        print(f"   data keys: {list(resp['data'].keys())}")
    return resp

def main():
    print("Connecting to 127.0.0.1:25580...")
    s = socket.socket()
    s.settimeout(10)
    try:
        s.connect(("127.0.0.1", 25580))
    except Exception as e:
        print(f"Connection failed: {e}")
        sys.exit(1)
    print("Connected!\n")

    # Test 1: query_player_state
    print("=== Test 1: query_player_state ===")
    send_command(s, "query_player_state")
    read_response(s)
    print()

    # Test 2: swing_arm
    print("=== Test 2: swing_arm ===")
    send_command(s, "swing_arm", {"hand": "main_hand"})
    read_response(s)
    print()

    # Test 3: player_look
    print("=== Test 3: player_look ===")
    send_command(s, "player_look", {"yaw": 90.0, "pitch": -30.0, "onGround": True})
    read_response(s)
    print()

    # Test 4: unknown action
    print("=== Test 4: unknown_action ===")
    send_command(s, "nonexistent_action")
    read_response(s)
    print()

    print("All tests done.")
    s.close()

if __name__ == "__main__":
    main()
