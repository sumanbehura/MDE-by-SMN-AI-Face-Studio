package com.mdebysmn.h3;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;

public class H3MainActivity extends Activity {
    static final String PREFS = "h3";
    static final String URL = "gateway_url";
    LinearLayout root;
    SharedPreferences prefs;

    int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }

    GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    TextView text(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
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
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(24));
        root.setBackgroundColor(Color.rgb(8,8,12));

        TextView title = text("MDE by SMN", 25, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        TextView sub = text("MiniMax H3 • Colab Gateway", 14, Color.rgb(170,170,185));
        root.addView(sub);

        TextView info = text("\nStart the Colab notebook, wait for the Gradio public URL, then paste it below.\n\nThe gateway forwards generation requests to the Hugging Face MiniMax H3 Space and shows the daily usage counter.", 14, Color.rgb(190,190,205));
        root.addView(info);

        EditText url = new EditText(this);
        url.setHint("https://xxxx.gradio.live");
        url.setText(prefs.getString(URL, ""));
        url.setTextColor(Color.WHITE);
        url.setHintTextColor(Color.rgb(120,120,135));
        url.setSingleLine(true);
        url.setPadding(dp(14), dp(12), dp(14), dp(12));
        url.setBackground(bg(Color.rgb(24,24,33), 14));
        LinearLayout.LayoutParams up = new LinearLayout.LayoutParams(-1, dp(54));
        up.setMargins(0, dp(20), 0, dp(12));
        root.addView(url, up);

        Button connect = new Button(this);
        connect.setText("Connect to H3");
        connect.setTextColor(Color.WHITE);
        connect.setAllCaps(false);
        connect.setTextSize(16);
        connect.setBackground(bg(Color.rgb(139,92,246), 16));
        connect.setOnClickListener(v -> {
            String value = url.getText().toString().trim();
            if (!value.startsWith("https://")) {
                Toast.makeText(this, "Use the https Gradio URL from Colab.", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString(URL, value).apply();
            showWeb(value);
        });
        root.addView(connect, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView note = text("\nThe current MiniMax H3 Space is a large unquantized split deployment; the Colab T4 is used as the gateway rather than pretending it can fit H3 locally.", 12, Color.rgb(145,145,160));
        root.addView(note);

        setContentView(root);
    }

    void showWeb(String url) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8,8,12));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), dp(6), dp(8), dp(6));
        bar.setBackgroundColor(Color.rgb(18,18,25));

        TextView title = text("MDE × MiniMax H3", 15, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        bar.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button settings = new Button(this);
        settings.setText("Gateway");
        settings.setAllCaps(false);
        settings.setTextColor(Color.WHITE);
        settings.setBackground(bg(Color.rgb(35,35,46), 12));
        settings.setOnClickListener(v -> showConnect());
        bar.addView(settings, new LinearLayout.LayoutParams(dp(100), dp(44)));

        root.addView(bar);

        WebView web = new WebView(this);
        web.setBackgroundColor(Color.rgb(8,8,12));
        web.setWebViewClient(new WebViewClient());
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        web.loadUrl(url);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    @Override public void onBackPressed() {
        if (root != null && root.getChildCount() > 0) {
            super.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }
}
