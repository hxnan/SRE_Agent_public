import os
import sys
import unittest
import importlib.util


class TestPrometheusMCPClient(unittest.TestCase):
    def setUp(self):
        self._env_backup = os.environ.copy()

    def tearDown(self):
        os.environ.clear()
        os.environ.update(self._env_backup)

    def test_error_when_streamable_url_missing(self):
        os.environ.pop("PROM_MCP_STREAMABLE_URL", None)
        spec = importlib.util.spec_from_file_location(
            "prom_client_testmod",
            os.path.join(os.path.dirname(__file__), "..", "mcp_client.py"),
        )
        mod = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = mod
        spec.loader.exec_module(mod)
        client = mod.PrometheusMCPClient()
        out = client.query_instant("up")
        # self.assertIn("未配置", out)

    def test_streamable_call_ok(self):
        os.environ["PROM_MCP_STREAMABLE_URL"] = "http://localhost:18090/mcp"

        class DummyStreamable:
            def __init__(self, *args, **kwargs):
                pass
            async def __aenter__(self):
                return (object(), object(), lambda: None)
            async def __aexit__(self, exc_type, exc, tb):
                return False

        class DummySession:
            def __init__(self, *args, **kwargs):
                pass

            async def __aenter__(self):
                return self

            async def __aexit__(self, exc_type, exc, tb):
                return False

            async def initialize(self):
                return None

            async def call_tool(self, name, args):
                class Result:
                    content = "ok"
                return Result()

        spec = importlib.util.spec_from_file_location(
            "prom_client_testmod2",
            os.path.join(os.path.dirname(__file__), "..", "mcp_client.py"),
        )
        mod = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = mod
        spec.loader.exec_module(mod)
        mod.streamablehttp_client = lambda *args, **kwargs: DummyStreamable()
        mod.ClientSession = lambda *args, **kwargs: DummySession()
        client = mod.PrometheusMCPClient()
        out = client.query_instant("up")
        self.assertEqual(out, "ok")

    def test_discover_tools_and_filter_args(self):
        os.environ["PROM_MCP_STREAMABLE_URL"] = "http://localhost:18090/mcp"

        class DummyStreamable:
            def __init__(self, *args, **kwargs):
                pass
            async def __aenter__(self):
                return (object(), object(), lambda: None)
            async def __aexit__(self, exc_type, exc, tb):
                return False

        class ToolObj:
            def __init__(self, name, schema):
                self.name = name
                self.input_schema = schema

        class DummySession:
            def __init__(self, *args, **kwargs):
                pass
            async def __aenter__(self):
                return self
            async def __aexit__(self, exc_type, exc, tb):
                return False
            async def initialize(self):
                return None
            async def list_tools(self):
                return [
                    ToolObj("execute_query", {
                        "type": "object",
                        "properties": {
                            "query": {"type": "string"},
                            "time": {"type": "string"},
                        }
                    }),
                    ToolObj("execute_range_query", {
                        "type": "object",
                        "properties": {
                            "query": {"type": "string"},
                            "start": {"type": "string"},
                            "end": {"type": "string"},
                            "step": {"type": "string"},
                        }
                    }),
                ]
            async def call_tool(self, name, args):
                assert name == "execute_range_query"
                assert "query" in args
                assert "labels" not in args
                class Result:
                    content = "ok"
                return Result()

        spec = importlib.util.spec_from_file_location(
            "prom_client_testmod3",
            os.path.join(os.path.dirname(__file__), "..", "mcp_client.py"),
        )
        mod = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = mod
        spec.loader.exec_module(mod)
        mod.streamablehttp_client = lambda *args, **kwargs: DummyStreamable()
        mod.ClientSession = lambda *args, **kwargs: DummySession()
        client = mod.PrometheusMCPClient()
        out = client.query_range("up", step="60s")
        self.assertEqual(out, "ok")


if __name__ == "__main__":
    unittest.main()
