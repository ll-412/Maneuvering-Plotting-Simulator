package com.example.jiancao;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String ALBUM = "舰操绘算";

    private WebView web;
    private long lastBack = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 画图时会长时间不碰屏幕，别让它息屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // localStorage 存档
        s.setAllowFileAccess(true);            // 读 assets 里的网页
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(this), "Android");
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);

        applyImmersive();
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    /** 全屏沉浸：隐藏状态栏 + 导航栏 */
    private void applyImmersive() {
        View d = getWindow().getDecorView();
        d.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersive();
            // 此时布局已完成，WebView 有了真实宽高，才能算手势排除区
            excludeEdgeGestures();
        }
    }

    /**
     * 把屏幕左右各 26dp 从「侧边滑动返回」手势里排除。
     * 否则贴着画布左边缘画线会直接退出 App（真我/小米的手势导航都这样）。
     */
    private void excludeEdgeGestures() {
        if (Build.VERSION.SDK_INT < 29) return;
        int w = web.getWidth();
        int h = web.getHeight();
        if (w <= 0 || h <= 0) return;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int edge = (int) (26 * dm.density);
        edge = Math.min(edge, w / 4);
        List<Rect> rects = new ArrayList<>();
        rects.add(new Rect(0, 0, edge, h));
        rects.add(new Rect(w - edge, 0, w, h));
        try {
            web.setSystemGestureExclusionRects(rects);
        } catch (Throwable ignored) {
        }
    }

    /** 返回键：交给网页决定（子页→回首页；首页→问要不要保存） */
    @Override
    public void onBackPressed() {
        if (web == null) {
            super.onBackPressed();
            return;
        }
        web.evaluateJavascript("window.__onBack && window.__onBack()", null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
        applyImmersive();
    }

    @Override
    protected void onPause() {
        if (web != null) web.onPause();
        super.onPause();
    }
}
