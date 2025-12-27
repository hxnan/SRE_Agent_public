from mcp_client import DeepWikiMCPClient


def test_normalize() -> None:
    c = DeepWikiMCPClient(repo_url="dummy")
    assert c._normalize("x") == "x"
    assert c._normalize([{"text": "a"}, {"content": "b"}]) == "a\nb"


def test_resolve_repo_error() -> None:
    c = DeepWikiMCPClient(repo_url=None)
    try:
        c._resolve_repo(None)
    except ValueError:
        pass
    else:
        raise AssertionError("expected ValueError")


if __name__ == "__main__":
    test_normalize()
    test_resolve_repo_error()
    c = DeepWikiMCPClient(repo_url="owner/repo")
    s = c.read_wiki_structure()
    print("read_wiki_structure (no network):", isinstance(s, str))
    c.close_sync()
    print("verify_mcp_client: OK")
