package kr.ac.korea.translator.view.main;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.LinkedHashMap;
import java.util.Map;

import androidx.constraintlayout.widget.ConstraintLayout;
import kr.ac.korea.translator.R;
import kr.ac.korea.translator.service.TopService;
import kr.ac.korea.translator.view.common.BaseActivity;

public class MainActivity extends BaseActivity {
    private static final String FILE_NAME = "LANG";

    private static final Map<String, Integer> m = new LinkedHashMap<>();
    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 1;
    private ConstraintLayout guide_1, guide_2, guide_3, guide_4;
    private Button guideButton, startButton, stopButton;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        View view = getWindow().getDecorView();

        // Spinner
        String[] langList = new String[]{"한국어", "영어", "일본어", "중국어", "독일어", "프랑스어", "스페인어", "헝가리어", "이탈리아어"};
        for(int i=0; i<langList.length; i++){
            m.put(langList[i], i);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (view != null) {
                view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                getWindow().setStatusBarColor(Color.parseColor("#ffffff"));
            }
        }
        final String[] data = getResources().getStringArray(R.array.arr_languages);
        final SharedPreferences sp = getSharedPreferences(FILE_NAME, MODE_PRIVATE);
        Spinner spnLanguage = findViewById(R.id.spn_language);
        spnLanguage.setFocusable(true);
        final Editor editor = sp.edit();
        ArrayAdapter<String> adapter;
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, data);
        spnLanguage.setAdapter(adapter);
        spnLanguage.setSelection(m.get(sp.getString("lang","한국어")));
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

        // adView
        MobileAds.initialize(this, getString(R.string.admob_app_id));
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Buttons
        startButton = (Button)findViewById(R.id.btn_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPermission();
            }
        });
        stopButton = (Button) findViewById(R.id.btn_stop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
        setGuideButtons();
    }

    public void setGuideButtons(){
        Button bt_help = (Button) findViewById(R.id.btn_help);
        bt_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdView.setVisibility(View.INVISIBLE);
                guide_1 = (ConstraintLayout)findViewById(R.id.guide_1);
                guide_2 = (ConstraintLayout)findViewById(R.id.guide_2);
                guide_3 = (ConstraintLayout)findViewById(R.id.guide_3);
                guide_4 = (ConstraintLayout)findViewById(R.id.guide_4);
                guide_1.setVisibility(View.VISIBLE);
                guideButton = (Button) findViewById(R.id.next_guide_1);
                guideButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        guide_1.setVisibility(View.INVISIBLE);
                        guide_2.setVisibility(View.VISIBLE);
                    }
                });
                guideButton = (Button) findViewById(R.id.next_guide_2);
                guideButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        guide_2.setVisibility(View.INVISIBLE);
                        guide_3.setVisibility(View.VISIBLE);
                    }
                });
                guideButton = (Button) findViewById(R.id.next_guide_3);
                guideButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        guide_3.setVisibility(View.INVISIBLE);
                        guide_4.setVisibility(View.VISIBLE);
                    }
                });
                guideButton = (Button) findViewById(R.id.next_guide_4);
                guideButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        guide_4.setVisibility(View.INVISIBLE);
                        mAdView.setVisibility(View.VISIBLE);
                    }
                });

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
