package com.android.phone.settings;

import static android.telephony.TelephonyManager.HAL_SERVICE_NETWORK;

import static com.android.internal.telephony.RIL.RADIO_HAL_VERSION_1_6;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.qualcomm.qcrilhook.QcRilHook;
import com.qualcomm.qcrilhook.QcRilHookCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.AccessNetworkConstants;
import android.telephony.RadioAccessSpecifier;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BandSelection extends Activity {
    private static final String TAG = "BandSelection";

    private boolean mQcRilHookReady = false;
    private QcRilHook mQcRilHook;
    private final int EVENT_QCRIL_HOOK_READY = 2;

    private class SysBandCls {
        public String mKey;
        public int mValue;

        public SysBandCls(String Key, int Value) {
            mKey = Key;
            mValue = Value;
        }
    }

    /*
     * ReadMe
     * check MAX_BANDS in ril.h
     * check RIL_GeranBands/RIL_GeranBands/RIL_EutranBands/RIL_NgranBands in ril.h
     * check qcril_qmi_nas_fill_band_info() in vendor/qcom/proprietary/qcril-hal/modules/nas/src/qcril_qmi_nas.cpp
    */
    private final int mGSMBandNum = 4;
    private final int mWCDMABandNum = 4;
    private final int mLTEBandNum = 20;
    private final int mNRBandNum = 15;

    private final int mPosSysBandBegin = 0;
    private final int mPosSysBandEnd = mGSMBandNum + mWCDMABandNum + mLTEBandNum + mNRBandNum;

    private final int mPosGSMBandBegin = 0;
    private final int mPosGSMBandEnd = mPosGSMBandBegin + mGSMBandNum;
    private final int mPosWCDMABandBegin = mGSMBandNum;
    private final int mPosWCDMABandEnd = mPosWCDMABandBegin + mWCDMABandNum;

    private final int mPosLTEBegin = mGSMBandNum + mWCDMABandNum;
    private final int mPosLTEEnd = mPosLTEBegin + mLTEBandNum;
    private final int mPosNRBegin = mGSMBandNum + mWCDMABandNum + mLTEBandNum;
    private final int mPosNREnd = mPosNRBegin + mNRBandNum;

    // Bands available for PM95
    private SysBandCls mSysBand[] = {
            // GSM band
            //new SysBandCls("GSM T380",              1),
            //new SysBandCls("GSM T410",              2),
            //new SysBandCls("GSM 450",               3),
            //new SysBandCls("GSM 480",               4),
            //new SysBandCls("GSM 710",               5),
            //new SysBandCls("GSM 750",               6),
            //new SysBandCls("GSM T810",              7),
            new SysBandCls("GSM 850",               8),
            //new SysBandCls("GSM P900",              9),
            new SysBandCls("GSM E900",              10),
            //new SysBandCls("GSM R900",              11),
            new SysBandCls("GSM DCS1800",           12),
            new SysBandCls("GSM PCS1900",           13),
            //new SysBandCls("GSM ER900",             14),

            // WCDMA band
            new SysBandCls("UTRAN EU_J_CH_IMT_2100",1),
            new SysBandCls("UTRAN US_PCS_1900",     2),
            //new SysBandCls("UTRAN EU_CH_DCS_1800",  3),
            //new SysBandCls("UTRAN US_1700",         4),
            new SysBandCls("UTRAN US_850",          5),
            //new SysBandCls("UTRAN JAPAN_800",       6),
            //new SysBandCls("UTRAN EU_2600",         7),
            new SysBandCls("UTRAN EU_J_900",        8),
            //new SysBandCls("UTRAN J_1700",          9),
            //new SysBandCls("UTRAN BAND10",          10),
            //new SysBandCls("UTRAN BAND11",          11),
            //new SysBandCls("UTRAN BAND12",          12),
            //new SysBandCls("UTRAN BAND13",          13),
            //new SysBandCls("UTRAN BAND14",          14),
            //new SysBandCls("UTRAN JAPAN_850",       19),
            //new SysBandCls("UTRAN BAND20",          20),
            //new SysBandCls("UTRAN BAND21",          21),
            //new SysBandCls("UTRAN BAND22",          22),
            //new SysBandCls("UTRAN BAND25",          25),
            //new SysBandCls("UTRAN BAND26",          26),

            // LTE band
            new SysBandCls("EUTRAN BAND1",          1),
            new SysBandCls("EUTRAN BAND2",          2),
            new SysBandCls("EUTRAN BAND3",          3),
            new SysBandCls("EUTRAN BAND4",          4),
            new SysBandCls("EUTRAN BAND5",          5),
            //new SysBandCls("EUTRAN BAND6",          6),
            new SysBandCls("EUTRAN BAND7",          7),
            new SysBandCls("EUTRAN BAND8",          8),
            //new SysBandCls("EUTRAN BAND9",          9),
            //new SysBandCls("EUTRAN BAND10",         10),
            //new SysBandCls("EUTRAN BAND11",         11),
            new SysBandCls("EUTRAN BAND12",         12),
            //new SysBandCls("EUTRAN BAND13",         13),
            //new SysBandCls("EUTRAN BAND14",         14),
            new SysBandCls("EUTRAN BAND17",         17),
            //new SysBandCls("EUTRAN BAND18",         18),
            //new SysBandCls("EUTRAN BAND19",         19),
            new SysBandCls("EUTRAN BAND20",         20),
            //new SysBandCls("EUTRAN BAND21",         21),
            //new SysBandCls("EUTRAN BAND22",         22),
            //new SysBandCls("EUTRAN BAND23",         23),
            //new SysBandCls("EUTRAN BAND24",         24),
            new SysBandCls("EUTRAN BAND25",         25),
            new SysBandCls("EUTRAN BAND26",         26),
            //new SysBandCls("EUTRAN BAND27",         27),
            new SysBandCls("EUTRAN BAND28",         28),
            //new SysBandCls("EUTRAN BAND29",         29),
            //new SysBandCls("EUTRAN BAND30",         30),
            //new SysBandCls("EUTRAN BAND31",         31),
            new SysBandCls("EUTRAN BAND32",         32),
            //new SysBandCls("EUTRAN BAND33",         33),
            //new SysBandCls("EUTRAN BAND34",         34),
            //new SysBandCls("EUTRAN BAND35",         35),
            //new SysBandCls("EUTRAN BAND36",         36),
            //new SysBandCls("EUTRAN BAND37",         37),
            new SysBandCls("EUTRAN BAND38",         38),
            //new SysBandCls("EUTRAN BAND39",         39),
            new SysBandCls("EUTRAN BAND40",         40),
            new SysBandCls("EUTRAN BAND41",         41),
            new SysBandCls("EUTRAN BAND42",         42),
            //new SysBandCls("EUTRAN BAND43",         43),
            //new SysBandCls("EUTRAN BAND44",         44),
            //new SysBandCls("EUTRAN BAND45",         45),
            //new SysBandCls("EUTRAN BAND46",         46),
            //new SysBandCls("EUTRAN BAND47",         47),
            //new SysBandCls("EUTRAN BAND48",         48),
            //new SysBandCls("EUTRAN BAND49",         49),
            //new SysBandCls("EUTRAN BAND65",         65),
            new SysBandCls("EUTRAN BAND66",         66),
            //new SysBandCls("EUTRAN BAND67",         67),
            //new SysBandCls("EUTRAN BAND68",         68),
            //new SysBandCls("EUTRAN BAND70",         70),
            new SysBandCls("EUTRAN BAND71",         71),
            //new SysBandCls("EUTRAN BAND72",         72),
            //new SysBandCls("EUTRAN BAND73",         73),
            //new SysBandCls("EUTRAN BAND85",         85),
            //new SysBandCls("EUTRAN BAND125",        125),
            //new SysBandCls("EUTRAN BAND126",        126),
            //new SysBandCls("EUTRAN BAND127",        127),
            //new SysBandCls("EUTRAN BAND250",        250),

            // NR band
            new SysBandCls("NGRAN BAND1",           1),
            new SysBandCls("NGRAN BAND2",           2),
            new SysBandCls("NGRAN BAND3",           3),
            new SysBandCls("NGRAN BAND5",           5),
            new SysBandCls("NGRAN BAND7",           7),
            new SysBandCls("NGRAN BAND8",           8),
            //new SysBandCls("NGRAN BAND12",          12),
            //new SysBandCls("NGRAN BAND13",          13),
            //new SysBandCls("NGRAN BAND14",          14),
            //new SysBandCls("NGRAN BAND18",          18),
            new SysBandCls("NGRAN BAND20",          20),
            //new SysBandCls("NGRAN BAND25",          25),
            //new SysBandCls("NGRAN BAND26",          26),
            new SysBandCls("NGRAN BAND28",          28),
            //new SysBandCls("NGRAN BAND29",          29),
            //new SysBandCls("NGRAN BAND30",          30),
            //new SysBandCls("NGRAN BAND34",          34),
            new SysBandCls("NGRAN BAND38",          38),
            //new SysBandCls("NGRAN BAND39",          39),
            new SysBandCls("NGRAN BAND40",          40),
            new SysBandCls("NGRAN BAND41",          41),
            //new SysBandCls("NGRAN BAND48",          48),
            //new SysBandCls("NGRAN BAND50",          50),
            //new SysBandCls("NGRAN BAND51",          51),
            //new SysBandCls("NGRAN BAND65",          65),
            new SysBandCls("NGRAN BAND66",          66),
            //new SysBandCls("NGRAN BAND70",          70),
            new SysBandCls("NGRAN BAND71",          71),
            //new SysBandCls("NGRAN BAND74",          74),
            //new SysBandCls("NGRAN BAND75",          75),
            //new SysBandCls("NGRAN BAND76",          76),
            new SysBandCls("NGRAN BAND77",          77),
            new SysBandCls("NGRAN BAND78",          78),
            //new SysBandCls("NGRAN BAND79",          79),
            //new SysBandCls("NGRAN BAND80",          80),
            //new SysBandCls("NGRAN BAND81",          81),
            //new SysBandCls("NGRAN BAND82",          82),
            //new SysBandCls("NGRAN BAND83",          83),
            //new SysBandCls("NGRAN BAND84",          84),
            //new SysBandCls("NGRAN BAND85",          85),
            //new SysBandCls("NGRAN BAND86",          86),
            //new SysBandCls("NGRAN BAND90",          90),
            //new SysBandCls("NGRAN BAND257",         257),
            //new SysBandCls("NGRAN BAND258",         258),
            //new SysBandCls("NGRAN BAND259",         259),
            //new SysBandCls("NGRAN BAND260",         260),
            //new SysBandCls("NGRAN BAND261",         261),
    };

    private boolean[] mItemChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItemChecked = new boolean[mSysBand.length];
        mQcRilHook = new QcRilHook(this, mQcRilHookCallback);
    }

    private QcRilHookCallback mQcRilHookCallback = new QcRilHookCallback() {
        @Override
        public void onQcRilHookReady() {
            Log.d(TAG, "QcRilHook is ready");
            mQcRilHookReady = true;
            mHandler.sendEmptyMessage(EVENT_QCRIL_HOOK_READY);
        }

        @Override
        public void onQcRilHookDisconnected() {
            // TODO: Handle onQcRilHookDisconnected
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_QCRIL_HOOK_READY:
                    Log.d(TAG, "EVENT_QCRIL_HOOK_READY");
                    getSysPrefBand();
                    CreateDialog();
                    break;
                default:
                    Log.d(TAG, "Unexpected event:" + msg.what);
                    break;
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private void CreateDialog() {
        String multiChoiceArr[] = new String[mSysBand.length];

        for (int i = 0; i < mSysBand.length; i++) {
            multiChoiceArr[i] = mSysBand[i].mKey;
        }

        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert)
                .setTitle("Band preferences selection UI")
                .setCancelable(false)
                .setMultiChoiceItems(multiChoiceArr, mItemChecked, new DialogInterface.OnMultiChoiceClickListener() {
                    /**
                     * This method will be invoked when an item in the dialog is clicked.
                     *
                     * @param dialog    the dialog where the selection was made
                     * @param which     the position of the item in the list that was clicked
                     * @param isChecked {@code true} if the click checked the item, else
                     *                  {@code false}
                     */
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        return;
                    }
                })
                .setPositiveButton("SAVE", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        setSysPrefBand();
                        finish();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        finish();
                    }
                })
                .create().show();
    }

    private void getSysPrefBand() {
        int phoneId = 0;
        Phone phone = PhoneFactory.getPhone(phoneId);
        if (phone != null && phone.getHalVersion(HAL_SERVICE_NETWORK).less(RADIO_HAL_VERSION_1_6)) {
            Log.d(TAG, "HAL version of NETWORK < 1.6");
            //int[] gsmBands = mQcRilHook.qcRilGetGsmBandPref(phoneId);
            //int[] wcdmaBands = mQcRilHook.qcRilGetWcdmaBandPref(phoneId);
            //int[] lteBands = mQcRilHook.qcRilGetLteBandPref(phoneId);
            //int[] nrBands = mQcRilHook.qcRilGetNrBandPref(phoneId);
            //setCheckedBand(mPosGSMBandBegin, mPosGSMBandEnd, gsmBands);
            //setCheckedBand(mPosWCDMABandBegin, mPosWCDMABandEnd, wcdmaBands);
            //setCheckedBand(mPosLTEBegin, mPosLTEEnd, lteBands);
            //setCheckedBand(mPosNRBegin, mPosNREnd, nrBands);

            return;
        }

        Log.d(TAG, "HAL version of NETWORK >= 1.6");
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        List<RadioAccessSpecifier> band_list = telephonyManager.getSystemSelectionChannels();
        if (band_list != null && band_list.isEmpty() == false) {
            for (RadioAccessSpecifier radioAccess : band_list) {
                int ran = radioAccess.getRadioAccessNetwork();
                int[] bands = radioAccess.getBands();

                Log.d(TAG, "radio access network: " + ran);
                if (bands != null) {
                    if (ran == AccessNetworkConstants.AccessNetworkType.GERAN) {
                        setCheckedBand(mPosGSMBandBegin, mPosGSMBandEnd, bands);
                    }

                    if (ran == AccessNetworkConstants.AccessNetworkType.UTRAN) {
                        setCheckedBand(mPosWCDMABandBegin, mPosWCDMABandEnd, bands);
                    }

                    if (ran == AccessNetworkConstants.AccessNetworkType.EUTRAN) {
                        setCheckedBand(mPosLTEBegin, mPosLTEEnd, bands);
                    }

                    if (ran == AccessNetworkConstants.AccessNetworkType.NGRAN) {
                        setCheckedBand(mPosNRBegin, mPosNREnd, bands);
                    }
                }
            }
        }
    }

    private boolean setSysPrefBand() {
        List<Integer> list = new ArrayList();
        boolean disabledAllBands = true;
        int gsm_band_checked[] = null, wcdma_band_checked[] = null,
                lte_band_checked[] = null, nr_band_checked[] = null;

        gsm_band_checked = getCheckedBand(mPosGSMBandBegin, mPosGSMBandEnd);
        wcdma_band_checked = getCheckedBand(mPosWCDMABandBegin, mPosWCDMABandEnd);
        lte_band_checked = getCheckedBand(mPosLTEBegin, mPosLTEEnd);
        nr_band_checked = getCheckedBand(mPosNRBegin, mPosNREnd);

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        List<RadioAccessSpecifier> band_list = new ArrayList();
        if (gsm_band_checked != null) {
            disabledAllBands = false;
            for (int i = 0; i < gsm_band_checked.length; i++) {
                Log.d(TAG, "gsm_band_checked[" + i + "] = " + gsm_band_checked[i]);
            }
            band_list.add(new RadioAccessSpecifier(
                    AccessNetworkConstants.AccessNetworkType.GERAN, gsm_band_checked, null));
        } else {
            Log.d(TAG, "disable GSM");
            band_list.add(new RadioAccessSpecifier(
                    AccessNetworkConstants.AccessNetworkType.GERAN, null, null));
        }

        if (wcdma_band_checked != null) {
            disabledAllBands = false;
            for (int i = 0; i < wcdma_band_checked.length; i++) {
                Log.d(TAG, "wcdma_band_checked[" + i + "] = " + wcdma_band_checked[i]);
            }
            band_list.add(new RadioAccessSpecifier(
                    AccessNetworkConstants.AccessNetworkType.UTRAN, wcdma_band_checked, null));
        } else {
            Log.d(TAG, "disable WCDMA");
            band_list.add(new RadioAccessSpecifier(
                    AccessNetworkConstants.AccessNetworkType.UTRAN, null, null));
        }

        if (lte_band_checked != null) {
            disabledAllBands = false;
            for (int i = 0; i < lte_band_checked.length; i++) {
                Log.d(TAG, "lte_band_checked[" + i + "] = " + lte_band_checked[i]);
            }
            band_list.add(new RadioAccessSpecifier(
                    AccessNetworkConstants.AccessNetworkType.EUTRAN, lte_band_checked, null));
        } else {
            Log.d(TAG, "disable LTE");
            band_list.add(new RadioAccessSpecifier(
                    AccessNetworkConstants.AccessNetworkType.EUTRAN, null, null));
        }

        if (nr_band_checked != null) {
            disabledAllBands = false;
            for (int i = 0; i < nr_band_checked.length; i++) {
                Log.d(TAG, "nr_band_checked[" + i + "] = " + nr_band_checked[i]);
            }
            band_list.add(new RadioAccessSpecifier(
                    AccessNetworkConstants.AccessNetworkType.NGRAN, nr_band_checked, null));
        } else {
            Log.d(TAG, "disable 5G");
            band_list.add(new RadioAccessSpecifier(
                    AccessNetworkConstants.AccessNetworkType.NGRAN, null, null));
        }

        if (disabledAllBands) {
            Toast.makeText(this, "At least one band to be selected", Toast.LENGTH_SHORT).show();
        } else {
            telephonyManager.setSystemSelectionChannels(band_list);
            return true;
        }

        return false;
    }

    private int[] getCheckedBand(int pos_begin, int pos_end) {
        List<Integer> list = new ArrayList();
        int band[] = null;

        for (int i = pos_begin; i < pos_end; i++) {
            if (mItemChecked[i] == true) {
                list.add(mSysBand[i].mValue);
            }
        }

        if (list.isEmpty() == false) {
            band = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                band[i] = list.get(i);
            }
        }

        return band;
    }

    private void setCheckedBand(int pos_begin, int pos_end, int bands[]) {
        //Log.d(TAG, "begin = " + pos_begin + " end = " + pos_end);
        //Log.d(TAG, "bands[] = " + Arrays.toString(bands));
        if (bands != null) {
            for (int index = 0; index < bands.length; index++) {
                for (int i = pos_begin; i < pos_end; i++) {
                    if (bands[index] == mSysBand[i].mValue) {
                        mItemChecked[i] = true;
                    }
                }
            }
        }
    }
}
