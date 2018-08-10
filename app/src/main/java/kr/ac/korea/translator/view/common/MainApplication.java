package kr.ac.korea.translator.view.common;
import android.app.Application;

import lombok.Data;

@Data
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }
    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
