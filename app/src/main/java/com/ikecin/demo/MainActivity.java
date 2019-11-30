package com.ikecin.demo;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GWDeviceManager.getInstance().init(this);

        findViewById(R.id.start).setOnClickListener(v -> {
            GWDeviceManager.getInstance().startSmartConfig("iKECINSmart", "iKECINSmart", Integer.MAX_VALUE);
        });


        findViewById(R.id.stop).setOnClickListener(v -> {
            GWDeviceManager.getInstance().stopSmartConfig();
        });

        Disposable disposable = GWDeviceManager.getInstance()
            .observeConfig()
            .subscribe(s -> {
                if (s.isEmpty()) {
                    Toast.makeText(this, "配网失败", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
                }
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        GWDeviceManager.getInstance().stopSmartConfig();

        GWDeviceManager.getInstance().deinit();
    }
}
