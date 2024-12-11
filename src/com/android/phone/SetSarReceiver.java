/* Copyright (C) 2018 Tcl Corporation Limited */
/*
 * add by T2M.dengxiangyu for task 7965433 2019-06-28, decrease SAR when wifi hotspot on
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.qualcomm.qcrilhook.QcRilHookCallback;
import com.qualcomm.sysrilcmd.SysRilCmd;

import android.net.wifi.WifiManager;
import android.media.AudioManager;
import android.os.SystemProperties;

import android.util.Log;

public class SetSarReceiver extends BroadcastReceiver {
    private static final String TAG = "SetSarReceiver";
    private static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    public static final String PROPERTY_SAR_SWITCH = "persist.sys.ctrl.sar.enable";
    private SysRilCmd mSysRil;
    private boolean mRilHookReady = false;
    private boolean receiverOn = false;
    private boolean wifiSpotOn = false;

    private QcRilHookCallback mQcrilHookCb = new QcRilHookCallback() {
        public void onQcRilHookReady() {
            Log.d(TAG," onQcRilHookReady");
            mRilHookReady = true;
            try {
                changeSar(0);
            } catch (Exception e) {}
        }
        @Override
        public void onQcRilHookDisconnected() {
            Log.d(TAG," onQcRilHookDisconnected");
            mRilHookReady = false;
        }
    };

    public void init(Context context) {
        mSysRil = new SysRilCmd(context, mQcrilHookCb);
    }

    private void processAction() {
        Log.d(TAG," receiverOn = " + receiverOn+",wifiSpotOn = "+wifiSpotOn);
        int sarValue = (receiverOn ? (wifiSpotOn ? 3 : 2) : (wifiSpotOn ? 1 : 0));
        changeSar(sarValue);
    }

    private void changeSar(int val) {
        Log.d(TAG,"changeSar = "+val);
        if (mRilHookReady) {
            try {
                mSysRil.setTransmitMaxPower(val, 0x80);
            } catch (Exception e) {}
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean ctrlSar = SystemProperties.getBoolean(PROPERTY_SAR_SWITCH, true);

        Log.e(TAG, "onReceive: intent action = " + intent.getAction() + " ctrlSar = " + ctrlSar);
        if (!ctrlSar) return;//debug

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        Log.d(TAG, "receive intent " + intent);


         if (ACTION_BOOT_COMPLETED.equals(action)) {
            try {
                changeSar(0);
            } catch (Exception e) {}
            return;
        }

        boolean old_receiverOn = receiverOn;
        boolean old_wifiSpotOn = wifiSpotOn;

        if (action.equals(AudioManager.STREAM_DEVICES_CHANGED_ACTION)) {
            int newDevice = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_DEVICES, -1);
            int oldDevice = intent.getIntExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_DEVICES, -1);
            Log.d(TAG, "New device = " + newDevice);
            Log.d(TAG, "Old device = " + oldDevice);

            if (newDevice == AudioManager.DEVICE_OUT_EARPIECE) {
                receiverOn = true;
            } else if (oldDevice == AudioManager.DEVICE_OUT_EARPIECE) {
               receiverOn = false;
            }
        } else if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_AP_STATE, WifiManager.WIFI_AP_STATE_DISABLED);
            if (state == WifiManager.WIFI_AP_STATE_ENABLED) {
                wifiSpotOn = true;
            }
            else if (state == WifiManager.WIFI_AP_STATE_DISABLED ||
                    state == WifiManager.WIFI_AP_STATE_FAILED) {
                wifiSpotOn = false;
            }
        }


        if(old_receiverOn != receiverOn || old_wifiSpotOn != wifiSpotOn){
            processAction();
        }

    }

}
