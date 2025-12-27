from typing_extensions import Annotated
from langchain_core.tools import InjectedToolArg, tool
from .mcp_client import MySQLMCPClient


client = MySQLMCPClient()


@tool()
def mysql_execute(
    sql: str,
) -> str:
    """执行 SQL 语句或查询（支持多条语句，以分号分隔）"""
    return client.execute(sql)


@tool()
def mysql_search_objects(
    object_type: str,
    pattern: Annotated[str | None, InjectedToolArg] = "%",
    schema: Annotated[str | None, InjectedToolArg] = None,
    detail_level: Annotated[str | None, InjectedToolArg] = "names",
    limit: Annotated[int | None, InjectedToolArg] = 100,
) -> str:
    """搜索数据库对象（schema/table/column/procedure/index），支持模式、库名、详情级别与结果上限"""
    return client.search_objects(object_type, pattern, schema, detail_level, limit)
