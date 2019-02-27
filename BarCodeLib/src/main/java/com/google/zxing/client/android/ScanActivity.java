package com.google.zxing.client.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;

import com.google.zxing.Result;

public class ScanActivity extends CaptureActivity {

    public static final String EXTRA_SCAN_RESULT = "scan_result";

    @Override
    protected void onResume() {
        super.onResume();
        restartPreviewAfterDelay(500L);
    }

    @Override
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        super.handleDecode(rawResult, barcode, scaleFactor);
        if (null != rawResult) {
            Intent data = new Intent();
            data.putExtra(EXTRA_SCAN_RESULT, rawResult.getText());
            setResult(Activity.RESULT_OK, data);
            finish();
        } else {
            restartPreviewAfterDelay(500L);
        }
    }

}
