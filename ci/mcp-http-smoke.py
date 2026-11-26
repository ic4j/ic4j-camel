"""Smoke-test the CI-only loopback instance of the Motoko MCP sample."""
import http.client
import json
import time
import uuid

VERSION = "2025-06-18"
session = None


def post(method, params, request_id=None):
    payload = {"jsonrpc": "2.0", "method": method, "params": params}
    if request_id is not None:
        payload["id"] = request_id
    headers = {
        "Content-Type": "application/json",
        "Accept": "application/json, text/event-stream",
        "MCP-Protocol-Version": VERSION,
    }
    if session:
        headers["Mcp-Session-Id"] = session
    conn = http.client.HTTPConnection("127.0.0.1", 13000, timeout=10)
    try:
        conn.request("POST", "/mcp", json.dumps(payload), headers)
        response = conn.getresponse()
        sid = response.getheader("Mcp-Session-Id")
        if request_id is None:
            assert response.status in (202, 204), f"{method}: HTTP {response.status}"
            assert not response.read(), f"{method}: notification returned a body"
            return None, sid
        assert response.status == 200, f"{method}: HTTP {response.status}"
        if "text/event-stream" in (response.getheader("Content-Type") or ""):
            data = []
            while line := response.readline():
                if line.startswith(b"data:"):
                    data.append(line[5:].strip())
                elif data and not line.strip():
                    break
            raw = b"\n".join(data)
        else:
            raw = response.read()
        assert raw, f"{method}: empty response"
        output = json.loads(raw)
        assert output.get("jsonrpc") == "2.0" and output.get("id") == request_id, output
        assert "error" not in output and isinstance(output.get("result"), dict), output
        return output["result"], sid
    finally:
        conn.close()


def tool_text(name, arguments, request_id):
    result, _ = post("tools/call", {"name": name, "arguments": arguments}, request_id)
    assert not result.get("isError"), result
    values = [x["text"] for x in result["content"] if x.get("type") == "text"]
    assert len(values) == 1, result
    return values[0]


for _ in range(35):
    try:
        init, session = post("initialize", {
            "protocolVersion": VERSION,
            "capabilities": {},
            "clientInfo": {"name": "camel-ci", "version": "1"},
        }, "init")
        break
    except ConnectionRefusedError:
        time.sleep(1)
else:
    raise RuntimeError("Local MCP HTTP endpoint never started")

assert init["protocolVersion"] == VERSION, init
assert "tools" in init["capabilities"], init
post("notifications/initialized", {})
catalog, _ = post("tools/list", {}, "list")
assert {"greet", "get-name"} <= {item["name"] for item in catalog["tools"]}, catalog
payload = f"mcp-ci-{uuid.uuid4()}"
assert tool_text("get-name", {}, "before") != payload
assert tool_text("greet", {"name": payload}, "greet") == f"Hello, {payload}!"
assert tool_text("get-name", {}, "after") == payload
print("PASS: MCP HTTP initialize, tools/list, greet UPDATE and get-name QUERY")
