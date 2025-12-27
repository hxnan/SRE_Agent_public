import os
import json
import asyncio
from typing import Any, Dict, List, Optional, Tuple
import logging
from contextlib import AsyncExitStack

from mcp import ClientSession
from mcp.client.sse import sse_client
try:
    from mcp.client.http import http_client
except Exception:
    http_client = None


class DeepWikiMCPClient:
    def __init__(self,
                 mode: Optional[str] = None,
                 base_url: Optional[str] = None,
                 repo_url: Optional[str] = None,
                 timeout: float = 30.0):
        self.mode = (mode or os.getenv("DEEPWIKI_MCP_MODE") or "sse").lower()
        self.base_url = base_url or os.getenv("DEEPWIKI_MCP_URL") or "https://mcp.deepwiki.com/mcp"
        self.sse_url = (os.getenv("DEEPWIKI_MCP_SSE_URL") or "https://mcp.deepwiki.com/sse").rstrip("/")
        self.repo_url = repo_url or os.getenv("DEEPWIKI_REPO_URL") or None
        self.timeout = timeout
        level = os.getenv("DEEPWIKI_MCP_LOG_LEVEL", "INFO").upper()
        verbose = os.getenv("DEEPWIKI_MCP_LOG_VERBOSE", "false").lower()
        self.logger = logging.getLogger("deepwiki_mcp")
        if not self.logger.handlers:
            handler = logging.StreamHandler()
            formatter = logging.Formatter("%(asctime)s %(levelname)s %(message)s")
            handler.setFormatter(formatter)
            self.logger.addHandler(handler)
        self.logger.setLevel(getattr(logging, level, logging.INFO))
        self.verbose = verbose in ("1", "true", "yes", "on")
        headers_env = os.getenv("DEEPWIKI_MCP_HEADERS")
        try:
            self.headers: Dict[str, str] = json.loads(headers_env) if headers_env else {}
        except Exception:
            self.headers = {}

        self._exit_stack: Optional[AsyncExitStack] = None
        self._session: Optional[ClientSession] = None

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

    def _resolve_repo(self, repo_url: Optional[str]) -> str:
        r = repo_url or self.repo_url
        if not r:
            raise ValueError("DEEPWIKI_REPO_URL 未配置且未提供 repo_url")
        return r

    async def _ensure_session(self) -> ClientSession:
        raise RuntimeError("_ensure_session is not used in per-call mode")

    async def _call_tool_async(self, name: str, arguments: Dict[str, Any]) -> str:
        stack = AsyncExitStack()
        await stack.__aenter__()
        if self.mode == "sse" or (self.mode == "http" and http_client is None):
            url = self.sse_url
            self.logger.info(f"MCP SSE connect url={url} repo={arguments.get('repo_url')}")
            read, write = await stack.enter_async_context(sse_client(url, headers=self.headers))
        else:
            url = self.base_url.rstrip("/")
            self.logger.info(f"MCP HTTP connect url={url} repo={arguments.get('repo_url')}")
            read, write = await stack.enter_async_context(http_client(url, headers=self.headers))
        session = await stack.enter_async_context(ClientSession(read, write))
        await session.initialize()
        args = dict(arguments)
        if "repo_url" in args and "repoName" not in args:
            args["repoName"] = args["repo_url"]
        self.logger.info(f"MCP call tool name={name} mode={self.mode} repo={arguments.get('repo_url')}")
        if self.verbose:
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
        if self.verbose:
            self.logger.info(f"MCP call normalized body={normalized}")
        else:
            self.logger.info(f"MCP call normalized len={len(normalized)} preview={self._truncate(normalized)}")
        return normalized

    def _call_tool(self, name: str, arguments: Dict[str, Any]) -> str:
        try:
            return asyncio.run(self._call_tool_async(name, arguments))
        except Exception as e:
            if self.verbose:
                self.logger.warning(f"MCP call tool error name={name} error={str(e)}")
            else:
                self.logger.warning(f"MCP call tool error name={name} preview={self._truncate(str(e))}")
            raise

    def close(self) -> None:
        try:
            if self._exit_stack is not None:
                pass
        except Exception:
            pass

    async def aclose(self) -> None:
        try:
            if self._exit_stack is not None:
                await self._exit_stack.aclose()
            self._exit_stack = None
            self._session = None
        except Exception:
            pass

    def close_sync(self) -> None:
        try:
            pass
        except Exception:
            pass

    def read_wiki_structure(self, repo_url: Optional[str] = None) -> str:
        r = self._resolve_repo(repo_url)
        try:
            return self._call_tool("read_wiki_structure", {"repo_url": r})
        except Exception as e:
            return f"调用 read_wiki_structure 失败: {str(e)}"

    def read_wiki_contents(self, repo_url: Optional[str] = None, paths: Optional[List[str]] = None) -> str:
        r = self._resolve_repo(repo_url)
        try:
            return self._call_tool("read_wiki_contents", {"repo_url": r, "paths": paths or []})
        except Exception as e:
            return f"调用 read_wiki_contents 失败: {str(e)}"

    def ask_question(self, question: str, repo_url: Optional[str] = None) -> str:
        r = self._resolve_repo(repo_url)
        try:
            return self._call_tool("ask_question", {"repo_url": r, "question": question})
        except Exception as e:
            return f"调用 ask_question 失败: {str(e)}"
