# 生成舰操绘算的 APK 图标（纯 Python，不依赖第三方库）
# 深蓝底 + 绿色同心方位圈 + 红色相对运动线 + 白色本船点
import struct, zlib, math


def png(w, h, pixels):
    def chunk(typ, data):
        c = struct.pack(">I", len(data)) + typ + data
        return c + struct.pack(">I", zlib.crc32(typ + data) & 0xffffffff)
    raw = b"".join(b"\x00" + bytes(v for p in row for v in p) for row in pixels)
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\x0a" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def icon(path):
    w = h = 192
    cx = cy = 96
    bg   = (15, 23, 42)
    ring = (52, 211, 153)
    red  = (239, 68, 68)

    px = [[list(bg) for _ in range(w)] for _ in range(h)]

    # 同心方位圈
    for r in (78, 62, 47, 33):
        rr = r * r
        for y in range(h):
            for x in range(w):
                d2 = (x - cx) ** 2 + (y - cy) ** 2
                if abs(d2 - rr) < r * 2.6:
                    px[y][x] = list(ring)

    # 外圈 15° 刻度
    for k in range(24):
        a = k * math.pi / 12.0
        for t in range(0, 9):
            rr = 78 - t
            x = int(cx + rr * math.sin(a))
            y = int(cy - rr * math.cos(a))
            if 0 <= x < w and 0 <= y < h:
                px[y][x] = list(ring)

    # 红色相对运动线（左下 → 右上）
    x0, y0, x1, y1 = 42, 148, 146, 52
    n = 220
    for i in range(n + 1):
        t = i / n
        x = x0 + (x1 - x0) * t
        y = y0 + (y1 - y0) * t
        for dy in (-3, -2, -1, 0, 1, 2, 3):
            for dx in (-2, -1, 0, 1, 2):
                xx = int(x + dx); yy = int(y + dy)
                if 0 <= xx < w and 0 <= yy < h:
                    px[yy][xx] = list(red)

    # 箭头
    ax, ay, ux, uy = x1, y1, 0.707, -0.707
    for i in range(0, 17):
        for j in range(0, i + 1):
            x = int(ax - ux * i * 0.9 + uy * j * 0.9)
            y = int(ay - uy * i * 0.9 - ux * j * 0.9)
            if 0 <= x < w and 0 <= y < h:
                px[y][x] = list(red)

    # 本船白点
    for y in range(h):
        for x in range(w):
            if (x - cx) ** 2 + (y - cy) ** 2 <= 49:
                px[y][x] = [255, 255, 255]

    open(path, "wb").write(png(w, h, px))
    print("OK", path)


icon("app/res/drawable/icon.png")
