from mcp_client import LokiMCPClient


def test_normalize() -> None:
    c = LokiMCPClient()
    assert c._normalize("x") == "x"
    assert c._normalize([{"text": "a"}, {"content": "b"}]) == "a\nb"


def test_query_methods() -> None:
    c = LokiMCPClient()
    s1 = c.query_logs("{job=\"app\"}", limit=1)
    assert isinstance(s1, str)


if __name__ == "__main__":
    test_normalize()
    test_query_methods()
    print("verify_mcp_client: OK")
