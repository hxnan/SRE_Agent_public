import os
import json
import asyncio
from mcp.client.sse import sse_client
from mcp import ClientSession


def _normalize(content):
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts = []
        for item in content:
            if isinstance(item, str):
                parts.append(item)
            elif isinstance(item, dict):
                t = item.get("text") or item.get("content") or ""
                parts.append(t if isinstance(t, str) else json.dumps(item, ensure_ascii=False))
            elif hasattr(item, "text") and isinstance(getattr(item, "text"), str):
                parts.append(getattr(item, "text"))
            else:
                parts.append(str(item))
        return "\n".join([p for p in parts if p])
    if isinstance(content, dict):
        t = content.get("text") or content.get("content")
        return t if isinstance(t, str) else json.dumps(content, ensure_ascii=False)
    if hasattr(content, "text") and isinstance(getattr(content, "text"), str):
        return getattr(content, "text")
    try:
        return json.dumps(content, ensure_ascii=False)
    except Exception:
        return str(content)


async def main():
    url = (os.getenv("PROM_MCP_SSE_URL") or "http://localhost:8000/mcp").rstrip("/")
    headers_env = os.getenv("PROM_MCP_HEADERS")
    headers = {}
    if headers_env:
        try:
            headers = json.loads(headers_env)
        except Exception:
            headers = {}
    async with sse_client(url, headers=headers) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            tools = await session.list_tools()
            print("tools:", tools)
            try:
                result = await session.call_tool("execute_query", {"query": "up", "time": None})
                content = getattr(result, "content", result)
                print(_normalize(content))
            except Exception as e:
                print("call_tool error:", str(e))


if __name__ == "__main__":
    asyncio.run(main())
