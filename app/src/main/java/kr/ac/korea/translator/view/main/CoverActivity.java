package kr.ac.korea.translator.view.main;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
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
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import kr.ac.korea.translator.R;
import kr.ac.korea.translator.model.Detection;
import kr.ac.korea.translator.model.TextContainer;
import kr.ac.korea.translator.network.OCRApi;
import kr.ac.korea.translator.network.TranslateApi;
import kr.ac.korea.translator.view.common.BaseActivity;

public class CoverActivity extends BaseActivity {
    private static final String SCREENCAP_NAME = "screencap";
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mProjectionManager;
    private Display mDisplay;
    private int mDensity, mRotation, REQUEST_CODE = 0;
    ImageReader mImageReader;
    private OrientationChangeCallback mOrientationChangeCallback;
    private VirtualDisplay mVirtualDisplay;
    private Handler mHandler;
    public static boolean state, up;
    public static int mWidth, mHeight, count, statusBarHeight;
    public Context mContext;
    public static List<TextContainer> ocrResult;
    public String translated;
    RelativeLayout container;
    public ProgressDialog mProgressDialog;
    public Gson gson;
    public TranslateApi translateApi;
    public OCRApi ocrApi;
    TranslateThread translateThread;

    public void onDestroy () {
        super.onDestroy();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cover);
        mContext = this;
        gson = new Gson();
        state = false;
        mHandler = new Handler();
        // Set cover UI
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        container = (RelativeLayout) findViewById(R.id.cover_container);
        container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishAndRemoveTask();
            }
        });
        showProgressDialog();

        // Set media projection
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        setStatusBarHeight();
        if (requestCode == REQUEST_CODE) {
            //setStatusBarHeight();
            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
            if (mMediaProjection != null) {
                // display metrics
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                mDensity = metrics.densityDpi;
                mDisplay = getWindowManager().getDefaultDisplay();
                // create virtual display depending on device width / height
                createVirtualDisplay();
                // register orientation change callback
                mOrientationChangeCallback = new OrientationChangeCallback(CoverActivity.this);
                if (mOrientationChangeCallback.canDetectOrientation()) {
                    mOrientationChangeCallback.enable();
                }
                // register media projection stop callback
                mMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
            }
        }
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
            ocrApi = new OCRApi(mContext);
            try {
                image = reader.acquireLatestImage();
                if (image != null && !CoverActivity.state) {
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
                            if (result == null || result.size() == 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideProgressDialog();
                                        Toast.makeText(getApplicationContext(), "번역할 글자를 찾지 못했습니다", Toast.LENGTH_LONG).show();
                                    }
                                });
                                return;
                            }

                            //result : OCR textBox List
                            ocrResult = result;
                            count = 0;
                            up = true;
                            translateThread = new TranslateThread();
                            translateThread.start();

                            // 이제는 모든 결과가 그냥 한번에 다 표시됨
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    while (count < ocrResult.size()) {
                                        if(up) continue;
                                        if(translated != null && ocrResult.get(count).getY() > statusBarHeight) {
                                            container.addView(createTextView(ocrResult.get(count)));
                                        }
                                        count++;
                                        up = true;
                                    }
                                    hideProgressDialog();
                                }
                            });
                        }

                    };
                    ocrApi.Post(bitmap, translateCallback);
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
        public TextView createTextView(TextContainer thisRst){
            TextView textView = new TextView(CoverActivity.this);
            textView.setText(translated);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(thisRst.getX(), thisRst.getY()-statusBarHeight, 0, 0); //statusBarHeight 빼는 것 없앰
            textView.setLayoutParams(params);
            textView.setTextColor(Color.WHITE);
            //Set Textsize - Pixel based on height
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, thisRst.getH());
            textView.setBackgroundColor(getResources().getColor(R.color.transparentBlack));
            textView.setGravity(View.TEXT_ALIGNMENT_CENTER);
            return textView;
        }
    }

    class TranslateThread extends Thread{
        @Override
        public void run() {
            translateApi = new TranslateApi(mContext);
            while (count < ocrResult.size()) {
                if (up) {
                    try {
                        String originalTxt = ocrResult.get(count).getRst();
                        List<Detection> translateResponseList = gson.fromJson(translateApi.Translate(originalTxt), new TypeToken<List<Detection>>() {
                        }.getType());
                        // 표시해도 되는 결과인지 check
                        if(checkIfCorrectlyTranslated(translateResponseList, originalTxt)) {
                            up = false;
                        }
                        else{
                            count++;
                        }

                    } catch (Exception e) {
                        Log.e("error", e.toString());
                    }
                }
            }
        }
    }

    public interface TranslateCallback{
        void resultToScreen(List<TextContainer> result);
    }

    public void createVirtualDisplay() {
        // get width and height
        Point size = new Point();
        mDisplay.getRealSize(size);
        mWidth = size.x;
        mHeight = size.y;
        // start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888 , 2);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
    }

    public void setStatusBarHeight(){
        Rect rectangle = new Rect();
        Window window = getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
        statusBarHeight =  rectangle.top;
    }

    public void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(CoverActivity.this);
            mProgressDialog.setMessage("번역중...");
            mProgressDialog.setIndeterminate(true);
        }
        mProgressDialog.show();
    }

    public void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public boolean checkIfCorrectlyTranslated(List<Detection> translateResponseList, String originalTxt){
        if(translateResponseList==null){
            return false;
        }
        Detection translateResponse = translateResponseList.get(0);
        List<Detection.Translation> translations = translateResponse.getTranslations();
        if(translations == null){
            return false;
        }
        translated = translations.get(0).getText();
        // 타겟 언어나 번역되지 않은 언어 거르기
        return (!translateResponse.getDetectedLanguage().getLanguage().equals("ko")) && (!originalTxt.equals(translated));
    }
}
