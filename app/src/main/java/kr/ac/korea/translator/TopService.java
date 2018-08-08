package kr.ac.korea.translator;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

public class TopService extends Service implements View.OnTouchListener{
    private WindowManager windowManager;
    View mView;
    boolean moving;
    private float offsetX;
    private float offsetY;
    private int originalXPos;
    private int originalYPos;
    private View topLeftView;

    @Override public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }
    @Override public void onCreate() {
        super.onCreate();
        LayoutInflater inflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        |WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        params.type = checkVersion();
        params.gravity = Gravity.TOP | Gravity.LEFT;
        mView = inflate.inflate(R.layout.layout_top_service, null);
        mView.setOnTouchListener(this);
        params.x = 0;
        params.y = 0; //floating view
        Button capture = mView.findViewById(R.id.takeShot);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),CoverActivity.class));
            }
        }); //capture

        Button close = mView.findViewById(R.id.close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSelf();
            }
        }); //closeButton

        windowManager.addView(mView, params);
        topLeftView = new View(this);
        WindowManager.LayoutParams topLeftParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        topLeftParams.type = checkVersion();
        topLeftParams.gravity = Gravity.LEFT | Gravity.TOP;
        topLeftParams.x = 0;
        topLeftParams.y = 0;
        topLeftParams.width = 0;
        topLeftParams.height = 0;
        windowManager.addView(topLeftView, topLeftParams); //전체 뷰
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mView != null) windowManager.removeView(mView);
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getRawX();
            float y = event.getRawY();
            moving = false;
            int[] location = new int[2];
            mView.getLocationOnScreen(location);
            originalXPos = location[0];
            originalYPos = location[1];
            offsetX = originalXPos - x;
            offsetY = originalYPos - y;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int[] topLeftLocationOnScreen = new int[2];
            topLeftView.getLocationOnScreen(topLeftLocationOnScreen);
            float x = event.getRawX();
            float y = event.getRawY();
            WindowManager.LayoutParams params = (WindowManager.LayoutParams) mView.getLayoutParams();
            int newX = (int) (offsetX + x);
            int newY = (int) (offsetY + y);
            if (Math.abs(newX - originalXPos) < 1 && Math.abs(newY - originalYPos) < 1 && !moving) {
                return false;
            }
            params.x = newX - (topLeftLocationOnScreen[0]);
            params.y = newY - (topLeftLocationOnScreen[1]);
            windowManager.updateViewLayout(mView, params);
            moving = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (moving) {
                return true;
            }
        } //floating View 이
        return false;
    }
    private int checkVersion(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return WindowManager.LayoutParams.TYPE_PHONE;
        else
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    }

}
