package com.mdebysmn.h3;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;

public class H3MainActivity extends Activity {
    static final String PREFS = "h3";
    static final String SPACE_URL = "space_url";
    static final String DEFAULT_SPACE = "https://observantdistressed-minimax-h3.hf.space/";
    LinearLayout root;
    SharedPreferences prefs;
    WebView web;
    TextView status;
    ValueCallback<Uri[]> uploadCallback;

    int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    TextView text(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8,8,12));
        getWindow().setNavigationBarColor(Color.rgb(8,8,12));
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        showConnect();
    }

    void showConnect() {
        web = null;
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(24));
        root.setBackgroundColor(Color.rgb(8,8,12));

        TextView title = text("MDE by SMN", 25, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        TextView sub = text("MiniMax H3 • Hugging Face", 14, Color.rgb(170,170,185));
        root.addView(sub);

        TextView info = text(
            "\nDirect connection to the official H3 Space.\n\n" +
            "No Colab T4, gateway, proxy, or intermediate server. " +
            "Hugging Face performs the actual H3 generation.",
            14, Color.rgb(190,190,205));
        root.addView(info);

        EditText url = new EditText(this);
        url.setHint("Hugging Face Space URL");
        url.setText(prefs.getString(SPACE_URL, DEFAULT_SPACE));
        url.setTextColor(Color.WHITE);
        url.setHintTextColor(Color.rgb(120,120,135));
        url.setSingleLine(true);
        url.setPadding(dp(14), dp(12), dp(14), dp(12));
        url.setBackground(bg(Color.rgb(24,24,33), 14));
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(-1, dp(54));
        up.setMargins(0, dp(20), 0, dp(12));
        root.addView(url, up);

        Button connect = new Button(this);
        connect.setText("Open MiniMax H3");
        connect.setTextColor(Color.WHITE);
        connect.setAllCaps(false);
        connect.setTextSize(16);
        connect.setBackground(bg(Color.rgb(139,92,246), 16));
        connect.setOnClickListener(v -> {
            String value = url.getText().toString().trim();
            if (!value.startsWith("https://huggingface.co/spaces/") &&
                !value.startsWith("https://observantdistressed-minimax-h3.hf.space")) {
                Toast.makeText(this, "Use the official MiniMax H3 Hugging Face Space URL.", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString(SPACE_URL, value).apply();
            showWeb(normalizeSpaceUrl(value));
        });
        root.addView(connect, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView note = text(
            "\nUpload support is enabled for H3 image/keyframe inputs. " +
            "The app also shows connection status so you can tell when the Space is loaded.",
            12, Color.rgb(145,145,160));
        root.addView(note);

        setContentView(root);
    }

    String normalizeSpaceUrl(String value) {
        if (value.startsWith("https://huggingface.co/spaces/observantdistressed/minimax-h3")) {
            return DEFAULT_SPACE;
        }
        return value.endsWith("/") ? value : value + "/";
    }

    void showWeb(String url) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8,8,12));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(4), dp(8), dp(4));
        bar.setBackgroundColor(Color.rgb(18,18,25));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("MDE × MiniMax H3", 15, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        labels.addView(title);
        status = text("Connecting to Hugging Face…", 11, Color.rgb(170,170,185));
        labels.addView(status);
        bar.addView(labels, new LinearLayout.LayoutParams(0, dp(52), 1));

        Button home = new Button(this);
        home.setText("Home");
        home.setAllCaps(false);
        home.setTextColor(Color.WHITE);
        home.setBackground(bg(Color.rgb(35,35,46), 12));
        home.setOnClickListener(v -> showConnect());
        bar.addView(home, new LinearLayout.LayoutParams(dp(90), dp(44)));
        root.addView(bar);

        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(8,8,12));
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String loadedUrl) {
                if (status != null) status.setText("Hugging Face Space loaded • Ready");
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame() && status != null) {
                    status.setText("Connection error • Check internet/Hugging Face status");
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (uploadCallback != null) uploadCallback.onReceiveValue(null);
                uploadCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), 1001);
                    return true;
                } catch (Exception e) {
                    uploadCallback = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(H3MainActivity.this, "Unable to open the file picker.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }

            @Override public void onProgressChanged(WebView view, int newProgress) {
                if (status != null && newProgress < 100) status.setText("Loading H3 Space… " + newProgress + "%");
            }
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        web.loadUrl(url);

        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && uploadCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) {
                    results = new Uri[]{data.getData()};
                }
            }
            uploadCallback.onReceiveValue(results);
            uploadCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}