# -*- coding: utf-8 -*-
# ============================================================================
# 【功能】AI 批改推理服务（阶段7）—— 本地多模态大模型 HTTP 服务
# ----------------------------------------------------------------------------
# 【说明】本脚本是"用代码代替第三方常驻软件"的轻量方案：
#         直接通过 transformers 库加载 Qwen2.5-VL 多模态模型（能看图、能读文），
#         扮演指导教师角色批改学生的课程设计模块内容。
#
# 【硬件自适应降级链】（自动探测，无需手工配置）
#         第1级：NVIDIA GPU 且空闲显存 ≥ 8GB → bfloat16 全精度推理（效果最佳）
#         第2级：GPU 显存不足 8GB          → 4bit 量化推理（需 bitsandbytes）
#         第3级：无 GPU / 量化库缺失        → CPU 推理（较慢，约10~30秒/模块）
#         第4级：依赖缺失或模型加载失败     → engine=none
#                （Java 后端检测到后会自动切换"本地规则模拟批改"兜底，
#                  保证任何机器上系统都完整可用）
#
# 【接口约定】
#         GET  /health → {"ok":true,"engine":"gpu|gpu-4bit|cpu|none","model":"..."}
#         POST /grade  → 入参 {"items":[{moduleCode,moduleName,moduleMax,
#                               contentText,images:[base64str...]}]}
#                        出参 {"results":[{moduleCode,score,comment}],model,engine}
#         comment 为 JSON 字符串：{"strengths":"优点","problems":"问题","advice":"建议"}
#
# 【启动方式】pip install -r requirements.txt 后执行：python ai_server.py
#         首次启动会自动从 HF 镜像下载模型权重（约 7GB，仅第一次）。
# ============================================================================
import os

# 国内网络环境优先使用 HF 镜像站加速模型下载（必须在 import transformers 之前设置）
os.environ.setdefault("HF_ENDPOINT", "https://hf-mirror.com")
# 禁用 Xet 下载协议：部分新仓库的文件会被重定向到境外 xethub.hf.co 导致国内直连超时，
# 关闭后强制走镜像站的普通 HTTP 下载（必须在 import huggingface_hub 之前设置）
os.environ.setdefault("HF_HUB_ENABLE_XET", "0")

import json
import re
import io
import base64
import threading
import traceback
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

# ----------------------------------------------------------------------------
# 全局配置
# ----------------------------------------------------------------------------
HOST = "127.0.0.1"      # 监听地址（仅本机访问，Java 后端与 Python 同机部署）
PORT = 8300             # 监听端口（Java 侧 application.yml 的 ai.engine.base-url 需一致）
MODEL_ID = "Qwen/Qwen2.5-VL-3B-Instruct"   # 多模态模型（3B：12GB 显存轻松承载，CPU 也可跑）
GPU_VRAM_MIN_GB = 8     # 第1级全精度推理要求的空闲显存下限
MAX_IMAGE_PIXELS = 1024 # 送入模型的图片最长边（防止图片占满上下文）

# 教师人设系统提示词：贴近真人指导教师的批改风格（亲切、鼓励为主、指出问题具体）
SYSTEM_PROMPT = (
    "你是高校课程设计课程的指导教师（智能助教），正在批改学生小组的作业模块。"
    "你的批改风格：严谨但亲切，以鼓励为主，问题指认具体、建议可执行，像真实老师写的评语。"
    "请阅读学生提交的模块内容（可能含图片），从内容完整性、技术正确性、逻辑条理、"
    "工作量与规范度五个维度综合评分。"
    "必须严格只输出一个 JSON 对象，不要输出任何其它文字，格式："
    '{"score": <0-100整数>, "strengths": "<优点，中文>", '
    '"problems": "<存在的问题，中文>", "advice": "<修改建议，中文>"}'
)

# ----------------------------------------------------------------------------
# 全局状态：模型引擎（懒加载 + 全局锁串行推理，防止并发打爆显存）
# ----------------------------------------------------------------------------
ENGINE_LOCK = threading.Lock()   # 推理串行锁
MODEL = None                     # 加载后的模型对象
PROCESSOR = None                 # 加载后的处理器（tokenizer+图像预处理）
ENGINE_NAME = "loading"          # 当前引擎级别：loading / gpu / gpu-4bit / cpu / none
MODEL_LABEL = "-"                # 实际使用的模型标识（写入批改记录供教师核查）


def _detect_free_vram_gb():
    """探测 GPU 空闲显存（GB）；无 CUDA 环境返回 0"""
    try:
        import torch
        if not torch.cuda.is_available():
            return 0.0
        free, _total = torch.cuda.mem_get_info(0)
        return free / (1024 ** 3)
    except Exception:
        return 0.0


