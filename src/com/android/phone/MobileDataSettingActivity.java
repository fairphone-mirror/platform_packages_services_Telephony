/* Copyright (C) 2017 Google Inc. All Rights Reserved. */

package com.android.phone;
import android.app.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.content.Intent;
import android.graphics.Color;
import android.view.WindowManager;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.text.TextUtils;
import android.telephony.SubscriptionManager;
import android.widget.Button;
import android.widget.CheckBox;
import com.android.internal.telephony.TelephonyIntents;
import android.util.Log;
import android.telephony.SubscriptionInfo;
import java.util.List;
import android.content.SharedPreferences;

public class MobileDataSettingActivity extends Activity {
    private TelephonyManager tm = null;
    private static final String TAG = MobileDataSettingActivity.class.getSimpleName();
    private SubscriptionManager mSubscriptionManager;
    private int mSubId = -1;
    private Button btnNext;
    private CheckBox checkBox;
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
        setContentView(R.layout.mobile_data_setting_activity);
        mSubscriptionManager = getSystemService(SubscriptionManager.class);
        mSubId = 1;
        initView();    

    }

    @Override
    protected void onResume(){
        super.onResume();
        if (tm == null) {
            tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        }
        if (checkBox != null) {
            checkBox.setChecked(mChecked);
        }
        if(!hasSimCard(tm)){
            returnToGoogleSetupWizard();
            finish();
        }
    }

    private void initView() {
        btnNext = findViewById(R.id.btn_next);
        checkBox = findViewById(R.id.ck_mobile);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMobileData();
            }
        });

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(isChecked){ 
              mChecked = true;              
            }else{                
              mChecked = false;
            }
            checkBox.setChecked(isChecked);
            setMobileDataPrefrece(isChecked);
            }
        });
    }

    private void setMobileDataPrefrece(boolean isChecked){
        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES,MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREFERENCES_KEY,isChecked);
        boolean isCommitSuccess = editor.commit();
        android.util.Log.e("MobileData","isChecked:"+isChecked+"   isCommitSuccess:"+isCommitSuccess);
    }

    private void returnToGoogleSetupWizard() {
        Intent i = new Intent("com.android.wizard.NEXT");
        //i.putExtra("scriptUri", getIntent().getStringExtra("scriptUri"));
        i.putExtra("actionId", getIntent().getStringExtra("actionId"));
        i.putExtra("wizardBundle", getIntent().getBundleExtra("wizardBundle"));
       // i.putExtra("theme", getIntent().getStringExtra("theme"));
        i.putExtra("com.android.setupwizard.ResultCode", Activity.RESULT_OK);
        startActivityForResult(i, 1000);
    }

    private void setMobileData(){
        //mSubscriptionManager.setSubscriptionEnabled(mSubId, mChecked); 
        // tm.setDataEnabled(mChecked);
        // Intent in = new Intent();
        // //"android.intent.action.SIM_STATE_CHANGED"
        // in.setAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        // this.sendBroadcast(in);
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

  private boolean hasSimCard(TelephonyManager tm){
    int simState = tm.getSimState();
    boolean result = true;
    switch(simState){
        case TelephonyManager.SIM_STATE_ABSENT:
            result = false;
            break;
        case TelephonyManager.SIM_STATE_UNKNOWN:
            result = false;
            break;
    }
    return result;

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
