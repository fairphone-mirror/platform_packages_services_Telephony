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
import android.widget.Button;
import android.util.Log;
import java.util.List;
import android.os.SystemProperties;
import android.view.Window;

public class InproductionModeFlagActivity extends Activity implements BottomTipsDialog.OnSkipOrCloseClickListener{
    private static final String TAG = InproductionModeFlagActivity.class.getSimpleName();
    private Button btnNext;
    private Button btnClear;
    private boolean mCleanOk = false;

    private static final String INPRODUCTION_MODE_FLAG = "ro.boot.inproductionflag";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inproduction_mode_flag_activity);
        initView();    

    }

    @Override
    protected void onResume(){
        super.onResume();
        boolean inproductionMode = SystemProperties.getBoolean(INPRODUCTION_MODE_FLAG, false);
        if (!inproductionMode) {
            returnToGoogleSetupWizard(337);
            finish();
        }
    }

    private void initView() {
        btnNext = findViewById(R.id.btn_next);
        btnClear = findViewById(R.id.btn_clear);
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnToGoogleSetupWizard(336);
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearDialog();
            }
        });
    }

     @Override
    public void onCloseClick() {

    }

    @Override
    public void onSkipClick(boolean cleanOk) {
        mCleanOk = cleanOk;
    }

   private void returnToGoogleSetupWizard(int code) {
        Intent i = new Intent("com.android.wizard.NEXT");
        //i.putExtra("scriptUri", getIntent().getStringExtra("scriptUri"));
        i.putExtra("actionId", getIntent().getStringExtra("actionId"));
        i.putExtra("wizardBundle", getIntent().getBundleExtra("wizardBundle"));
       // i.putExtra("theme", getIntent().getStringExtra("theme"));
        i.putExtra("com.android.setupwizard.ResultCode", code);
        startActivityForResult(i, 1000);
    }

    private void showClearDialog(){
        BottomTipsDialog mBottomTipsDialog = new BottomTipsDialog(this);
        Window win = mBottomTipsDialog.getWindow();
        win.getDecorView().setPadding(0,0,0,0);
        WindowManager.LayoutParams attributes = win.getAttributes();
        attributes.dimAmount = 0.7f;
        attributes.width = WindowManager.LayoutParams.MATCH_PARENT;
        attributes.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mBottomTipsDialog.getWindow().setAttributes(attributes);
        mBottomTipsDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mBottomTipsDialog.setOnSkipOrCloseClickListener(this,mCleanOk);
        mBottomTipsDialog.show();
    }


}
