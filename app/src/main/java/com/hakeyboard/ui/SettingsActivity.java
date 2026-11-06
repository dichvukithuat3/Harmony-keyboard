package com.hakeyboard.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.hakeyboard.R;
import com.hakeyboard.ime.VietKeyboardService;

/**
 * Launcher screen: the three steps needed to start using the keyboard, plus
 * every setting the keyboard reads at runtime.
 */
public class SettingsActivity extends Activity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Button enable = (Button) findViewById(R.id.btnEnable);
        enable.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));

        Button pick = (Button) findViewById(R.id.btnPick);
        pick.setOnClickListener(v -> {
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showInputMethodPicker();
            }
        });

        RadioGroup modeGroup = (RadioGroup) findViewById(R.id.modeGroup);
        boolean vni = "vni".equals(prefs.getString(VietKeyboardService.PREF_MODE, "telex"));
        ((RadioButton) findViewById(vni ? R.id.modeVni : R.id.modeTelex)).setChecked(true);
        modeGroup.setOnCheckedChangeListener((group, checkedId) ->
                prefs.edit()
                        .putString(VietKeyboardService.PREF_MODE,
                                checkedId == R.id.modeVni ? "vni" : "telex")
                        .apply());

        bindCheck(R.id.chkVibrate, VietKeyboardService.PREF_VIBRATE, true);
        bindCheck(R.id.chkSound, VietKeyboardService.PREF_SOUND, false);
        bindCheck(R.id.chkPopup, VietKeyboardService.PREF_POPUP, true);
        bindCheck(R.id.chkNumberRow, VietKeyboardService.PREF_NUMBER_ROW, false);

        RadioGroup heightGroup = (RadioGroup) findViewById(R.id.heightGroup);
        int height = prefs.getInt(VietKeyboardService.PREF_HEIGHT, 1);
        int heightId = height == 0 ? R.id.heightShort : height == 2 ? R.id.heightTall : R.id.heightNormal;
        ((RadioButton) findViewById(heightId)).setChecked(true);
        heightGroup.setOnCheckedChangeListener((group, checkedId) -> {
            int value = checkedId == R.id.heightShort ? 0 : checkedId == R.id.heightTall ? 2 : 1;
            prefs.edit().putInt(VietKeyboardService.PREF_HEIGHT, value).apply();
        });
    }

    private void bindCheck(int viewId, final String prefKey, boolean defaultValue) {
        CheckBox box = (CheckBox) findViewById(viewId);
        box.setChecked(prefs.getBoolean(prefKey, defaultValue));
        box.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(prefKey, isChecked).apply());
    }
}
