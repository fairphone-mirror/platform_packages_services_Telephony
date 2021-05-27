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

public class MobileDataSettingActivity extends Activity {
    private TelephonyManager tm = null;
    private static final String TAG = MobileDataSettingActivity.class.getSimpleName();
    private SubscriptionManager mSubscriptionManager;
    private int mSubId = -1;
    private Button btnNext;
    private CheckBox checkBox;
    private boolean mChecked = true;


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
                returnToGoogleSetupWizard();
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
            }
        });
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
        mSubscriptionManager.setSubscriptionEnabled(mSubId, mChecked); 
        tm.setDataEnabled(mChecked);
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

}
