# 舰操绘算 · Maneuvering Plotting Simulator

把纸上的舰操绘算图搬到手机上：画相对运动线、作速度三角形、算 DCPA / TCPA，再配一套习题和知识点。

> 单人自用 + 小范围分享。

---

## 现在有什么

| 模块 | 状态 | 说明 |
|---|:--:|---|
| 🧭 舰操模拟 | ✅ | 相对运动标绘、速度三角形、DCPA / TCPA 计算 |
| 📝 习题模拟 | 🚧 | 题库框架 + 4 道示例题，题库持续补充 |
| 📖 说明书 · 知识点 | ✅ | 7 章用法 + 7 章理论知识 |
| 🤖 AI 助教 | ⏸ | 接口占位，等定模型 |

## 这一版新加的东西

- **线段数据面板**：画完线自动跳到「辅助」页，角度 / 长度 / 起点 / 终点 全部可以直接打数字改。精度不靠放大，靠数值。
- **高倍率抑制噪点**：放大超过 5 倍时底图自动提对比度，把密集的密位点阵压下去，让线更突出。
- **明暗主题**：首页右上角 🌙 / ☀️ 切换，选完记住。

---

## 技术栈

| 层 | 用什么 |
|---|---|
| 界面 | 单文件 HTML + Canvas 2D，无框架、无 CDN |
| 安卓壳 | 原生 WebView（无 Gradle，手工 aapt2 + d8 + apksigner） |
| 存储 | `localStorage` 自动存画布 + 主题 |
| 导出 | Canvas → JPEG → `@JavascriptInterface` → `MediaStore` 存相册 |

**为什么是单文件 HTML**：离线可用、一个文件就是全部、打包进 APK 只要 `assets/index.html`。代价是底图用 Base64 内嵌，HTML 有 200 多 KB 是这张图。

---

## 目录结构

```
.
├── VERSION                        ← 改这里就升版
├── build.sh                       ← 一键构建
├── icon_gen.py                    ← 纯 Python 生成图标
├── debug.keystore                 ← 签名章（不进库！）
├── web/
│   ├── 舰操绘算手机版.html         ← 主程序
│   └── 舰操绘算课堂演示工具.html    ← 电脑版（保留不改）
├── app/
│   ├── AndroidManifest.xml
│   ├── res/                       ← 图标、应用名
│   └── src/com/example/jiancao/
│       ├── MainActivity.java      ← WebView 壳、沉浸全屏、手势排除
│       └── Bridge.java            ← JS 桥：存相册、切横竖屏、当前屏
└── out/
    ├── 舰操绘算.apk                ← 最新
    └── archive/舰操绘算_vX.Y.apk   ← 按版本归档
```

> `build/`、`assets/`、`out/`、`*.keystore` 都不进库。

---

## 构建

本机依赖：`E:\Project\playground\apk_playground\tools\`（JDK 21 + build-tools 34.0.0 + android-34）、Python 3、Git Bash。

```bash
cd /e/Project/Maneuvering_Plotting_Simulator
bash build.sh
```

### 升版本

```bash
echo v1.1 > VERSION
bash build.sh
git add -A && git commit -m "v1.1：xxx"
git tag -a v1.1 -m "v1.1"
git push origin main --tags
```

安装：电脑微信 → 文件传输助手 → 发 `.apk` → 手机允许「未知来源」→ 装。

> ⚠️ `debug.keystore` 千万别丢、别换。换了章手机就不认，只能卸载重装（画的东西全丢）。

---

## 操作速查（手机）

| 操作 | 手势 |
|---|---|
| 画线段 | 点起点 → 移动看读数 → 点终点（也可按住直接拖） |
| 画圆 | 点圆心 → 点圆周 |
| 选中 / 移动 | 单击选中，拖本体平移，拖端点只转方向 |
| 精确调线 | 画完自动跳到「辅助」页，直接打数字改角度/长度/坐标 |
| 缩放 | 双指捏合（0.4× ~ 50×） |
| 平移 | 双指拖动 |
| 回整图 | 双指轻点 |
| 取消 | 右上角 ✕ |

---

## 已知问题

- **高倍率下底图是糊的**：底图是 1027×978 的位图，超过约 12 倍就是在放大马赛克（已有提示 + 自动提对比压噪点）。
  - 试过自动矢量化（vtracer），线稿被糊成色斑、点阵并成一坨，**不可用**，3.9 MB 的体积也不适合塞进 APK。
  - 想真正清晰只有两条路：拿到更高清的原图，或按几何重画底图。
  - **当前策略**：不做高清底图，用「线段数据面板」的数值精度代替放大精度。

---

## 版本记录

| 版本 | 日期 | 内容 |
|---|---|---|
| v1.0 | 2026-11-05 | 竖屏目录 + 横屏工具；绘图/圆/点/文字/量距/数值落点；DCPA·TCPA + 速度三角形；习题框架；说明书 14 章；明暗主题；存相册；**线段数据面板** |
