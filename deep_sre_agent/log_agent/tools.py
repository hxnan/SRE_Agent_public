from typing import Any
from typing_extensions import Annotated
from langchain_core.tools import InjectedToolArg, tool
from .mcp_client import LokiMCPClient


client = LokiMCPClient()


@tool()
def log_query(
    query: str,
    start: Annotated[str | None, InjectedToolArg] = None,
    end: Annotated[str | None, InjectedToolArg] = None,
    limit: Annotated[int | None, InjectedToolArg] = None,
    labels: Annotated[dict[str, str] | None, InjectedToolArg] = None,
) -> str:
    """执行 Loki 查询语句，支持时间范围与标签过滤"""
    return client.query_logs(query, start, end, limit, labels)
