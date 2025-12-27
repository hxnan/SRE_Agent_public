import os
import json
import asyncio
import unittest
import logging
import traceback
from mcp.client.streamable_http import streamablehttp_client
from mcp import ClientSession

class TestPrintMySQLMCPTools(unittest.TestCase):
    def test_print_tools(self):
        logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
        url = os.getenv("MYSQL_MCP_STREAMABLE_URL") or "http://localhost:18081/mcp"
        headers_env = os.getenv("MYSQL_MCP_HEADERS")
        headers = {}
        if headers_env:
            try:
                headers = json.loads(headers_env)
            except Exception:
                headers = {}
        logging.info(f"MYSQL_MCP_STREAMABLE_URL={url}")
        logging.info(f"MYSQL_MCP_HEADERS={json.dumps(headers, ensure_ascii=False)}")
        if not url:
            print("MYSQL_MCP_STREAMABLE_URL 未设置，跳过连接")
            return
        try:
            async def main():
                async with streamablehttp_client(url, headers=headers) as (read_stream, write_stream, _):
                    async with ClientSession(read_stream, write_stream) as session:
                        await session.initialize()
                        tools_resp = await session.list_tools()
                        out = []
                        tools_iter = getattr(tools_resp, "tools", tools_resp)
                        for t in tools_iter:
                            name = getattr(t, "name", None)
                            schema = getattr(t, "input_schema", None) or getattr(t, "inputSchema", None) or {}
                            out.append({"name": name, "schema": schema})
                        return out
            out = asyncio.run(main())
            print(json.dumps(out, ensure_ascii=False))
            logging.info(f"工具数量={len(out)}")
            self.assertTrue(isinstance(out, list))
        except Exception as e:
            logging.error(f"MySQL MCP 连接或列举工具失败: {str(e)}")
            traceback.print_exc()
            self.skipTest(f"连接 MCP 失败，跳过：{str(e)}")
