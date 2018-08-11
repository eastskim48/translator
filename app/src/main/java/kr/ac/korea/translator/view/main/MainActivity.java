package kr.ac.korea.translator.view.main;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.SharedPreferences.Editor;

import java.util.LinkedHashMap;
import java.util.Map;

import kr.ac.korea.translator.R;
import kr.ac.korea.translator.service.TopService;
import kr.ac.korea.translator.view.common.BaseActivity;

public class MainActivity extends BaseActivity {
    private static final String FILE_NAME = "LANG";

    private static final Map<String, String> m = new LinkedHashMap<>();
    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View view = getWindow().getDecorView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (view != null) {
                view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                getWindow().setStatusBarColor(Color.parseColor("#ffffff"));
            }
        }

        final String[] data = getResources().getStringArray(R.array.arr_languages);
        final SharedPreferences sp = getSharedPreferences(FILE_NAME, MODE_PRIVATE);
        final Editor editor = sp.edit();
        Spinner spnLanguage = findViewById(R.id.spn_language);
        ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, data);
        spnLanguage.setAdapter(adapter);
        spnLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                editor.putString("lang", data[i]);
                editor.apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        Button button = (Button)findViewById(R.id.btn_start);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermission();
            }
        });
        Button bt_stop = (Button) findViewById(R.id.btn_stop);
        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(MainActivity.this, TopService.class));
            }
        });
    }
    public void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 마시멜로우 이상일 경우
            if (!Settings.canDrawOverlays(this)) {              // 체크
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            } else {
                startService(new Intent(MainActivity.this, TopService.class));
                finish();
            }
        } else {
            startService(new Intent(MainActivity.this, TopService.class));
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this,"권한 동의를 얻지 못해 실행할 수 없습니다",Toast.LENGTH_LONG).show();

            } else {
                startService(new Intent(MainActivity.this, TopService.class));
            }
        }
    }
}
