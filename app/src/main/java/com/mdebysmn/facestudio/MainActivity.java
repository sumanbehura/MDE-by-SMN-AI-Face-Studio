package com.mdebysmn.facestudio;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import android.util.Base64;

public class MainActivity extends Activity {
    LinearLayout root, content;
    ImageView sourcePreview, targetPreview;
    TextView status, strengthValue;
    Spinner engine;
    SeekBar strength;
    Uri sourceUri, targetUri;
    ExecutorService pool = Executors.newSingleThreadExecutor();
    final int SRC = 101, TGT = 102;

    final int BG = Color.rgb(8,8,12);
    final int PANEL = Color.rgb(18,18,25);
    final int PANEL2 = Color.rgb(25,25,34);
    final int TEXT = Color.WHITE;
    final int MUTED = Color.rgb(164,164,178);
    final int ACCENT = Color.rgb(139,92,246);

    String[] engines = {
        "Fast Swap — InSwapper",
        "High Fidelity — Multi-model",
        "Restore + Enhance",
        "Generative Identity"
    };

    int dp(float x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }

    GradientDrawable bg(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radius));
        return g;
    }

    TextView text(String s, float sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(sp);
        return t;
    }

    TextView label(String s) {
        TextView t = text(s, 12, MUTED);
        t.setLetterSpacing(.08f);
        t.setTypeface(null, 1);
        return t;
    }

    Button actionButton(String s, boolean primary) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(null, 1);
        b.setTextColor(TEXT);
        b.setPadding(dp(16), 0, dp(16), 0);
        b.setMinHeight(dp(48));
        b.setBackground(bg(primary ? ACCENT : PANEL2, 16));
        return b;
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        build();
    }

    void build() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(14), dp(20), dp(10));

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.mdebysmn.facestudio.R.mipmap.ic_launcher);
        bar.addView(logo, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(12), 0, 0, 0);
        TextView name = text("MDE by SMN", 18, TEXT);
        name.setTypeface(null, 1);
        brand.addView(name);
        brand.addView(text("AI Face Studio", 12, MUTED));
        bar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        TextView version = text("1.0", 11, MUTED);
        version.setPadding(dp(10), dp(6), dp(10), dp(6));
        version.setBackground(bg(PANEL2, 20));
        bar.addView(version);
        root.addView(bar);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(8), dp(20), dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView hero = text("Create. Swap. Enhance.", 27, TEXT);
        hero.setTypeface(null, 1);
        content.addView(hero, margin(0, 8, 0, 4));
        content.addView(text("High-fidelity face editing with identity controls.", 14, MUTED), margin(0,0,0,18));

        LinearLayout previews = new LinearLayout(this);
        previews.setOrientation(LinearLayout.HORIZONTAL);
        previews.setWeightSum(2);
        sourcePreview = preview("SOURCE FACE");
        targetPreview = preview("TARGET IMAGE");
        previews.addView(sourcePreview, new LinearLayout.LayoutParams(0, dp(178), 1));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, dp(178), 1);
        tp.setMargins(dp(10),0,0,0);
        previews.addView(targetPreview, tp);
        content.addView(previews);

        LinearLayout pickRow = new LinearLayout(this);
        pickRow.setWeightSum(2);
        Button bs = actionButton("Choose source", false);
        bs.setOnClickListener(v -> pick(SRC));
        Button bt = actionButton("Choose target", false);
        bt.setOnClickListener(v -> pick(TGT));
        pickRow.addView(bs, new LinearLayout.LayoutParams(0, dp(50), 1));
        LinearLayout.LayoutParams bm = new LinearLayout.LayoutParams(0, dp(50), 1);
        bm.setMargins(dp(10),0,0,0);
        pickRow.addView(bt, bm);
        content.addView(pickRow, margin(0,10,0,18));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.setBackground(bg(PANEL, 20));

        card.addView(label("GENERATION ENGINE"), margin(0,0,0,8));
        engine = new Spinner(this);
        engine.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, engines));
        card.addView(engine, new LinearLayout.LayoutParams(-1, dp(48)));

        LinearLayout strengthRow = new LinearLayout(this);
        strengthRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView st = text("Identity strength", 14, TEXT);
        strengthRow.addView(st, new LinearLayout.LayoutParams(0, dp(36), 1));
        strengthValue = text("80%", 13, MUTED);
        strengthRow.addView(strengthValue);
        card.addView(strengthRow, margin(0,14,0,0));

        strength = new SeekBar(this);
        strength.setMax(100);
        strength.setProgress(80);
        strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean f) { strengthValue.setText(p + "%"); }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        card.addView(strength);

        TextView hint = text("Higher values prioritize source identity; lower values preserve more target facial structure.", 12, MUTED);
        card.addView(hint, margin(0,4,0,0));
        content.addView(card, margin(0,0,0,12));

        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setPadding(dp(16), dp(10), dp(16), dp(8));
        options.setBackground(bg(PANEL, 20));

        CheckBox restore = new CheckBox(this);
        restore.setText("Face restoration");
        restore.setTextColor(TEXT);
        restore.setTextSize(14);
        restore.setChecked(true);
        options.addView(restore);

        CheckBox upscale = new CheckBox(this);
        upscale.setText("AI upscale (4× / 8K when supported)");
        upscale.setTextColor(TEXT);
        upscale.setTextSize(14);
        upscale.setChecked(true);
        options.addView(upscale);

        TextView privacy = text("Use images you have permission to edit. Processing may send images to your configured backend.", 11, MUTED);
        privacy.setPadding(dp(4), dp(4), dp(4), dp(8));
        options.addView(privacy);
        content.addView(options, margin(0,0,0,14));

        Button run = actionButton("Generate image", true);
        run.setTextSize(16);
        run.setMinHeight(dp(56));
        run.setOnClickListener(v -> generate(restore.isChecked(), upscale.isChecked()));
        content.addView(run);

        Button settings = actionButton("Backend & API settings", false);
        settings.setOnClickListener(v -> settingsDialog());
        content.addView(settings, margin(0,10,0,0));

        status = text("Ready — connect an inference backend to generate.", 12, MUTED);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(12), dp(14), dp(12), dp(4));
        content.addView(status);

        setContentView(root);
    }

    LinearLayout.LayoutParams margin(int l,int t,int r,int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(dp(l),dp(t),dp(r),dp(b));
        return p;
    }

    ImageView preview(String label) {
        ImageView i = new ImageView(this);
        i.setBackground(bg(PANEL, 18));
        i.setScaleType(ImageView.ScaleType.CENTER_CROP);
        i.setContentDescription(label);
        i.setPadding(dp(4),dp(4),dp(4),dp(4));
        i.setImageDrawable(new LabelDrawable(label, MUTED));
        return i;
    }

    class LabelDrawable extends Drawable {
        String s; int c;
        Paint p = new Paint(1);
        LabelDrawable(String s,int c){this.s=s;this.c=c;}
        public void draw(Canvas canvas){
            p.setColor(c); p.setTextSize(dp(11)); p.setTypeface(Typeface.DEFAULT_BOLD);
            p.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(s,getBounds().centerX(),getBounds().centerY(),p);
        }
        public void setAlpha(int a){p.setAlpha(a);}
        public void setColorFilter(android.graphics.ColorFilter f){p.setColorFilter(f);}
        public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }

    void pick(int code) {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(i, code);
    }

    @Override protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);
        if(c!=RESULT_OK || d==null) return;
        Uri u=d.getData();
        if(r==SRC){sourceUri=u;sourcePreview.setImageURI(u);}
        else {targetUri=u;targetPreview.setImageURI(u);}
    }

    String prefs(String k){return getPreferences(0).getString(k,"");}

    void settingsDialog(){
        LinearLayout l=new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20),dp(4),dp(20),0);
        EditText url=new EditText(this);
        url.setHint("https://your-inference-endpoint");
        url.setSingleLine(true);
        url.setText(prefs("url"));
        l.addView(url);
        EditText key=new EditText(this);
        key.setHint("API key (optional)");
        key.setSingleLine(true);
        key.setInputType(129);
        key.setText(prefs("key"));
        l.addView(key);
        new AlertDialog.Builder(this)
            .setTitle("Inference backend")
            .setMessage("MDE by SMN is the client. Heavy AI models run on your configured backend.")
            .setView(l)
            .setPositiveButton("Save",(d,w)->{
                getPreferences(0).edit().putString("url",url.getText().toString().trim()).putString("key",key.getText().toString().trim()).apply();
                status.setText("Backend settings saved.");
            }).setNegativeButton("Cancel",null).show();
    }

    byte[] read(Uri u)throws Exception{
        InputStream in=getContentResolver().openInputStream(u);
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] b=new byte[8192]; int n;
        while((n=in.read(b))>0)out.write(b,0,n);
        in.close(); return out.toByteArray();
    }

    void generate(boolean restore,boolean upscale){
        if(sourceUri==null||targetUri==null){status.setText("Choose both source and target images first.");return;}
        String url=prefs("url");
        if(url.isEmpty()){settingsDialog();return;}
        status.setText("Processing…");
        pool.submit(()->{
            try{
                String body="{"+
                    "\"engine\":"+q(engine.getSelectedItem().toString())+
                    ",\"strength\":"+(strength.getProgress()/100.0)+
                    ",\"restore\":"+restore+
                    ",\"upscale\":"+upscale+
                    ",\"source_image_base64\":"+q(Base64.encodeToString(read(sourceUri),Base64.NO_WRAP))+
                    ",\"target_image_base64\":"+q(Base64.encodeToString(read(targetUri),Base64.NO_WRAP))+"}";
                HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();
                h.setRequestMethod("POST"); h.setConnectTimeout(30000); h.setReadTimeout(300000); h.setDoOutput(true);
                h.setRequestProperty("Content-Type","application/json");
                String key=prefs("key"); if(!key.isEmpty())h.setRequestProperty("Authorization","Bearer "+key);
                h.getOutputStream().write(body.getBytes("UTF-8"));
                int code=h.getResponseCode();
                InputStream in=code>=200&&code<300?h.getInputStream():h.getErrorStream();
                ByteArrayOutputStream out=new ByteArrayOutputStream();
                byte[] bb=new byte[8192]; int n;
                while(in!=null&&(n=in.read(bb))>0)out.write(bb,0,n);
                String resp=new String(out.toByteArray(),"UTF-8");
                runOnUiThread(()->status.setText(code>=200&&code<300?"Generation request completed. Backend response received.":"Backend error "+code+": "+resp));
            }catch(Exception e){runOnUiThread(()->status.setText("Connection failed: "+e.getMessage()));}
        });
    }

    String q(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}

    @Override protected void onDestroy(){pool.shutdownNow();super.onDestroy();}
}
