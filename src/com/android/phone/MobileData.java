/* Copyright (C) 2017 Google Inc. All Rights Reserved. */

package com.android.phone;
import android.app.Activity;

import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.util.List;
import android.content.SharedPreferences;

public class MobileData extends Activity {
    private static final String TAG = MobileData.class.getSimpleName();
    private boolean mChecked = true;
    private static final String INTENT_DATA_OFF = "com.android.internal.telephony.DATA_OFF";
    private static final String INTENT_DATA_ON = "com.android.internal.telephony.DATA_ON";
    private static final int NEXT_REQUEST_CODE = 1;
    private static final int RESULT_SKIP = 1;
    private static final String PREFERENCES = "MobileDataChecked";
    private static final String PREFERENCES_KEY = "isChecked";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  
        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES,MODE_PRIVATE);
        mChecked = sharedPreferences.getBoolean(PREFERENCES_KEY,true);
        android.util.Log.e("MobileData","mChecked:"+mChecked);
        setMobileData();
    }

    private void setMobileData(){
        Intent intent = null;
        if(mChecked){
           intent = new Intent(INTENT_DATA_ON);
        } else {
           intent = new Intent(INTENT_DATA_OFF);
        }
        this.sendBroadcast(intent);
        finish(RESULT_OK);
        done(true);
    }

    private void finish(int resultCode) {
        setResult(resultCode);
//        finish();
    }

    public void done(boolean success) {
        int resultCode = success ? Activity.RESULT_OK : RESULT_SKIP;
        Intent intent =  new Intent("com.android.wizard.NEXT");
        intent.putExtra("actionId", getIntent().getStringExtra("actionId"));
        intent.putExtra("wizardBundle", getIntent().getBundleExtra("wizardBundle"));
        intent.putExtra("com.android.setupwizard.ResultCode", resultCode);
        startActivityForResult(intent, NEXT_REQUEST_CODE);
    }
}
