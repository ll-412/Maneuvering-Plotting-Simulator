#!/bin/bash
# ============================================================
#  舰操绘算 · 手工构建 WebView 壳 APK（无 Gradle）
#  用法：bash build.sh
#  产物：out/舰操绘算.apk  +  out/archive/舰操绘算_vX.Y.apk
# ============================================================
set -e
cd "$(dirname "$0")"

# 工具链放在 playground/apk_playground/tools（559MB，不进这个仓库）
TOOLS=/e/Project/playground/apk_playground/tools
JDK=$TOOLS/jdk/jdk-21.0.12.1+1
BT=$TOOLS/sdk/build-tools/34.0.0
PLATFORM=$TOOLS/sdk/platforms/android-34/android.jar
export JAVA_HOME='E:\Project\playground\apk_playground\tools\jdk\jdk-21.0.12.1+1'

APP=app
PKG=com/example/jiancao

VER=$(cat VERSION 2>/dev/null | tr -d '[:space:]')
[ -z "$VER" ] && VER="v1.0"

echo "===== 1. 生成图标 ====="
python icon_gen.py

echo "===== 2. 同步网页（版本 $VER）====="
mkdir -p assets build/gen build/obj out/archive
cp "web/舰操绘算手机版.html" assets/index.html
sed -i "s/window\.APP_VERSION = '[^']*'/window.APP_VERSION = '$VER'/" assets/index.html
echo "assets/index.html 已同步"

echo "===== 3. 签名密钥 ====="
if [ ! -f debug.keystore ]; then
  echo "！找不到 debug.keystore，新建一个"
  "$JDK/bin/keytool" -genkeypair -keystore debug.keystore \
    -storepass android -keypass android -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" 2>/dev/null
fi
echo "keystore 就绪"

echo "===== 4. 编译资源 ====="
rm -rf build && mkdir -p build/gen build/obj
"$BT/aapt2" compile --dir $APP/res -o build/res.zip

echo "===== 5. 链接 + 打包 assets ====="
"$BT/aapt2" link -o build/base.apk \
  -I "$PLATFORM" \
  --manifest $APP/AndroidManifest.xml \
  -R build/res.zip --auto-add-overlay \
  -A assets \
  --java build/gen

echo "===== 6. 编译 Java → dex ====="
"$JDK/bin/javac" --release 8 -classpath "$PLATFORM" \
  -d build/obj $APP/src/$PKG/*.java
"$BT/d8.bat" --release --lib "$PLATFORM" \
  --output build build/obj/$PKG/*.class

echo "===== 7. 塞进 APK ====="
python - <<EOF
import zipfile
apk = zipfile.ZipFile("build/base.apk", "a")
apk.write("build/classes.dex", "classes.dex")
apk.close()
print("classes.dex 已加入")
EOF

echo "===== 8. 对齐 + 签名 ====="
"$BT/zipalign" -f 4 build/base.apk build/aligned.apk
"$BT/apksigner.bat" sign --ks debug.keystore \
  --ks-pass pass:android --key-pass pass:android \
  --out "out/舰操绘算.apk" build/aligned.apk
"$BT/apksigner.bat" verify --print-certs "out/舰操绘算.apk" | head -2
ls -la "out/舰操绘算.apk"

echo "===== 9. 归档这一版 ====="
cp "out/舰操绘算.apk" "out/archive/舰操绘算_${VER}.apk"
echo "已归档：out/archive/舰操绘算_${VER}.apk"

echo ""
echo "===== 完成 ====="
