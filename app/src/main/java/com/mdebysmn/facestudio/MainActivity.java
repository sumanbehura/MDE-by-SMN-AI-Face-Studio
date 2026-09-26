package com.mdebysmn.facestudio;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.List;

public class MainActivity extends Activity {
    LinearLayout root, content;
    ImageView sourcePreview, targetPreview, resultPreview;
    TextView status, strengthValue;
    SeekBar strength;
    Uri sourceUri, targetUri;
    ExecutorService pool = Executors.newSingleThreadExecutor();
    FaceFusionProcessor processor;
    Bitmap sourceBitmap, targetBitmap, resultBitmap;
    final int SRC=101, TGT=102;

    final int BG=Color.rgb(8,8,12), PANEL=Color.rgb(18,18,25), PANEL2=Color.rgb(25,25,34);
    final int TEXT=Color.WHITE, MUTED=Color.rgb(164,164,178), ACCENT=Color.rgb(139,92,246);

    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    GradientDrawable bg(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}
    TextView text(String s,float sp,int c){TextView t=new TextView(this);t.setText(s);t.setTextColor(c);t.setTextSize(sp);return t;}
    TextView label(String s){TextView t=text(s,12,MUTED);t.setLetterSpacing(.08f);t.setTypeface(null,1);return t;}
    Button button(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);b.setTypeface(null,1);b.setTextColor(TEXT);b.setMinHeight(dp(50));b.setBackground(bg(primary?ACCENT:PANEL2,16));return b;}
    LinearLayout.LayoutParams margin(int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(l),dp(t),dp(r),dp(b));return p;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b); getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);build(); initLocalEngine();
    }

    void build(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(20),dp(14),dp(20),dp(10));
        ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);bar.addView(logo,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout brand=new LinearLayout(this);brand.setOrientation(LinearLayout.VERTICAL);brand.setPadding(dp(12),0,0,0);
        TextView n=text("MDE by SMN",18,TEXT);n.setTypeface(null,1);brand.addView(n);brand.addView(text("AI Face Studio • On-device",12,MUTED));
        bar.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView chip=text("OFFLINE AI",10,TEXT);chip.setTypeface(null,1);chip.setPadding(dp(10),dp(6),dp(10),dp(6));chip.setBackground(bg(PANEL2,20));bar.addView(chip);
        root.addView(bar);

        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(8),dp(20),dp(28));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        TextView hero=text("Create. Swap. Enhance.",27,TEXT);hero.setTypeface(null,1);content.addView(hero,margin(0,8,0,4));
        content.addView(text("Your photos stay on this phone during processing.",14,MUTED),margin(0,0,0,18));

        LinearLayout previews=new LinearLayout(this);previews.setWeightSum(2);
        sourcePreview=preview("SOURCE FACE");targetPreview=preview("TARGET IMAGE");
        previews.addView(sourcePreview,new LinearLayout.LayoutParams(0,dp(178),1));
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(178),1);tp.setMargins(dp(10),0,0,0);previews.addView(targetPreview,tp);
        content.addView(previews);

        LinearLayout picks=new LinearLayout(this);picks.setWeightSum(2);
        Button bs=button("Choose source",false);bs.setOnClickListener(v->pick(SRC));
        Button bt=button("Choose target",false);bt.setOnClickListener(v->pick(TGT));
        picks.addView(bs,new LinearLayout.LayoutParams(0,dp(50),1));LinearLayout.LayoutParams bm=new LinearLayout.LayoutParams(0,dp(50),1);bm.setMargins(dp(10),0,0,0);picks.addView(bt,bm);
        content.addView(picks,margin(0,10,0,18));

        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(14),dp(16),dp(16));card.setBackground(bg(PANEL,20));
        card.addView(label("IDENTITY STRENGTH"),margin(0,0,0,8));
        LinearLayout sr=new LinearLayout(this);sr.setGravity(Gravity.CENTER_VERTICAL);sr.addView(text("Preserve source identity",14,TEXT),new LinearLayout.LayoutParams(0,dp(36),1));
        strengthValue=text("80%",13,MUTED);sr.addView(strengthValue);card.addView(sr);
        strength=new SeekBar(this);strength.setMax(100);strength.setProgress(80);strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean f){strengthValue.setText(p+"%");}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});card.addView(strength);
        card.addView(text("Higher values favor the source identity.",12,MUTED),margin(0,4,0,0));content.addView(card,margin(0,0,0,12));

        Button run=button("Generate image",true);run.setTextSize(16);run.setMinHeight(dp(56));run.setOnClickListener(v->generate());content.addView(run);
        status=text("Preparing on-device AI…",12,MUTED);status.setGravity(Gravity.CENTER);status.setPadding(dp(12),dp(14),dp(12),dp(4));content.addView(status);

        TextView license=text("Use only images you have permission to edit. The face-swap models are bundled for offline processing and have separate licensing terms.",11,MUTED);license.setPadding(dp(4),dp(12),dp(4),0);content.addView(license);
        setContentView(root);
    }

    ImageView preview(String s){ImageView i=new ImageView(this);i.setBackground(bg(PANEL,18));i.setScaleType(ImageView.ScaleType.CENTER_CROP);i.setContentDescription(s);i.setImageDrawable(new LabelDrawable(s,MUTED));return i;}
    class LabelDrawable extends Drawable{String s;int c;Paint p=new Paint(1);LabelDrawable(String s,int c){this.s=s;this.c=c;}public void draw(Canvas c){p.setColor(this.c);p.setTextSize(dp(11));p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,getBounds().centerX(),getBounds().centerY(),p);}public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(android.graphics.ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}}

    void pick(int code){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,code);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;Uri u=d.getData();if(u==null)return;try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}if(r==SRC){sourceUri=u;sourcePreview.setImageURI(u);}else{targetUri=u;targetPreview.setImageURI(u);}}

    void initLocalEngine(){
        pool.submit(()->{
            try{
                runOnUiThread(()->status.setText("Loading bundled AI models…"));
                ModelDownloader d=new ModelDownloader(this);
                d.setCallback(new ModelDownloader.DownloadCallback(){public void onProgress(String n,int p){runOnUiThread(()->status.setText("Setting up "+n+" • "+p+"%"));}public void onComplete(String n){}public void onError(String n,String e){}});
                FaceDetector det=new FaceDetector(this);det.initialize();
                FaceEmbedder emb=new FaceEmbedder(this);emb.initialize();
                FaceSwapper swp=new FaceSwapper(this);swp.initialize();
                processor=new FaceFusionProcessor(det,emb,swp);
                runOnUiThread(()->status.setText("Ready — AI runs on this phone."));
            }catch(Exception e){runOnUiThread(()->status.setText("AI setup failed: "+e.getMessage()));}
        });
    }

    Bitmap load(Uri u)throws Exception{InputStream in=getContentResolver().openInputStream(u);Bitmap b=BitmapFactory.decodeStream(in);in.close();if(b==null)throw new Exception("Unable to read image");return resize(b,1600);}
    Bitmap resize(Bitmap b,int max){float r=Math.min(1f,Math.min((float)max/b.getWidth(),(float)max/b.getHeight()));if(r>=1f)return b;return Bitmap.createScaledBitmap(b,Math.round(b.getWidth()*r),Math.round(b.getHeight()*r),true);}

    void generate(){
        if(processor==null){status.setText("AI is still preparing. Please wait.");return;}
        if(sourceUri==null||targetUri==null){status.setText("Choose both source and target images first.");return;}
        status.setText("Swapping face on-device…");
        pool.submit(()->{
            try{
                Bitmap s=load(sourceUri),t=load(targetUri);
                Bitmap out=processor.processFaceFusion(s,t,0);
                resultBitmap=out;
                runOnUiThread(()->showResult(out));
            }catch(Exception e){runOnUiThread(()->status.setText("Generation failed: "+e.getMessage()));}
        });
    }

    void showResult(Bitmap out){
        if(resultPreview!=null)content.removeView(resultPreview);
        resultPreview=new ImageView(this);resultPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);resultPreview.setBackground(bg(PANEL,18));resultPreview.setImageBitmap(out);
        content.addView(resultPreview,0,new LinearLayout.LayoutParams(-1,dp(320)));
        Button save=button("Save result to Gallery",true);save.setOnClickListener(v->saveResult());
        content.addView(save,1,margin(0,10,0,0));
        status.setText("Done — generated locally. No backend used.");
    }

    void saveResult(){
        if(resultBitmap==null)return;
        try{
            String name="MDE_"+System.currentTimeMillis()+".jpg";
            android.content.ContentValues v=new android.content.ContentValues();v.put(MediaStore.Images.Media.DISPLAY_NAME,name);v.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");v.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/MDE by SMN");
            Uri u=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);if(u==null)throw new Exception("Gallery insert failed");
            OutputStream o=getContentResolver().openOutputStream(u);resultBitmap.compress(Bitmap.CompressFormat.JPEG,95,o);o.close();status.setText("Saved to Gallery.");
        }catch(Exception e){status.setText("Save failed: "+e.getMessage());}
    }

    @Override protected void onDestroy(){pool.shutdownNow();super.onDestroy();}
}