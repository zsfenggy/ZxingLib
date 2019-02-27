package me.stefan.zxinglib;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.zxing.client.android.ScanActivity;

public class MainActivity extends Activity {

    private static final int REQUEST_CODE_SCAN = 0b01;

    private TextView textSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textSummary = findViewById(android.R.id.summary);
        Button button = findViewById(android.R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ScanActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SCAN);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK
                && null != data) {
            String code = data.getStringExtra(ScanActivity.EXTRA_SCAN_RESULT);
            textSummary.setText(code);
        }
    }

}