def load_engine():
    """
    【功能】按硬件降级链加载模型（后台调用一次，全局只加载一份）
    【模型来源优先级】
      1. 本地目录 models/qwen2.5-vl-3b（用魔搭 ModelScope 预先下载，国内直连免代理）
      2. HF 线上仓库（HF_ENDPOINT 已指向 hf-mirror，网络通畅时自动下载缓存）
    """
    global MODEL, PROCESSOR, ENGINE_NAME, MODEL_LABEL
    try:
        import torch
        from transformers import AutoProcessor
        # Qwen2.5-VL 的专用模型类（旧版 transformers 无此类时捕获后降级）
        from transformers import Qwen2_5_VLForConditionalGeneration

        # ---------- 模型来源探测：本地目录完整（关键文件齐全）则离线加载 ----------
        local_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                 "models", "qwen2.5-vl-3b")
        key_files = ["config.json", "model.safetensors.index.json",
                     "model-00001-of-00002.safetensors", "model-00002-of-00002.safetensors"]
        if all(os.path.isfile(os.path.join(local_dir, f)) for f in key_files):
            model_source = local_dir
            print(f"[AI] 检测到本地模型目录，离线加载：{local_dir}")
        else:
            model_source = MODEL_ID
            print(f"[AI] 本地模型目录不完整，改走 HF 线上加载：{MODEL_ID}")

        has_cuda = torch.cuda.is_available()
        free_gb = _detect_free_vram_gb()
        print(f"[AI] 硬件探测：CUDA={has_cuda}，GPU空闲显存={free_gb:.1f}GB")

        # ---------- 第1级：GPU 全精度 bfloat16 ----------
        if has_cuda and free_gb >= GPU_VRAM_MIN_GB:
            print("[AI] 降级链第1级：GPU bfloat16 全精度推理")
            MODEL = Qwen2_5_VLForConditionalGeneration.from_pretrained(
                model_source, torch_dtype=torch.bfloat16, device_map={"": 0})
            ENGINE_NAME, MODEL_LABEL = "gpu", "Qwen2.5-VL-3B-Instruct(bf16)"

        # ---------- 第2级：GPU 4bit 量化 ----------
        elif has_cuda:
            print("[AI] 降级链第2级：GPU 4bit 量化推理（bitsandbytes）")
            from transformers import BitsAndBytesConfig
            bnb = BitsAndBytesConfig(load_in_4bit=True, bnb_4bit_quant_type="nf4",
                                     bnb_4bit_compute_dtype=torch.bfloat16)
            MODEL = Qwen2_5_VLForConditionalGeneration.from_pretrained(
                model_source, quantization_config=bnb, device_map={"": 0})
            ENGINE_NAME, MODEL_LABEL = "gpu-4bit", "Qwen2.5-VL-3B-Instruct(4bit)"

        # ---------- 第3级：CPU 推理 ----------
        else:
            print("[AI] 降级链第3级：CPU 推理（速度较慢，属正常现象）")
            dtype = torch.bfloat16 if torch.cuda.is_bf16_supported() else torch.float32
            MODEL = Qwen2_5_VLForConditionalGeneration.from_pretrained(
                model_source, torch_dtype=dtype, device_map="cpu")
            ENGINE_NAME, MODEL_LABEL = "cpu", "Qwen2.5-VL-3B-Instruct(CPU)"

        PROCESSOR = AutoProcessor.from_pretrained(model_source)
        MODEL.eval()
        print(f"[AI] 模型加载完成：engine={ENGINE_NAME}, model={MODEL_LABEL}")
    except Exception as e:
        # ---------- 第4级：加载失败 → 交由 Java 侧规则模拟兜底 ----------
        MODEL, PROCESSOR = None, None
        ENGINE_NAME = "none"
        print(f"[AI] 模型加载失败（Java后端将走规则模拟兜底）：{e}")
        traceback.print_exc()


def _decode_image(b64str):
    """
    【功能】base64 图片字符串 → PIL 图片对象（超长边自动等比缩放）
    """
    from PIL import Image
    # 兼容 dataURL 前缀（Quill 富文本内嵌图片为 data:image/...;base64,xxxx）
    if "," in b64str and b64str.strip().startswith("data:"):
        b64str = b64str.split(",", 1)[1]
    img = Image.open(io.BytesIO(base64.b64decode(b64str))).convert("RGB")
    w, h = img.size
    if max(w, h) > MAX_IMAGE_PIXELS:
        scale = MAX_IMAGE_PIXELS / float(max(w, h))
        img = img.resize((int(w * scale), int(h * scale)))
    return img


