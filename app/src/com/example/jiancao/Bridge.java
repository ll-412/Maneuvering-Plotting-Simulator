package com.example.jiancao;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 网页 <-> 安卓 的桥。
 * 独立成顶层类，避免 d8 在处理内部类时崩（该项目用的 d8 8.2.2 有这毛病）。
 */
class Bridge {

    private static final String ALBUM = "舰操绘算";

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
