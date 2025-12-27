from langchain_core.tools import tool

@tool(parse_docstring=True)
def sre_think_tool(diagnosis_log: str) -> str:
    """用于故障排查和根因定位期间进行结构化分析的思考工具。

    在调试循环中使用此工具可以强制进行“深思熟虑的暂停”，以便综合可观测性数据、验证假设并规划安全的修复步骤。

    使用时机 (When to use):
    - 查询监控工具（Prometheus, ELK, Jaeger 等）之后：区分哪些是关键信号，哪些是噪音？
    - 验证假设之后：数据是否支持我的理论（例如：“真的是数据库锁表了吗”）？
    - 执行具体检查之前：我为什么要查这个指标？我预期看到什么结果？
    - 采取行动之前：此操作是安全的吗？是否会引发生产环境的二次故障？

    思考记录 (diagnosis_log) 应包含以下要素：
    1. 观测综合 (Observation Synthesis) - 已确认了哪些基于事实的异常（具体的 Metrics/Logs/Traces）？
    2. 假设状态 (Hypothesis Status) - 哪些可能性已被排除？哪些目前是高概率嫌疑？
    3. 缺口分析 (Gap Analysis) - 还需要哪些遥测数据才能完成故障定界（Isolation）？
    4. 安全与影响 (Safety & Impact) - 服务降级是否在恶化？下一步操作（如重启、回滚）是否可逆？
    5. 下一步诊断 (Next Diagnostic Step) - 具体要运行的查询语句或命令（专注于缩小故障范围）。

    Args:
        diagnosis_log: 你的详细技术推理日志，需包含假设的变更、证据的评估以及风险评估。

    Returns:
        确认诊断思考步骤已被记录。
    """
    return f"诊断日志已记录: {diagnosis_log}"