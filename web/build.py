# -*- coding: utf-8 -*-
"""
构建 H5 单文件版 index.html
用法: python build.py
- 模板: template.html (含 __ICON180__ / __MANIFEST__ / __JSQR__ 占位符)
- jsqr.js 内联为 <script>__JSQR__</script>
- 图标和 PWA manifest 的 data URI 从上一版 index.html 中提取复用
"""
import re
import base64
from pathlib import Path

WEB = Path(__file__).parent
TEMPLATE = WEB / "template.html"
JSQR = WEB / "jsqr.js"
OUT = WEB / "index.html"

def extract_prev_uri(pattern: str) -> str:
    """从上一版 index.html 提取 data URI（图标 / manifest）"""
    if not OUT.exists():
        raise SystemExit(f"缺少上一版 {OUT.name}，无法提取 {pattern}")
    html = OUT.read_text(encoding="utf-8")
    m = re.search(pattern, html)
    if not m:
        raise SystemExit(f"上一版 {OUT.name} 中未找到 {pattern}")
    return m.group(1)

def main():
    tpl = TEMPLATE.read_text(encoding="utf-8")
    jsqr = JSQR.read_text(encoding="utf-8")

    icon_uri = extract_prev_uri(r'<link rel="apple-touch-icon" href="([^"]+)"')
    manifest_uri = extract_prev_uri(r'<link rel="manifest" href="([^"]+)"')

    # 校验 manifest 可解码（防止误提取到半个属性）
    payload = manifest_uri.split("base64,", 1)[1]
    base64.b64decode(payload)

    html = tpl.replace("__ICON180__", icon_uri)
    html = html.replace("__MANIFEST__", manifest_uri)
    html = html.replace("__JSQR__", jsqr)

    OUT.write_text(html, encoding="utf-8")
    print(f"OK -> {OUT}  ({OUT.stat().st_size/1024:.0f} KB)")

if __name__ == "__main__":
    main()