def _extract_json(text):
    """
    【功能】从模型回复中提取 JSON 对象（容错：模型偶尔会在 JSON 前后加说明文字）
    """
    m = re.search(r"\{.*\}", text, re.S)
    if not m:
        return None
    try:
        return json.loads(m.group(0))
    except Exception:
        return None


def grade_one(item):
    """
    【功能】批改单个模块：组装多模态消息 → 模型推理 → 解析评分与评语
    """
    import torch
    # 1. 组装用户消息：模块背景信息 + 内容文本 + 图片（多模态输入）
    user_text = (
        f"请批改课程设计的「{item['moduleName']}」模块（该模块满分 {item['moduleMax']} 分，"
        f"占总评权重按满分折算）。评分要求：给出的 score 为百分制（0-100）。"
        f"\n\n===== 学生提交内容开始 =====\n{item['contentText']}\n===== 学生提交内容结束 ====="
    )
    content = [{"type": "text", "text": user_text}]
    images = []
    for b64 in (item.get("images") or [])[:4]:   # 最多携带4张图片，防止上下文溢出
        try:
            images.append(_decode_image(b64))
            content.append({"type": "image", "image": images[-1]})
        except Exception:
            pass  # 单张图片解码失败直接跳过，不影响文本批改

    messages = [{"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": content}]
    text = PROCESSOR.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
    inputs = PROCESSOR(text=[text], images=images if images else None,
                       padding=True, return_tensors="pt").to(MODEL.device)

    # 2. 生成（贪心解码保证评分稳定可复现）
    with torch.no_grad():
        out = MODEL.generate(**inputs, max_new_tokens=600, do_sample=False)
    reply = PROCESSOR.batch_decode(out[:, inputs["input_ids"].shape[1]:],
                                   skip_special_tokens=True)[0].strip()

    # 3. 解析模型输出
    data = _extract_json(reply)
    if data is None:
        # 兜底：模型未按格式输出时，把原文作为评语、不给分（由 Java 侧再兜底给分）
        return {"moduleCode": item["moduleCode"], "score": None, "comment": reply[:800]}
    score = data.get("score")
    try:
        score = max(0, min(100, int(round(float(score)))))
    except Exception:
        score = None
    comment = json.dumps({
        "strengths": str(data.get("strengths", "")),
        "problems": str(data.get("problems", "")),
        "advice": str(data.get("advice", "")),
    }, ensure_ascii=False)
    return {"moduleCode": item["moduleCode"], "score": score, "comment": comment}


class Handler(BaseHTTPRequestHandler):
    """HTTP 请求处理器：/health 健康检查 + /grade 批改"""

    def _send(self, code, obj):
        """统一 JSON 响应输出"""
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        """健康检查：Java 每次触发批改前先探测本服务是否可用"""
        if self.path == "/health":
            self._send(200, {"ok": ENGINE_NAME != "none" and ENGINE_NAME != "loading",
                             "engine": ENGINE_NAME, "model": MODEL_LABEL,
                             "detail": "模型加载中" if ENGINE_NAME == "loading" else "ok"})
        else:
            self._send(404, {"ok": False})

    def do_POST(self):
        """批改接口：逐模块推理（全局锁串行），单模块失败不阻塞其余模块"""
        if self.path != "/grade":
            return self._send(404, {"ok": False})
        try:
            length = int(self.headers.get("Content-Length", 0))
            payload = json.loads(self.rfile.read(length).decode("utf-8"))
            items = payload.get("items") or []
            if MODEL is None or PROCESSOR is None:
                # 模型不可用：返回 503，Java 收到后自动切换规则模拟兜底
                return self._send(503, {"ok": False, "engine": ENGINE_NAME,
                                        "error": "模型未加载，请使用规则模拟兜底"})
            results = []
            with ENGINE_LOCK:                     # 串行推理，防显存溢出
                for item in items:
                    try:
                        results.append(grade_one(item))
                    except Exception as e:
                        print(f"[AI] 模块{item.get('moduleCode')}批改异常：{e}")
                        traceback.print_exc()
                        results.append({"moduleCode": item.get("moduleCode"),
                                        "score": None, "comment": f"批改异常：{e}"})
            self._send(200, {"ok": True, "results": results,
                             "model": MODEL_LABEL, "engine": ENGINE_NAME})
        except Exception as e:
            traceback.print_exc()
            self._send(500, {"ok": False, "error": str(e), "engine": ENGINE_NAME})

    def log_message(self, fmt, *args):
        """精简访问日志"""
        print("[HTTP]", fmt % args)


if __name__ == "__main__":
    # 后台线程先加载模型（首次需下载权重，控制台会显示进度）
    threading.Thread(target=load_engine, daemon=True).start()
    print(f"[AI] 推理服务启动：http://{HOST}:{PORT} （GET /health 探活）")
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    server.serve_forever()
