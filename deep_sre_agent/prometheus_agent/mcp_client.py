import os
import json
import asyncio
from typing import Any, Dict, List, Optional
import logging
from contextlib import AsyncExitStack
from datetime import datetime, timedelta, timezone

from mcp import ClientSession
from mcp.client.streamable_http import streamablehttp_client


class PrometheusMCPClient:
    def __init__(
        self,
        base_url: Optional[str] = None,
        timeout: float = 30.0,
    ):
        self.base_url = (base_url or os.getenv("PROM_MCP_STREAMABLE_URL") or os.getenv("PROM_MCP_URL") or "").rstrip("/")
        self.base_url = self.base_url or "http://localhost:18090/mcp"
        self.timeout = timeout
        level = os.getenv("PROM_MCP_LOG_LEVEL", "INFO").upper()
        verbose = os.getenv("PROM_MCP_LOG_VERBOSE", "false").lower()
        self.logger = logging.getLogger("prometheus_mcp")
        if not self.logger.handlers:
            handler = logging.StreamHandler()
            formatter = logging.Formatter("%(asctime)s %(levelname)s %(message)s")
            handler.setFormatter(formatter)
            self.logger.addHandler(handler)
        self.logger.setLevel(getattr(logging, level, logging.INFO))
        self.verbose = verbose in ("1", "true", "yes", "on")
        headers_env = os.getenv("PROM_MCP_HEADERS")
        try:
            self.headers: Dict[str, str] = json.loads(headers_env) if headers_env else {}
        except Exception:
            self.headers = {}
            logging.getLogger("prometheus_mcp").warning("PROM_MCP_HEADERS 解析失败，已忽略该配置")

        self.tool_query = os.getenv("PROM_MCP_TOOL_QUERY", "prometheus_query")
        self.tool_targets = os.getenv("PROM_MCP_TOOL_TARGETS", "prometheus_targets")
        self.tool_metadata = os.getenv("PROM_MCP_TOOL_METADATA", "prometheus_metadata")
        self._discovered: bool = False
        self._tool_names: List[str] = []
        self._tool_schemas: Dict[str, Dict[str, Any]] = {}

        if not self.base_url:
            self.logger.warning("PROM_MCP_STREAMABLE_URL 未配置")

    def _truncate(self, s: str, n: int = 400) -> str:
        if not isinstance(s, str):
            return str(s)
        return s if len(s) <= n else s[:n] + "..."

    def _normalize(self, content: Any) -> str:
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            parts: List[str] = []
            for item in content:
                if isinstance(item, str):
                    parts.append(item)
                elif isinstance(item, dict):
                    t = item.get("text") or item.get("content") or ""
                    if isinstance(t, str):
                        parts.append(t)
                    else:
                        parts.append(json.dumps(self._convert_prometheus_timestamps(item), ensure_ascii=False))
                elif hasattr(item, "text") and isinstance(getattr(item, "text"), str):
                    parts.append(getattr(item, "text"))
                else:
                    parts.append(str(item))
            return "\n".join([p for p in parts if p])
        if isinstance(content, dict):
            t = content.get("text") or content.get("content")
            if isinstance(t, str):
                return t
            return json.dumps(self._convert_prometheus_timestamps(content), ensure_ascii=False)
        if hasattr(content, "text") and isinstance(getattr(content, "text"), str):
            return getattr(content, "text")
        try:
            return json.dumps(content, ensure_ascii=False)
        except Exception:
            return str(content)

    def _safe_json(self, obj: Any) -> str:
        try:
            return json.dumps(obj, ensure_ascii=False)
        except Exception:
            return str(obj)

    def _log_error(self, name: str, e: Exception, arguments: Dict[str, Any]) -> None:
        try:
            self.logger.exception(
                f"MCP call tool error name={name} type={type(e).__name__} message={self._truncate(str(e), 2000)}"
            )
        except Exception:
            self.logger.error(f"MCP call tool error name={name}", exc_info=True)
        try:
            subs = getattr(e, "exceptions", None)
            if subs:
                for i, sub in enumerate(subs):
                    self.logger.error(
                        f"MCP sub-exception[{i}] type={type(sub).__name__} message={self._truncate(str(sub), 2000)}"
                    )
        except Exception:
            pass
        try:
            preview_args = self._safe_json(arguments)
            self.logger.error(f"MCP call tool args={self._truncate(preview_args, 2000)}")
        except Exception:
            pass

    async def _call_tool_async(self, name: str, arguments: Dict[str, Any]) -> str:
        stack = AsyncExitStack()
        await stack.__aenter__()
        if not self.base_url:
            await stack.aclose()
            raise RuntimeError("PROM_MCP_STREAMABLE_URL 未配置")
        url = self.base_url
        self.logger.info(f"MCP StreamableHTTP connect url={url}")
        read, write, _ = await stack.enter_async_context(streamablehttp_client(url, headers=self.headers, timeout=self.timeout))
        session = await stack.enter_async_context(ClientSession(read, write))
        await session.initialize()
        args = dict(arguments)
        self.logger.info(f"MCP call url={url} name={name} mode=streamable-http")
        try:
            self.logger.info(f"MCP call arguments={json.dumps(args, ensure_ascii=False)}")
        except Exception:
            self.logger.info(f"MCP call arguments={args}")
        try:
            result = await session.call_tool(name, args)
        finally:
            await stack.aclose()
        content = getattr(result, "content", result)
        normalized = self._normalize(content)
        self.logger.info(f"MCP call normalized body={normalized}")
        return normalized

    def _call_tool(self, name: str, arguments: Dict[str, Any]) -> str:
        try:
            return asyncio.run(self._call_tool_async(name, arguments))
        except Exception as e:
            self._log_error(name, e, arguments)
            raise

    async def _discover_tools_async(self) -> None:
        stack = AsyncExitStack()
        await stack.__aenter__()
        if not self.base_url:
            await stack.aclose()
            raise RuntimeError("PROM_MCP_STREAMABLE_URL 未配置")
        url = self.base_url
        read, write, _ = await stack.enter_async_context(streamablehttp_client(url, headers=self.headers, timeout=self.timeout))
        session = await stack.enter_async_context(ClientSession(read, write))
        await session.initialize()
        tools_resp = await session.list_tools()
        await stack.aclose()
        names: List[str] = []
        schemas: Dict[str, Dict[str, Any]] = {}
        try:
            tools_iter = getattr(tools_resp, "tools", tools_resp)
            for t in tools_iter:
                name = getattr(t, "name", None)
                schema = getattr(t, "input_schema", None) or getattr(t, "inputSchema", None) or {}
                if isinstance(name, str):
                    names.append(name)
                    if isinstance(schema, dict):
                        schemas[name] = schema
            self._tool_names = names
            self._tool_schemas = schemas
            self._discovered = True
            preview = {"names": names, "schemas": schemas}
            self.logger.info(f"Prometheus MCP 可用工具: {json.dumps(preview, ensure_ascii=False)}")
        except Exception as e:
            self.logger.info(f"Prometheus MCP 可用工具解析失败: {str(e)}")
            self._tool_names = []
            self._tool_schemas = {}
            self._discovered = True

    def _ensure_discovered(self) -> None:
        if not self._discovered:
            try:
                asyncio.run(self._discover_tools_async())
            except Exception as e:
                self.logger.warning(f"发现工具失败: {self._truncate(str(e), 1000)}")
                self._discovered = True

    def _match_tool(self, candidates: List[str]) -> Optional[str]:
        self._ensure_discovered()
        if not self._tool_names:
            for c in candidates:
                if isinstance(c, str) and c:
                    return c
            return None
        for c in candidates:
            if c in self._tool_names:
                return c
        return None

    def _filter_args(self, tool_name: Optional[str], args: Dict[str, Any]) -> Dict[str, Any]:
        if not tool_name:
            return {}
        schema = self._tool_schemas.get(tool_name) or {}
        props = schema.get("properties") or {}
        allowed = set(props.keys()) if isinstance(props, dict) else set()
        if not allowed:
            return {k: v for k, v in args.items() if v is not None}
        out: Dict[str, Any] = {}
        for k, v in args.items():
            if v is not None and k in allowed:
                out[k] = v
        return out

    def _resolve_query_tool(self) -> Optional[str]:
        t = self._match_tool([
            os.getenv("PROM_MCP_TOOL_QUERY") or "execute_query",
            "execute_query",
            "query",
        ])
        return t

    def _resolve_range_tool(self) -> Optional[str]:
        t = self._match_tool([
            os.getenv("PROM_MCP_TOOL_RANGE") or "execute_range_query",
            "execute_range_query",
            "range_query",
        ])
        return t

    def query_range(
        self,
        promql: str,
        start: Optional[str] = None,
        end: Optional[str] = None,
        step: Optional[str] = None,
    ) -> str:
        tool = self._resolve_range_tool()
        if not tool:
            return "当前 MCP 未提供范围查询工具"
        args = {"query": promql, "start": start, "end": end, "step": step}
        filtered = self._filter_args(tool, args)
        try:
            return self._call_tool(tool, filtered)
        except Exception as e:
            return f"调用 {tool} 失败: {str(e)}"

    def query_instant(self, promql: str, time: Optional[str] = None) -> str:
        tool = self._resolve_query_tool()
        if not tool:
            return "当前 MCP 未提供即时查询工具"
        args = {"query": promql, "time": time}
        filtered = self._filter_args(tool, args)
        try:
            return self._call_tool(tool, filtered)
        except Exception as e:
            return f"调用 {tool} 失败: {str(e)}"


    def list_targets(self) -> str:
        tool = self._match_tool([self.tool_targets, "prometheus_targets", "targets", "get_targets"])
        if not tool:
            return "当前 MCP 未提供抓取目标查询工具"
        try:
            return self._call_tool(tool, {})
        except Exception as e:
            return f"调用 {tool} 失败: {str(e)}"

    def list_metrics(self, limit: Optional[int] = None, offset: Optional[int] = None, filter_pattern: Optional[str] = None) -> str:
        tool = self._match_tool(["list_metrics"])
        if not tool:
            return "当前 MCP 未提供指标列表工具"
        args = {"limit": limit, "offset": offset, "filter_pattern": filter_pattern}
        filtered = self._filter_args(tool, args)
        try:
            return self._call_tool(tool, filtered)
        except Exception as e:
            return f"调用 {tool} 失败: {str(e)}"

    def _convert_prometheus_timestamps(self, obj: Any) -> Any:
        def fmt(ts: Any) -> Any:
            try:
                v = float(ts)
                if v > 0:
                    dt = datetime.fromtimestamp(v, tz=timezone(timedelta(hours=8)))
                    return dt.strftime("%Y-%m-%d %H:%M:%S")
                return ts
            except Exception:
                return ts
        if isinstance(obj, dict):
            out: Dict[str, Any] = {}
            for k, v in obj.items():
                if k == "value" and isinstance(v, list) and len(v) >= 1:
                    first = fmt(v[0])
                    v = [first] + v[1:]
                elif k == "values" and isinstance(v, list):
                    vv: List[Any] = []
                    for item in v:
                        if isinstance(item, list) and len(item) >= 1:
                            first = fmt(item[0])
                            item = [first] + item[1:]
                        vv.append(item)
                    v = vv
                else:
                    v = self._convert_prometheus_timestamps(v)
                out[k] = v
            return out
        if isinstance(obj, list):
            return [self._convert_prometheus_timestamps(x) for x in obj]
        return obj


    def metadata(self, metric: Optional[str] = None) -> str:
        tool = self._match_tool([self.tool_metadata, "prometheus_metadata", "metadata", "get_metric_metadata"])
        if not tool:
            return "当前 MCP 未提供元数据查询工具"
        args = {"metric": metric}
        filtered = self._filter_args(tool, args)
        try:
            return self._call_tool(tool, filtered)
        except Exception as e:
            return f"调用 {tool} 失败: {str(e)}"

    def health_check(self) -> str:
        tool = self._match_tool(["health_check"])
        if not tool:
            return "当前 MCP 未提供健康检查工具"
        try:
            return self._call_tool(tool, {})
        except Exception as e:
            return f"调用 {tool} 失败: {str(e)}"
