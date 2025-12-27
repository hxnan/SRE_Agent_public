import os
import json
import random
import unittest
import logging
from datetime import datetime, timedelta, timezone
import sys
import importlib.util
import os as _os


class TestPrometheusMCPEndToEnd(unittest.TestCase):
    def test_end_to_end_prometheus_mcp(self):
        logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
        url = os.getenv("PROM_MCP_STREAMABLE_URL") or os.getenv("PROM_MCP_URL") or "http://localhost:18090/mcp"
        headers_env = os.getenv("PROM_MCP_HEADERS")
        headers = {}
        if headers_env:
            try:
                headers = json.loads(headers_env)
            except Exception:
                headers = {}
        logging.info(f"PROM_MCP_STREAMABLE_URL={url}")
        logging.info(f"PROM_MCP_HEADERS={json.dumps(headers, ensure_ascii=False)}")
        spec = importlib.util.spec_from_file_location(
            "prom_mcp_client_mod",
            _os.path.join(_os.path.dirname(__file__), "..", "mcp_client.py"),
        )
        mod = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = mod
        spec.loader.exec_module(mod)
        client = mod.PrometheusMCPClient(base_url=url)
        health = client.health_check()
        logging.info(f"health_check={health}")
        targets = client.list_targets()
        logging.info(f"targets={targets[:500]}")
        self.assertTrue(isinstance(targets, str) and len(targets) > 0)
        metrics_raw = client.list_metrics(limit=1000, offset=0, filter_pattern=None)
        logging.info(f"metrics_raw_len={len(metrics_raw)}")
        metrics = []
        try:
            parsed = json.loads(metrics_raw)
            if isinstance(parsed, list):
                for item in parsed:
                    if isinstance(item, str):
                        metrics.append(item)
                    elif isinstance(item, dict):
                        name = item.get("name") or item.get("metric") or item.get("__name__")
                        if isinstance(name, str):
                            metrics.append(name)
            elif isinstance(parsed, dict):
                for key in ("metrics", "items", "data", "result"):
                    arr = parsed.get(key)
                    if isinstance(arr, list):
                        for item in arr:
                            if isinstance(item, str):
                                metrics.append(item)
                            elif isinstance(item, dict):
                                name = item.get("name") or item.get("metric") or item.get("__name__")
                                if isinstance(name, str):
                                    metrics.append(name)
        except Exception:
            lines = [s.strip() for s in metrics_raw.splitlines() if s.strip()]
            for s in lines:
                if s and s.replace("_", "").replace(":", "").isalnum():
                    metrics.append(s)
        metrics = list(dict.fromkeys([m for m in metrics if isinstance(m, str) and m]))
        logging.info(f"metrics_count={len(metrics)}")
        self.assertTrue(len(metrics) > 0)
        end = datetime.now(timezone.utc)
        start = end - timedelta(hours=1)
        fmt = "%Y-%m-%dT%H:%M:%SZ"
        start_s = start.strftime(fmt)
        end_s = end.strftime(fmt)
        step = "60s"
        sample = metrics if len(metrics) <= 20 else random.sample(metrics, 20)
        success = 0
        for metric in sample:
            out = client.query_range(metric, start=start_s, end=end_s, step=step)
            logging.info(f"query_range metric={metric} out_len={len(out)}")
            if isinstance(out, str) and len(out) > 0:
                success += 1
        logging.info(f"query_success={success}/{len(sample)}")
        self.assertTrue(success >= 1)
