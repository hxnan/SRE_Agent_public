from typing_extensions import Annotated
from langchain_core.tools import InjectedToolArg, tool
from .mcp_client import DeepWikiMCPClient


client = DeepWikiMCPClient()


@tool()
def wiki_read_structure(
    repo_url: Annotated[str | None, InjectedToolArg] = None,
) -> str:
    """读取仓库的 Wiki 结构概览"""
    return client.read_wiki_structure(repo_url)


@tool()
def wiki_read_contents(
    repo_url: Annotated[str | None, InjectedToolArg] = None,
    paths: Annotated[list[str] | None, InjectedToolArg] = None,
) -> str:
    """读取指定 Wiki 页面内容或关键页面摘要"""
    return client.read_wiki_contents(repo_url, paths)


@tool()
def wiki_ask_question(
    question: str,
    repo_url: Annotated[str | None, InjectedToolArg] = None,
) -> str:
    """就仓库提出问题并获得结合上下文的回答"""
    return client.ask_question(question, repo_url)
