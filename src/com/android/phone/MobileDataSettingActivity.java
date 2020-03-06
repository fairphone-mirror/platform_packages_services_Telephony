/*
 * Copyright 2020-2022 Fairphone B.V.
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.android.setupwizardlib.util.ResultCodes;
import com.android.setupwizardlib.util.WizardManagerHelper;
import com.android.setupwizardlib.view.NavigationBar;
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener;

public class MobileDataSettingActivity extends Activity implements NavigationBarListener {
    public static final int NEXT_REQUEST_CODE = 1;
    public static final int RESULT_SKIP = ResultCodes.RESULT_SKIP;
    public static final String MOBILE_DATA_SETTING_TITLE = "Mobile data";
    public static final String INTENT_DATA_OFF = "com.android.internal.telephony.DATA_OFF";
    public static final String INTENT_DATA_ON = "com.android.internal.telephony.DATA_ON";
    public boolean mChecked = true;
    public CheckBox mCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mobile_data_setting);

        getNavigationBar().setNavigationBarListener(this);

        mCheckBox = (CheckBox) findViewById(R.id.enabledata);
        mCheckBox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mChecked = isChecked;
                        mCheckBox.setChecked(isChecked);
                    }
                });
    }

    protected NavigationBar getNavigationBar() {
        return (NavigationBar) findViewById(R.id.suw_layout_navigation_bar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCheckBox != null) mCheckBox.setChecked(true);
    }

    @Override
    public void onNavigateBack() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onNavigateNext() {
        Intent intent = null;
        mChecked = mCheckBox.isChecked();
        if (mChecked) intent = new Intent(INTENT_DATA_ON);
        else intent = new Intent(INTENT_DATA_OFF);
        sendBroadcast(intent);
        finish(RESULT_OK);
        done(true);
    }

    private void finish(int resultCode) {
        setResult(resultCode);
    }

    public void done(boolean success) {
        int resultCode = success ? Activity.RESULT_OK : RESULT_SKIP;
        Intent intent = WizardManagerHelper.getNextIntent(getIntent(), resultCode);
        startActivityForResult(intent, NEXT_REQUEST_CODE);
    }
}
