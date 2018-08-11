package kr.ac.korea.translator.view.common;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

public class BaseActivity extends Activity implements View.OnClickListener {
    protected String token;
    @Override
    public void onClick(View v) {
    }

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void init(){

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
