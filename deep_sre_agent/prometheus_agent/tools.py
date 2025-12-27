from typing_extensions import Annotated
from langchain_core.tools import InjectedToolArg, tool
from .mcp_client import PrometheusMCPClient


client = PrometheusMCPClient()


@tool()
def prom_query_range(
    promql: str,
    start: Annotated[str | None, InjectedToolArg] = None,
    end: Annotated[str | None, InjectedToolArg] = None,
    step: Annotated[str | None, InjectedToolArg] = None,
) -> str:
    """执行 PromQL 范围查询（query/start/end/step）"""
    return client.query_range(promql, start, end, step)


@tool()
def prom_query_instant(
    promql: str,
    time: Annotated[str | None, InjectedToolArg] = None,
) -> str:
    """执行 PromQL 即时查询（query/time）"""
    return client.query_instant(promql, time)


@tool()
def prom_list_targets() -> str:
    """列出抓取目标与健康状态"""
    return client.list_targets()

@tool()
def prom_list_metrics(
    limit: Annotated[int | None, InjectedToolArg] = None,
    offset: Annotated[int | None, InjectedToolArg] = None,
    filter_pattern: Annotated[str | None, InjectedToolArg] = None,
) -> str:
    """按过滤条件列出指标（limit/offset/filter_pattern）"""
    return client.list_metrics(limit, offset, filter_pattern)

@tool()
def prom_metadata(
    metric: str,
) -> str:
    """查询指标元数据与帮助信息"""
    return client.metadata(metric)

@tool()
def prom_health_check() -> str:
    """检查 MCP 服务健康（health_check）"""
    return client.health_check()
