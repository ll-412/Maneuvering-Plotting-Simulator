package com.example.jiancao;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 网页 <-> 安卓 的桥。
 * 独立成顶层类，避免 d8 在处理内部类时崩（该项目用的 d8 8.2.2 有这毛病）。
 */
class Bridge implements Runnable {

    private static final String ALBUM = "舰操绘算";

    /* 授权码用的密钥。改了它就等于让所有已发出的授权码作废。 */
    private static final String LIC_KEY = "JC-Plot-2026-1155-Lic";

    /** 网页当前在哪一屏，给返回键判断用 */
    static String currentScreen = "home";

    private final Context ctx;
    private final Activity act;

    Bridge(Activity a) {
        this.ctx = a;
        this.act = a;
    }

    /** 网页调用：切换横竖屏（进舰操模拟转横屏，回目录转竖屏） */
    @JavascriptInterface
    public void setScreen(String id) {
        currentScreen = (id == null) ? "home" : id;
    }

    @JavascriptInterface
    public void setOrientation(String mode) {
        if (act == null) return;
        act.setRequestedOrientation("landscape".equals(mode)
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /** 网页调用：Android.saveImage(png的base64) → 存进相册 */
    @JavascriptInterface
    public boolean saveImage(String base64) {
        try {
            byte[] data = Base64.decode(base64, Base64.DEFAULT);
            String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String name = ALBUM + "_" + stamp + ".jpg";
            if (Build.VERSION.SDK_INT >= 29) return saveQ(data, name);
            return saveLegacy(data, name);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 网页调用：退出 App（退出询问里点「保存并退出 / 不保存」之后） */
    /* ---------- 授权码（一人一码） ---------- */

    /** 本机机器码：ANDROID_ID 的摘要，形如 K7X2-9M4P */
    @JavascriptInterface
    public String deviceCode() {
        String id = null;
        try {
            id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Throwable ignored) {
        }
        if (id == null || id.trim().length() == 0) {
            try {
                id = Build.FINGERPRINT + "|" + Build.MODEL + "|" + Build.MANUFACTURER;
            } catch (Throwable t) {
                id = "unknown-device";
            }
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((id + "|jcplot").getBytes(Charset.forName("UTF-8")));
            String h = hexOf(d).substring(0, 8);
            return h.substring(0, 4) + "-" + h.substring(4, 8);
        } catch (Throwable t) {
            return "";
        }
    }

    /** 网页传入授权码，返回是否匹配本机 */
    @JavascriptInterface
    public boolean checkLicense(String input) {
        String want = licenseFor(deviceCode());
        if (want.length() == 0) return false;
        String got = (input == null) ? "" : input.toUpperCase(Locale.US).replaceAll("[^A-Z0-9]", "");
        return want.replace("-", "").equals(got);
    }

    private static String licenseFor(String device) {
        try {
            String msg = (device == null) ? "" : device.toUpperCase(Locale.US).replaceAll("[^A-Z0-9]", "");
            if (msg.length() < 6) return "";
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(LIC_KEY.getBytes(Charset.forName("UTF-8")), "HmacSHA256"));
            String h = hexOf(mac.doFinal(msg.getBytes(Charset.forName("UTF-8"))));
            return h.substring(0, 4) + "-" + h.substring(4, 8);
        } catch (Throwable t) {
            return "";
        }
    }

    private static String hexOf(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(Character.forDigit((x >> 4) & 0xF, 16));
            sb.append(Character.forDigit(x & 0xF, 16));
        }
        return sb.toString().toUpperCase(Locale.US);
    }

    /** 网页调用：把文字复制到剪贴板（方便把机器码发给主人） */
    @JavascriptInterface
    public void copyText(String s) {
        if (act == null) return;
        act.runOnUiThread(new UiCopy(act, s));
    }

    @JavascriptInterface
    public void exitApp() {
        if (act == null) return;
        act.runOnUiThread(this);          // 不写匿名类，否则 d8 会崩
    }

    /** 切回主线程后真正执行 finish */
    @Override
    public void run() {
        if (act != null) act.finish();
    }

    @JavascriptInterface
    public String info() {
        return "android sdk=" + Build.VERSION.SDK_INT;
    }

    /** Android 10+：走 MediaStore，不需要存储权限 */
    private boolean saveQ(byte[] data, String name) {
        ContentValues v = new ContentValues();
        v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + ALBUM);
        v.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri uri = null;
        try {
            uri = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri == null) return false;
            OutputStream os = ctx.getContentResolver().openOutputStream(uri);
            if (os == null) return false;
            try {
                os.write(data);
                os.flush();
            } finally {
                os.close();
            }
            ContentValues done = new ContentValues();
            done.put(MediaStore.Images.Media.IS_PENDING, 0);
            ctx.getContentResolver().update(uri, done, null, null);
            return true;
        } catch (Throwable t) {
            if (uri != null) {
                try { ctx.getContentResolver().delete(uri, null, null); } catch (Throwable ignored) {}
            }
            return false;
        }
    }

    /** Android 9 及以下：直接写公共目录 + 通知媒体库刷新 */
    @SuppressWarnings("deprecation")
    private boolean saveLegacy(byte[] data, String name) {
        try {
            File dir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    ALBUM);
            if (!dir.exists() && !dir.mkdirs()) return false;
            File f = new File(dir, name);
            FileOutputStream fos = new FileOutputStream(f);
            try {
                fos.write(data);
                fos.flush();
            } finally {
                fos.close();
            }
            MediaScannerConnection.scanFile(ctx, new String[]{ f.getAbsolutePath() },
                    new String[]{ "image/jpeg" }, null);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}

/**
 * 把一段文字放进剪贴板。
 * 必须是顶层类：匿名类/内部类会让 d8 崩。
 */
class UiCopy implements Runnable {

    private final Activity act;
    private final String text;

    UiCopy(Activity a, String t) {
        this.act = a;
        this.text = (t == null) ? "" : t;
    }

    @Override
    public void run() {
        try {
            ClipboardManager cm = (ClipboardManager) act.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return;
            cm.setPrimaryClip(ClipData.newPlainText("jiancao", text));
        } catch (Throwable ignored) {
        }
    }
}
