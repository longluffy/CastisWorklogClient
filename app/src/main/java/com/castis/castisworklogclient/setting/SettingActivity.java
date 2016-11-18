package com.castis.castisworklogclient.setting;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.castis.castisworklogclient.R;

public class SettingActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // add the xml resource
        addPreferencesFromResource(R.xml.ciwls_setting);
    }
}