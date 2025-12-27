import os
import json
import asyncio
from typing import Any, Dict, List, Optional
import logging
from contextlib import AsyncExitStack

from mcp import ClientSession
from mcp.client.sse import sse_client
try:
    from mcp.client.http import http_client
except Exception:
    http_client = None


class LokiMCPClient:
    def __init__(self,
                 mode: Optional[str] = None,
                 base_url: Optional[str] = None,
                 sse_url: Optional[str] = None,
                 timeout: float = 30.0):
        self.mode = (mode or os.getenv("LOKI_MCP_MODE") or "sse").lower()
        self.base_url = base_url or os.getenv("LOKI_MCP_URL") or None
        self.sse_url = (sse_url or os.getenv("LOKI_MCP_SSE_URL") or "http://localhost:7080/sse").rstrip("/")
        self.timeout = timeout
        level = os.getenv("LOKI_MCP_LOG_LEVEL", "INFO").upper()
        verbose = os.getenv("LOKI_MCP_LOG_VERBOSE", "false").lower()
        self.logger = logging.getLogger("loki_mcp")
        if not self.logger.handlers:
            handler = logging.StreamHandler()
            formatter = logging.Formatter("%(asctime)s %(levelname)s %(message)s")
            handler.setFormatter(formatter)
            self.logger.addHandler(handler)
        self.logger.setLevel(getattr(logging, level, logging.INFO))
        self.verbose = verbose in ("1", "true", "yes", "on")
        headers_env = os.getenv("LOKI_MCP_HEADERS")
        try:
            self.headers: Dict[str, str] = json.loads(headers_env) if headers_env else {}
        except Exception:
            self.headers = {}
            logging.getLogger("loki_mcp").warning("LOKI_MCP_HEADERS 解析失败，已忽略该配置")

        self.tool_query = os.getenv("LOKI_MCP_TOOL_QUERY", "loki_query")

        if self.mode == "sse" and not self.sse_url and self.base_url and http_client is not None:
            self.logger.info("未配置 LOKI_MCP_SSE_URL，自动切换到 HTTP 模式")
            self.mode = "http"

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
                        parts.append(json.dumps(item, ensure_ascii=False))
                elif hasattr(item, "text") and isinstance(getattr(item, "text"), str):
                    parts.append(getattr(item, "text"))
                else:
                    parts.append(str(item))
            return "\n".join([p for p in parts if p])
        if isinstance(content, dict):
            t = content.get("text") or content.get("content")
            if isinstance(t, str):
                return t
            return json.dumps(content, ensure_ascii=False)
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
        if self.mode == "sse" or (self.mode == "http" and http_client is None):
            if not self.sse_url:
                raise RuntimeError("LOKI_MCP_SSE_URL 未配置")
            url = self.sse_url
            self.logger.info(f"MCP SSE connect url={url}")
            try:
                read, write = await stack.enter_async_context(sse_client(url, headers=self.headers))
            except Exception as e:
                self._log_error(name, e, arguments)
                if http_client is not None and self.base_url:
                    fb = self.base_url.rstrip("/")
                    self.logger.warning(f"SSE 连接失败，回退到 HTTP url={fb}")
                    try:
                        read, write = await stack.enter_async_context(http_client(fb, headers=self.headers))
                        self.mode = "http"
                        url = fb
                    except Exception as he:
                        self._log_error(name, he, arguments)
                        await stack.aclose()
                        raise
                else:
                    await stack.aclose()
                    if http_client is None:
                        raise RuntimeError("SSE 失败且 HTTP 传输不可用：请安装 mcp 的 HTTP 客户端或设置 LOKI_MCP_MODE=http")
                    raise
        else:
            if not self.base_url:
                raise RuntimeError("LOKI_MCP_URL 未配置")
            url = self.base_url.rstrip("/")
            self.logger.info(f"MCP HTTP connect url={url}")
            read, write = await stack.enter_async_context(http_client(url, headers=self.headers))
        session = await stack.enter_async_context(ClientSession(read, write))
        await session.initialize()
        args = dict(arguments)
        self.logger.info(f"MCP call url={url} name={name} mode={self.mode}")
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

    def query_logs(self,
                   query: str,
                   start: Optional[str] = None,
                   end: Optional[str] = None,
                   limit: Optional[int] = None,
                   labels: Optional[Dict[str, str]] = None) -> str:
        try:
            return self._call_tool(self.tool_query, {
                "query": query,
                "start": start,
                "end": end,
                "limit": limit,
                "labels": labels or {}
            })
        except Exception as e:
            return f"调用 {self.tool_query} 失败: {str(e)}"
