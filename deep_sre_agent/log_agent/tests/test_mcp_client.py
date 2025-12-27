import os
import sys
import unittest
import importlib.util


class TestLokiMCPClient(unittest.TestCase):
    def setUp(self):
        self._env_backup = os.environ.copy()

    def tearDown(self):
        os.environ.clear()
        os.environ.update(self._env_backup)

    def test_auto_switch_to_http_when_sse_url_missing(self):
        os.environ.pop("LOKI_MCP_MODE", None)
        os.environ["LOKI_MCP_URL"] = "http://example/mcp"
        os.environ.pop("LOKI_MCP_SSE_URL", None)

        spec = importlib.util.spec_from_file_location(
            "mcp_client_testmod",
            os.path.join(os.path.dirname(__file__), "..", "mcp_client.py")
        )
        mod = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = mod
        spec.loader.exec_module(mod)
        mod.http_client = object()
        client = mod.LokiMCPClient()
        self.assertEqual(client.mode, "http")

    def test_fallback_to_http_on_sse_failure(self):
        os.environ["LOKI_MCP_MODE"] = "sse"
        os.environ["LOKI_MCP_URL"] = "http://example/mcp"
        os.environ["LOKI_MCP_SSE_URL"] = "http://example/sse"

        async def sse_fail(*args, **kwargs):
            raise RuntimeError("sse fail")

        class DummyHTTP:
            def __init__(self, *args, **kwargs):
                pass

            async def __aenter__(self):
                return (object(), object())

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
            "mcp_client_testmod2",
            os.path.join(os.path.dirname(__file__), "..", "mcp_client.py")
        )
        mod = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = mod
        spec.loader.exec_module(mod)
        # patch module-level symbols
        mod.sse_client = sse_fail
        mod.http_client = lambda *args, **kwargs: DummyHTTP()
        mod.ClientSession = lambda *args, **kwargs: DummySession()
        client = mod.LokiMCPClient()
        out = client.query_logs("{job=\"app\"}")
        self.assertEqual(out, "ok")
        self.assertEqual(client.mode, "http")


if __name__ == "__main__":
    unittest.main()
