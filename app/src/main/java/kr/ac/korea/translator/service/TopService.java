package kr.ac.korea.translator.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;

import kr.ac.korea.translator.R;
import kr.ac.korea.translator.view.main.CoverActivity;

public class TopService extends Service implements View.OnTouchListener{
    View vOverlay;
    float xpos = 0;
    float ypos = 0;
    private WindowManager wm;

    @Override public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }
    @Override public void onCreate() {
        super.onCreate();

        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, LayoutParams.TYPE_APPLICATION_OVERLAY, LayoutParams.FLAG_NOT_FOCUSABLE|LayoutParams.FLAG_NOT_TOUCH_MODAL|LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT);

        lp.gravity = Gravity.START | Gravity.TOP;
        lp.type = checkVersion();

        vOverlay = li.inflate(R.layout.layout_top_service, null);
        vOverlay.setOnTouchListener(this);

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        wm.addView(vOverlay, lp);

        Button btnTranslate = vOverlay.findViewById(R.id.btn_translate);
        ImageButton btnClose = vOverlay.findViewById(R.id.btn_close);

        btnTranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),CoverActivity.class));
            }
        });
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSelf();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (vOverlay != null) {
            wm.removeView(vOverlay);
            vOverlay = null;
        }
    }
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        WindowManager wmSub = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display d = wmSub.getDefaultDisplay();
        Point p = new Point();
        d.getSize(p);

        Log.d("[SIZE]", String.valueOf(p));

        int action = motionEvent.getAction();
        int pointer = motionEvent.getPointerCount();

        switch (action) {
            case MotionEvent.ACTION_UP:
                if(pointer == 1) {
                    vOverlay.performClick();
                }
                break;
            case MotionEvent.ACTION_DOWN:
                if(pointer == 1) {
                    xpos = motionEvent.getRawX();
                    ypos = motionEvent.getRawY();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(pointer == 1) {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view.getLayoutParams();

                    float dx = xpos - motionEvent.getRawX();
                    float dy = ypos - motionEvent.getRawY();

                    xpos = motionEvent.getRawX();
                    ypos = motionEvent.getRawY();

                    lp.x = (int) (lp.x - dx);
                    lp.y = (int) (lp.y - dy);

                    wm.updateViewLayout(view, lp);
                    return true;
                }
                break;
        }
        return false;
    }
    private int checkVersion(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return WindowManager.LayoutParams.TYPE_PHONE;
        else
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    }

}
