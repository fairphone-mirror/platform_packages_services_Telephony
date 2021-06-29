package com.android.phone;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.telephony.euicc.EuiccManager;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class ESimActivity extends Activity {

    private static final String TAG = ESimActivity.class.getSimpleName();
    private boolean isJumpESimSetting = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        @SuppressLint("WrongConstant") EuiccManager mgr = (EuiccManager) getSystemService(Context.EUICC_SERVICE);
        boolean isEnabled = mgr.isEnabled();
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if(hasSimCard(tm)){
            returnToGoogleSetupWizard();
            finish();
        } else {
            if (isEnabled) {
                  // Intent intent = new Intent(EuiccManager.ACTION_MANAGE_EMBEDDED_SUBSCRIPTIONS);
                  // startActivity(intent);

                 Intent intent = new Intent(EuiccManager.ACTION_PROVISION_EMBEDDED_SUBSCRIPTION);
                 intent.putExtra(EuiccManager.EXTRA_FORCE_PROVISION, true);
              startActivity(intent);
                 isJumpESimSetting = true;
            } else {
             returnToGoogleSetupWizard();
            }
        }
    }

    private void returnToGoogleSetupWizard() {
        isJumpESimSetting = false;
        Intent i = new Intent("com.android.wizard.NEXT");
        //i.putExtra("scriptUri", getIntent().getStringExtra("scriptUri"));
        i.putExtra("actionId", getIntent().getStringExtra("actionId"));
        i.putExtra("wizardBundle", getIntent().getBundleExtra("wizardBundle"));
       // i.putExtra("theme", getIntent().getStringExtra("theme"));
        i.putExtra("com.android.setupwizard.ResultCode", Activity.RESULT_OK);
        startActivityForResult(i, 1000);
    }

    @Override
    protected void onResume(){
        super.onResume();
        if (isJumpESimSetting) {
            finish();
        }
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
