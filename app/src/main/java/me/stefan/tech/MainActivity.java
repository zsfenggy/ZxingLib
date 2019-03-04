package me.stefan.tech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.view.View;
import android.widget.Button;

import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.encode.EncodeActivity;
import com.google.zxing.client.android.util.Contents;
import com.google.zxing.client.android.util.Intents;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SCAN = 0b01;

    private AppCompatEditText etSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        etSummary = findViewById(android.R.id.summary);
        String text = etSummary.getEditableText().toString();
        etSummary.setSelection(text.length());
        Button button1 = findViewById(android.R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, CaptureActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SCAN);
            }
        });
        Button button2 = findViewById(android.R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, EncodeActivity.class);
                intent.setAction(Intents.Encode.ACTION);
                intent.putExtra(Intents.Encode.FORMAT, "QR_CODE");
                intent.putExtra(Intents.Encode.TYPE, Contents.Type.TEXT);
                String text = etSummary.getEditableText().toString();
                intent.putExtra(Intents.Encode.DATA, text);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK
                && null != data) {
            String code = data.getStringExtra(CaptureActivity.EXTRA_SCAN_RESULT);
            etSummary.setText(code);
        }
    }

}
