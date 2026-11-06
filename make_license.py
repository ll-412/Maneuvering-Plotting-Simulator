# -*- coding: utf-8 -*-
"""
舰操绘算 · 授权码生成器

用法一（双击/直接运行）：
    python make_license.py
    然后把同学发来的机器码粘进去，回车，出授权码。

用法二（命令行）：
    python make_license.py K7X2-9M4P

算法：HmacSHA256(机器码, KEY) → 取前 8 位十六进制 → XXXX-XXXX
和 App 里的 Java 实现完全一致（已用同一组输入验证过）。
"""
import hashlib
import hmac
import os
import sys
import time

KEY = b"JC-Plot-2026-1155-Lic"
LOG = os.path.join(os.path.dirname(os.path.abspath(__file__)), "licenses.txt")


def license_for(device):
    """机器码 → 授权码"""
    msg = "".join(c for c in (device or "").upper() if c.isalnum())
    if len(msg) < 6:
        return None
    digest = hmac.new(KEY, msg.encode("utf-8"), hashlib.sha256).hexdigest()[:8].upper()
    return digest[:4] + "-" + digest[4:]


def clean(device):
    """统一成 XXXX-XXXX 的样子（方便对照）"""
    msg = "".join(c for c in (device or "").upper() if c.isalnum())
    if len(msg) == 8:
        return msg[:4] + "-" + msg[4:]
    return device.strip().upper()


def record(device, lic):
    try:
        with open(LOG, "a", encoding="utf-8") as f:
            f.write("%s\t%s\t%s\n" % (time.strftime("%Y-%m-%d %H:%M"), clean(device), lic))
    except Exception:
        pass


def show(device):
    lic = license_for(device)
    if not lic:
        print("  [X] 机器码太短，应该是 XXXX-XXXX 这样的 8 位")
        return None
    print()
    print("  机器码：%s" % clean(device))
    print("  授权码：%s" % lic)
    print()
    record(device, lic)
    return lic


def main():
    args = [a for a in sys.argv[1:] if a.strip()]
    if args:
        for a in args:
            show(a)
        return

    print("=" * 46)
    print("  舰操绘算 · 授权码生成器")
    print("=" * 46)
    print("  把同学发来的机器码粘进来（连回车），空行退出")
    print()

    while True:
        try:
            raw = input("  机器码 > ").strip()
        except (EOFError, KeyboardInterrupt):
            break
        if not raw:
            break
        show(raw)

    print()
    print("  记录已追加到 licenses.txt")
    if os.name == "nt":
        try:
            input("  按回车关闭…")
        except EOFError:
            pass


if __name__ == "__main__":
    main()
