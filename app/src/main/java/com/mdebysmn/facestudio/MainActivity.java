package com.mdebysmn.facestudio;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import android.util.Base64;

public class MainActivity extends Activity {
    LinearLayout root, controls;
    ImageView sourcePreview, targetPreview;
    TextView status;
    Spinner engine;
    SeekBar strength;
    Uri sourceUri, targetUri;
    ExecutorService pool = Executors.newSingleThreadExecutor();
    final int SRC = 101, TGT = 102;

    String[] engines = {
        "Fast Swap — InSwapper",
        "High Fidelity — Multi-model",
        "Restore + Enhance",
        "Generative Identity"
    };

    int dp(float x) {
        return (int)(x * getResources().getDisplayMetrics().density + .5f);
    }

    TextView tv(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(sp);
        t.setPadding(dp(12), dp(8), dp(12), dp(8));
        return t;
    }

    Button btn(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        return b;
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        build();
    }

    void build() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(11, 11, 15));

        TextView title = tv("MDE by SMN", 26);
        title.setTypeface(null, 1);
        root.addView(title);

        TextView sub = tv("AI Face Studio", 18);
        sub.setTextColor(Color.rgb(167, 167, 179));
        root.addView(sub);

        ScrollView sv = new ScrollView(this);
        controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        sv.addView(controls);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        sourcePreview = preview("SOURCE");
        targetPreview = preview("TARGET");
        row.addView(sourcePreview, new LinearLayout.LayoutParams(0, dp(150), 1));
        row.addView(targetPreview, new LinearLayout.LayoutParams(0, dp(150), 1));
        controls.addView(row);

        Button bs = btn("Choose source face");
        bs.setOnClickListener(v -> pick(SRC));
        controls.addView(bs);

        Button bt = btn("Choose target image");
        bt.setOnClickListener(v -> pick(TGT));
        controls.addView(bt);

        controls.addView(tv("Engine", 16));
        engine = new Spinner(this);
        engine.setAdapter(new ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_dropdown_item, engines));
        controls.addView(engine);

        controls.addView(tv("Identity strength", 16));
        strength = new SeekBar(this);
        strength.setMax(100);
        strength.setProgress(80);
        controls.addView(strength);

        controls.addView(tv(
            "Preserve expression • face shape • hairline/ears • target mouth", 13));

        CheckBox restore = new CheckBox(this);
        restore.setText("Face restoration");
        restore.setTextColor(Color.WHITE);
        restore.setChecked(true);
        controls.addView(restore);

        CheckBox upscale = new CheckBox(this);
        upscale.setText("AI upscale (4× / 8K output when backend supports it)");
        upscale.setTextColor(Color.WHITE);
        upscale.setChecked(true);
        controls.addView(upscale);

        Button run = btn("GENERATE");
        run.setOnClickListener(v -> generate(restore.isChecked(), upscale.isChecked()));
        controls.addView(run);

        Button settings = btn("Backend settings");
        settings.setOnClickListener(v -> settingsDialog());
        controls.addView(settings);

        status = tv(
            "Backend not configured. Add your inference endpoint in Backend settings.", 13);
        status.setTextColor(Color.rgb(167, 167, 179));
        controls.addView(status);

        setContentView(root);
    }

    ImageView preview(String label) {
        ImageView i = new ImageView(this);
        i.setBackgroundColor(Color.rgb(21, 21, 28));
        i.setScaleType(ImageView.ScaleType.CENTER_CROP);
        i.setContentDescription(label);
        return i;
    }

    void pick(int code) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, code);
    }

    @Override protected void onActivityResult(int r, int c, Intent d) {
        super.onActivityResult(r, c, d);
        if (c != RESULT_OK || d == null) return;
        Uri u = d.getData();
        if (r == SRC) {
            sourceUri = u;
            sourcePreview.setImageURI(u);
        } else {
            targetUri = u;
            targetPreview.setImageURI(u);
        }
    }

    String prefs(String k) {
        return getPreferences(0).getString(k, "");
    }

    void settingsDialog() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20), dp(8), dp(20), 0);

        EditText url = new EditText(this);
        url.setHint("https://your-inference-endpoint");
        url.setText(prefs("url"));
        l.addView(url);

        EditText key = new EditText(this);
        key.setHint("API key (optional)");
        key.setText(prefs("key"));
        l.addView(key);

        new AlertDialog.Builder(this)
            .setTitle("Inference backend")
            .setMessage("Use a backend you are authorized to access. The APK is the client; heavy AI models run on the backend.")
            .setView(l)
            .setPositiveButton("Save", (d, w) -> {
                getPreferences(0).edit()
                    .putString("url", url.getText().toString().trim())
                    .putString("key", key.getText().toString().trim())
                    .apply();
                status.setText("Backend saved.");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    byte[] read(Uri u) throws Exception {
        InputStream in = getContentResolver().openInputStream(u);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) out.write(b, 0, n);
        in.close();
        return out.toByteArray();
    }

    void generate(boolean restore, boolean upscale) {
        if (sourceUri == null || targetUri == null) {
            status.setText("Choose both source and target images first.");
            return;
        }

        String url = prefs("url");
        if (url.isEmpty()) {
            settingsDialog();
            return;
        }

        status.setText("Processing…");

        pool.submit(() -> {
            try {
                String body = "{"
                    + "\"engine\":" + q(engine.getSelectedItem().toString())
                    + ",\"strength\":" + (strength.getProgress() / 100.0)
                    + ",\"restore\":" + restore
                    + ",\"upscale\":" + upscale
                    + ",\"source_image_base64\":"
                    + q(Base64.encodeToString(read(sourceUri), Base64.NO_WRAP))
                    + ",\"target_image_base64\":"
                    + q(Base64.encodeToString(read(targetUri), Base64.NO_WRAP))
                    + "}";

                HttpURLConnection h =
                    (HttpURLConnection)new URL(url).openConnection();
                h.setRequestMethod("POST");
                h.setConnectTimeout(30000);
                h.setReadTimeout(300000);
                h.setDoOutput(true);
                h.setRequestProperty("Content-Type", "application/json");

                String key = prefs("key");
                if (!key.isEmpty())
                    h.setRequestProperty("Authorization", "Bearer " + key);

                h.getOutputStream().write(body.getBytes("UTF-8"));

                int code = h.getResponseCode();
                InputStream in = code >= 200 && code < 300
                    ? h.getInputStream() : h.getErrorStream();

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] bb = new byte[8192];
                int n;
                while ((n = in.read(bb)) > 0) out.write(bb, 0, n);

                String resp = new String(out.toByteArray(), "UTF-8");

                runOnUiThread(() -> status.setText(
                    code >= 200 && code < 300
                        ? "Generation submitted successfully. Backend response received."
                        : "Backend error " + code + ": " + resp));
            } catch (Exception e) {
                runOnUiThread(() ->
                    status.setText("Connection failed: " + e.getMessage()));
            }
        });
    }

    String q(String s) {
        return "\""
            + s.replace("\\", "\\\\").replace("\"", "\\\"")
            + "\"";
    }

    @Override protected void onDestroy() {
        pool.shutdownNow();
        super.onDestroy();
    }
}
