//Add by shaopan.tang 2021-04-28 [FP4-89]Customize SIP/XCAP UserAgent string
package com.android.phone;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.SystemProperties;
import android.sysprop.TelephonyProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
import com.qualcomm.qcrilhook.QcRilHookCallback;
import com.qualcomm.sysrilcmd.SysRilCmd;
import com.qualcomm.sysrilcmd.ISysRilCmd;

public class CustomizeNVReceiver extends BroadcastReceiver {
    private static String TAG = "CustomizeNVService";
    private static final boolean DEBUG = true;

    private static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

    private int mPhoneId = 0;
    private SysRilCmd mSysRil;
    private Context mContext;

    public void init(Context context) {
        mSysRil = new SysRilCmd(context, null);
        mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.e(TAG, "onReceive: intent action = " + intent.getAction());

        if (ACTION_BOOT_COMPLETED.equals(action)) {
            writeXcapUserAgent();
            writeIMSUserAgent();
        }else if (action.equals(TelephonyIntents.ACTION_SIM_STATE_CHANGED)){
            String simStatus = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
            if (IccCardConstants.INTENT_VALUE_ICC_LOADED.equals(simStatus)
                    || IccCardConstants.INTENT_VALUE_ICC_READY.equals(simStatus)){
                writeXcapUserAgent();
                writeIMSUserAgent();
            }
        }
    }

    /**
     * Writes the passed string value into cUaUserAgent
     * NV73852 /nv/item_files/ims/qp_ims_ut_config_item if the version supports it; noop otherwise.
     * NOTE: The value is interpreted as ASCII encoded and clipped at 128 chars
     * @param newStringVal new User Agent for request over Ua interface
     */
    private final void writeXcapUserAgent() {
        String newStringVal = null;
        String brand = SystemProperties.get("ro.product.brand", "");
        String model = SystemProperties.get("ro.product.model", "");
        String softVer = SystemProperties.get("ro.build.version.incremental", "");

        String PROPERTY_ICC_OPERATOR_NUMERIC = "gsm.sim.operator.numeric";
        String mccmnc = SystemProperties.get(PROPERTY_ICC_OPERATOR_NUMERIC);
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            mccmnc = TelephonyManager.getTelephonyProperty(mPhoneId, PROPERTY_ICC_OPERATOR_NUMERIC, "0");
        }

        if(TextUtils.isEmpty(mccmnc)) {
            Log.i(TAG, "writeXcapUserAgent() ,mccmnc is null, not handle.");
            return;
        } else if(!TextUtils.isEmpty(brand) && !TextUtils.isEmpty(model) && !TextUtils.isEmpty(softVer)) {
            newStringVal = brand + "_" + model + "_" + softVer + " " + "3gpp-gba";
        }

        if(TextUtils.isEmpty(newStringVal)) {
            Log.i(TAG, "writeXcapUserAgent() ,newStringVal as null, not handle, else overwrite modem value");
            return;
        }else{
            Log.i(TAG, "writeXcapUserAgent() , newStringVal = " + newStringVal);
        }

        /*   NV73852:
         *
         *   Version:                               1
         *                                       bits
         *   ----------------------------------------
         *   version                                8
         *   cXCAPServerName                        2048
         *   cXCAPApplicationUID                    2048
         *   eMediaElementUsage                     8
         *   eEmptySIBUsage                         8
         *   cUaUserAgent                           2048
         */
        String XcapUserAgent = null;
        try {
            XcapUserAgent = mSysRil.getDBStringVal(ISysRilCmd.RIL_SUB_CMD_STRING_XCAP_USER_AGENT);
        }catch (Exception e) {
            Log.w(TAG, "failed to read /nv/item_files/ims/qp_ims_ut_config_item:");
            Log.e(TAG, e.getMessage());
        }

        Log.i(TAG, "writeXcapUserAgent() , get XcapUserAgent = " + XcapUserAgent);
        if(null != XcapUserAgent) {
            if (!XcapUserAgent.equals(newStringVal)){
                try {
                    mSysRil.setStringVal(ISysRilCmd.RIL_SUB_CMD_STRING_XCAP_USER_AGENT, newStringVal);
                    Log.i(TAG, "Wrote "
                            + (null != newStringVal && newStringVal.length() > 0 ? newStringVal : "<empty>")
                            + " into cUaUserAgent in /nv/item_files/ims/qp_ims_ut_config_item");
                }
                catch (Exception e) {
                    Log.w(TAG, "Failed to write VoWiFiProvId in /nv/item_files/ims/qp_ims_ut_config_item");
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }

    private final void writeIMSUserAgent() {
        String newStringVal = null;
        String brand = SystemProperties.get("ro.product.brand", "");
        String model = SystemProperties.get("ro.product.model", "");
        String softVer = SystemProperties.get("ro.build.version.incremental", "");

        String PROPERTY_ICC_OPERATOR_NUMERIC = "gsm.sim.operator.numeric";
        String mccmnc = SystemProperties.get(PROPERTY_ICC_OPERATOR_NUMERIC);
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            mccmnc = TelephonyManager.getTelephonyProperty(mPhoneId, PROPERTY_ICC_OPERATOR_NUMERIC, "0");
        }

        if(TextUtils.isEmpty(mccmnc)) {
            Log.i(TAG, "writeIMSUserAgent() ,mccmnc is null, not handle.");
            return;
        } else if(!TextUtils.isEmpty(brand) && !TextUtils.isEmpty(model) && !TextUtils.isEmpty(softVer)) {
            newStringVal = brand + "_" + model + "_" + softVer;
        }

        if(TextUtils.isEmpty(newStringVal)) {
            Log.i(TAG, "writeIMSUserAgent() ,newStringVal as null, not handle, else overwrite modem value");
            return;
        }else{
            Log.i(TAG, "writeIMSUserAgent() , newStringVal = " + newStringVal);
        }

        /*   NV69689:
         *
         *   IMSUserAgent                           1024
         */
        String IMSUserAgent = null;
        try {
            IMSUserAgent = mSysRil.getDBStringVal(ISysRilCmd.RIL_SUB_CMD_STRING_IMS_USERAGENT);
        }catch (Exception e) {
            Log.w(TAG, "failed to read /nv/item_files/ims/ims_user_agent:");
            Log.e(TAG, e.getMessage());
        }

        Log.i(TAG, "writeIMSUserAgent() , get IMSUserAgent = " + IMSUserAgent);
        if(null != IMSUserAgent) {
            if (!IMSUserAgent.equals(newStringVal)){
                try {
                    mSysRil.setStringVal(ISysRilCmd.RIL_SUB_CMD_STRING_IMS_USERAGENT, newStringVal);
                    Log.i(TAG, "Wrote "
                            + (null != newStringVal && newStringVal.length() > 0 ? newStringVal : "<empty>")
                            + " into cUaUserAgent in /nv/item_files/ims/qp_ims_ut_config_item");
                }
                catch (Exception e) {
                    Log.w(TAG, "Failed to write VoWiFiProvId in /nv/item_files/ims/ims_user_agent");
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }
}
