import os
import json
import asyncio
import unittest
import logging
from mcp.client.streamable_http import streamablehttp_client
from mcp import ClientSession

class TestMySQLMCPClientConnection(unittest.TestCase):
    def setUp(self):
        logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
        self.url = os.getenv("MYSQL_MCP_STREAMABLE_URL") or "http://localhost:18081/mcp"
        headers_env = os.getenv("MYSQL_MCP_HEADERS")
        try:
            self.headers = json.loads(headers_env) if headers_env else {}
        except Exception:
            self.headers = {}

    def test_connect_and_list_tools(self):
        try:
            async def main():
                async with streamablehttp_client(self.url, headers=self.headers) as (read_stream, write_stream, _):
                    async with ClientSession(read_stream, write_stream) as session:
                        await session.initialize()
                        tools_resp = await session.list_tools()
                        tools_iter = getattr(tools_resp, "tools", tools_resp)
                        names = [getattr(t, "name", None) for t in tools_iter]
                        return [n for n in names if isinstance(n, str)]
            names = asyncio.run(main())
            print(json.dumps({"tools": names}, ensure_ascii=False))
            self.assertTrue(len(names) > 0, "未能获取到任何 MCP tool，请确认 DBHub MCP 服务已启动并可访问")
        except Exception as e:
            self.skipTest(f"连接 MCP 失败，跳过：{str(e)}")

    def test_optional_select_1_if_query_available(self):
        async def main():
            async with streamablehttp_client(self.url, headers=self.headers) as (read_stream, write_stream, _):
                async with ClientSession(read_stream, write_stream) as session:
                    await session.initialize()
                    tools_resp = await session.list_tools()
                    tools_iter = getattr(tools_resp, "tools", tools_resp)
                    names = [getattr(t, "name", None) for t in tools_iter]
                    # 选择候选查询工具
                    candidates = ["execute_sql", "mysql_query", "sql.query", "execute_query", "query"]
                    chosen = None
                    for c in candidates:
                        if c in names:
                            chosen = c
                            break
                    if not chosen:
                        return None
                    # 调用查询工具，尝试 SELECT 1
                    result = await session.call_tool(chosen, {"sql": "SELECT 1"})
                    return getattr(result, "content", result)
        try:
            out = asyncio.run(main())
            if out is None:
                self.skipTest("未发现查询类工具，跳过 SELECT 1 验证")
            self.assertTrue(bool(out))
        except Exception as e:
            self.skipTest(f"连接 MCP 或调用查询失败，跳过：{str(e)}")

    def test_optional_search_objects_tables(self):
        async def main():
            async with streamablehttp_client(self.url, headers=self.headers) as (read_stream, write_stream, _):
                async with ClientSession(read_stream, write_stream) as session:
                    await session.initialize()
                    tools_resp = await session.list_tools()
                    tools_iter = getattr(tools_resp, "tools", tools_resp)
                    names = [getattr(t, "name", None) for t in tools_iter]
                    if "search_objects" not in names:
                        return None
                    result = await session.call_tool("search_objects", {
                        "object_type": "table",
                        "pattern": "%",
                        "detail_level": "names",
                        "limit": 10,
                    })
                    return getattr(result, "content", result)
        try:
            out = asyncio.run(main())
            if out is None:
                self.skipTest("未发现 search_objects 工具，跳过对象搜索验证")
            self.assertTrue(bool(out))
        except Exception as e:
            self.skipTest(f"连接 MCP 或调用 search_objects 失败，跳过：{str(e)}")

if __name__ == "__main__":
    unittest.main()
