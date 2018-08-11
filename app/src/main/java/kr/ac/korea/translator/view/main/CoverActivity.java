package kr.ac.korea.translator.view.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kr.ac.korea.translator.R;
import kr.ac.korea.translator.model.Detection;
import kr.ac.korea.translator.model.TextContainer;
import kr.ac.korea.translator.network.OCRApi;
import kr.ac.korea.translator.network.TranslateApi;
import kr.ac.korea.translator.view.common.BaseActivity;

public class CoverActivity extends BaseActivity {
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private int REQUEST_CODE = 0;
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mProjectionManager;
    private Display mDisplay;
    private int mDensity;
    ImageReader mImageReader;
    private OrientationChangeCallback mOrientationChangeCallback;
    private VirtualDisplay mVirtualDisplay;
    private Handler mHandler;
    public static boolean state;
    private int mRotation;
    public static int mWidth;
    public static int mHeight;
    public Context mContext;
    public static List<TextContainer> mResult;
    public Detection r;
    public TextContainer t;
    public int count;
    RelativeLayout container;
    public static String selectedLang;
    private static final Map<String, String> m = new LinkedHashMap<>();

    public void onDestroy () {
        super.onDestroy();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover);
        mContext = this;
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        state=false;
        mHandler = new Handler();
        container = (RelativeLayout) findViewById(R.id.container);
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAndRemoveTask();
            }
        });
        m.put("한국어","ko");
        m.put("영어","en");
        m.put("일본어","ja");
        m.put("중국어","zh-Hans");
        m.put("독일어","de");
        m.put("프랑스어","fr");
        m.put("스페인어","es");
        m.put("헝가리어","hu");
        m.put("이탈리아어","it");
        SharedPreferences sp = getSharedPreferences("LANG",MODE_PRIVATE);
        selectedLang = m.get(sp.getString("lang","한국어"));
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            if (mMediaProjection != null) {
                // display metrics
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDensity = metrics.densityDpi;
                mDisplay = getWindowManager().getDefaultDisplay();
                // create virtual display depending on device width / height
                createVirtualDisplay();
                // register orientation change callback
                mOrientationChangeCallback = new OrientationChangeCallback(this);
                if (mOrientationChangeCallback.canDetectOrientation()) {
                    mOrientationChangeCallback.enable();
                }
                // register media projection stop callback
                mMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
            }
        }
    }

    public void createVirtualDisplay() {
        // get width and height
        Point size = new Point();
        mDisplay.getSize(size);
        mWidth = size.x;
        mHeight = size.y;
        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888 , 2);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
    }

    public class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                    if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
                    mMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }

    public class OrientationChangeCallback extends OrientationEventListener {
        OrientationChangeCallback(Context context) {
            super(context);
        }
        @Override
        public void onOrientationChanged(int orientation) {
            final int rotation = mDisplay.getRotation();
            if (rotation != mRotation) {
                mRotation = rotation;
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                    // re-create virtual display depending on device width / height
                    createVirtualDisplay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class ImageAvailableListener implements ImageReader.OnImageAvailableListener{
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            FileOutputStream fos = null;
            Bitmap bitmap = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null&&!CoverActivity.state) {
                    CoverActivity.state = true;
                    android.media.Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mWidth;
                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    TranslateCallback translateCallback = new TranslateCallback() {
                        @Override
                        public void resultToScreen(List<TextContainer> result) {
                            mResult = result;
                            Gson gson = new Gson();
                            count=-1;
                            for (TextContainer tv : result) {
                                t=tv;
                                try {
                                    List<Detection> translateRst = gson.fromJson(TranslateApi.Translate(t.getRst()), new TypeToken<List<Detection>>(){}.getType());
                                    r=translateRst.get(0);
                                    if (r.getTranslations().get(0)!= null) {
                                        r=translateRst.get(0);
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                TextView textView = new TextView(CoverActivity.this);
                                                textView.setText(r.getTranslations().get(0).getText());
                                                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                                                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                                params.setMargins(mResult.get(count).getX(), mResult.get(count).getY(), 0, 0);
                                                textView.setLayoutParams(params);
                                                textView.setTextColor(Color.WHITE);
                                                textView.setBackgroundColor(getResources().getColor(R.color.transparentBlack));
                                                textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
                                                container.addView(textView);
                                            }
                                        });
                                    }
                                } catch (Exception e) {
                                    Log.e("error", e.toString());
                                }
                                count++;
                            }
                        }
                    };
                    OCRApi.callOcr(bitmap, mContext, translateCallback);
                    mMediaProjection.stop();
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }

                if (bitmap != null)
                    bitmap.recycle();
                if (image != null)
                    image.close();
            }
        }
    }
    public interface TranslateCallback{
        void resultToScreen(List<TextContainer> result);
    }
    public static String getSelecteedLang(){
        return selectedLang;
    }
}
