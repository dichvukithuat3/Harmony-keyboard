package com.hakeyboard.ime;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.hakeyboard.R;
import com.hakeyboard.core.VietnameseEngine;
import com.hakeyboard.core.VietnameseEngine.Mode;
import com.hakeyboard.core.WordComposer;
import com.hakeyboard.ime.KeyboardLayouts.Action;
import com.hakeyboard.ime.KeyboardLayouts.Key;

import java.util.ArrayList;
import java.util.List;

/**
 * The keyboard itself.
 *
 * Text is applied with deleteSurroundingText + commitText rather than a
 * composing region: composing regions behave inconsistently across apps
 * (especially WebView-based ones), while plain commits work everywhere.
 */
public class VietKeyboardService extends InputMethodService {

    public static final String PREF_MODE = "pref_mode";          // "telex" | "vni"
    public static final String PREF_VIETNAMESE = "pref_vietnamese";
    public static final String PREF_VIBRATE = "pref_vibrate";
    public static final String PREF_SOUND = "pref_sound";
    public static final String PREF_POPUP = "pref_popup";
    public static final String PREF_NUMBER_ROW = "pref_number_row";
    public static final String PREF_HEIGHT = "pref_height";      // 0 short, 1 normal, 2 tall

    private static final int SHIFT_OFF = 0;
    private static final int SHIFT_ON = 1;
    private static final int SHIFT_LOCK = 2;

    private static final int LAYER_LETTERS = 0;
    private static final int LAYER_SYMBOLS = 1;
    private static final int LAYER_MORE = 2;

    private static final long DELETE_FIRST_DELAY_MS = 400;
    private static final long DELETE_REPEAT_MS = 55;
    private static final long LONG_PRESS_MS = 320;
    private static final long DOUBLE_SHIFT_MS = 350;

    private final WordComposer composer = new WordComposer();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private SharedPreferences prefs;
    private Vibrator vibrator;
    private AudioManager audioManager;

    private LinearLayout rootView;
    private LinearLayout keysContainer;
    private TextView languageKeyView;
    private TextView modeChipView;
    private TextView shiftKeyView;

    private int shiftState = SHIFT_OFF;
    private int layer = LAYER_LETTERS;
    private boolean vietnameseOn = true;
    private long lastShiftTapAt = 0;

    private PopupWindow popup;
    private final List<TextView> popupItems = new ArrayList<>();
    private int popupSelection = -1;

    // ------------------------------------------------------------ lifecycle

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vietnameseOn = prefs.getBoolean(PREF_VIETNAMESE, true);
        composer.setMode(readMode());
    }

    @Override
    public View onCreateInputView() {
        rootView = new LinearLayout(this);
        rootView.setOrientation(LinearLayout.VERTICAL);
        rootView.setBackgroundColor(getColorCompat(R.color.kb_background));
        rootView.addView(buildToolbar());

        keysContainer = new LinearLayout(this);
        keysContainer.setOrientation(LinearLayout.VERTICAL);
        rootView.addView(keysContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        rebuildKeys();
        return rootView;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        composer.setMode(readMode());
        vietnameseOn = prefs.getBoolean(PREF_VIETNAMESE, true);
        shiftState = SHIFT_OFF;
        layer = LAYER_LETTERS;
        composer.reset();
        rebuildKeys();
    }

    @Override
    public void onFinishInput() {
        super.onFinishInput();
        composer.reset();
        dismissPopup();
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        // The cursor moved for a reason other than our own typing — whatever
        // syllable we were building is no longer next to the caret.
        if (!composer.isEmpty() && newSelStart != oldSelStart + 1) {
            composer.reset();
        }
    }

    // -------------------------------------------------------------- toolbar

    private LinearLayout buildToolbar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(getColorCompat(R.color.kb_toolbar));
        bar.setGravity(Gravity.CENTER_VERTICAL);
        int pad = dp(6);
        bar.setPadding(pad, pad, pad, pad);

        modeChipView = chip(composer.getMode() == Mode.TELEX ? "Telex" : "VNI");
        modeChipView.setOnClickListener(v -> {
            Mode next = composer.getMode() == Mode.TELEX ? Mode.VNI : Mode.TELEX;
            prefs.edit().putString(PREF_MODE, next == Mode.TELEX ? "telex" : "vni").apply();
            composer.setMode(next);
            modeChipView.setText(next == Mode.TELEX ? "Telex" : "VNI");
            feedback();
        });
        bar.addView(modeChipView);

        TextView spacer = new TextView(this);
        bar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));

        TextView hint = new TextView(this);
        hint.setText(R.string.ime_name);
        hint.setTextColor(getColorCompat(R.color.kb_text_dim));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        hint.setPadding(dp(8), 0, dp(8), 0);
        bar.addView(hint);

        return bar;
    }

    private TextView chip(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(getColorCompat(R.color.kb_text));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundResource(R.drawable.key_special);
        tv.setPadding(dp(14), dp(6), dp(14), dp(6));
        return tv;
    }

    // ----------------------------------------------------------- key layout

    private void rebuildKeys() {
        if (keysContainer == null) {
            return;
        }
        keysContainer.removeAllViews();
        languageKeyView = null;
        shiftKeyView = null;

        if (layer == LAYER_LETTERS && prefs.getBoolean(PREF_NUMBER_ROW, false)) {
            keysContainer.addView(buildRow(KeyboardLayouts.NUMBER_ROW));
        }

        Key[][] rows;
        if (layer == LAYER_SYMBOLS) {
            rows = KeyboardLayouts.SYMBOLS;
        } else if (layer == LAYER_MORE) {
            rows = KeyboardLayouts.MORE_SYMBOLS;
        } else {
            rows = KeyboardLayouts.LETTERS;
        }
        for (Key[] row : rows) {
            keysContainer.addView(buildRow(row));
        }
    }

    private LinearLayout buildRow(Key[] keys) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int m = dp(3);
        row.setPadding(m, m / 2, m, m / 2);

        for (Key key : keys) {
            TextView view = buildKeyView(key);
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(0, keyHeightPx(), key.weight);
            lp.setMargins(m / 2, 0, m / 2, 0);
            row.addView(view, lp);
        }
        return row;
    }

    private TextView buildKeyView(final Key key) {
        final TextView tv = new TextView(this);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(getColorCompat(R.color.kb_text));
        tv.setText(displayLabel(key));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, key.action == Action.CHAR ? 19 : 15);
        tv.setBackgroundResource(key.style == 2 ? R.drawable.key_accent
                : key.style == 1 ? R.drawable.key_special : R.drawable.key_normal);

        if (key.action == Action.SHIFT) {
            shiftKeyView = tv;
            paintShiftKey();
        }
        if (key.action == Action.LANGUAGE) {
            languageKeyView = tv;
            paintLanguageKey();
        }

        attachTouch(tv, key);
        return tv;
    }

    private String displayLabel(Key key) {
        if (key.action == Action.LANGUAGE) {
            return vietnameseOn ? "VI" : "EN";
        }
        if (key.action != Action.CHAR) {
            return key.label;
        }
        return isShifted() ? key.label.toUpperCase() : key.label;
    }

    private int keyHeightPx() {
        int level = prefs.getInt(PREF_HEIGHT, 1);
        int dpHeight = level == 0 ? 44 : level == 2 ? 60 : 52;
        return dp(dpHeight);
    }

    // -------------------------------------------------------- touch handling

    private void attachTouch(final TextView view, final Key key) {
        final Runnable longPress = () -> onLongPress(view, key);
        final Runnable[] repeat = new Runnable[1];

        view.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    feedback();
                    if (key.action == Action.DELETE) {
                        handleKey(key, null);
                        repeat[0] = new Runnable() {
                            @Override
                            public void run() {
                                handleKey(key, null);
                                handler.postDelayed(this, DELETE_REPEAT_MS);
                            }
                        };
                        handler.postDelayed(repeat[0], DELETE_FIRST_DELAY_MS);
                    } else {
                        if (!TextUtils.isEmpty(key.longPress)) {
                            handler.postDelayed(longPress, LONG_PRESS_MS);
                        }
                        showPreview(view, key);
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (popup != null && popup.isShowing() && !popupItems.isEmpty()) {
                        updatePopupSelection(view, event);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    handler.removeCallbacks(longPress);
                    if (repeat[0] != null) {
                        handler.removeCallbacks(repeat[0]);
                        repeat[0] = null;
                    }
                    boolean consumedByPopup = false;
                    if (popup != null && popup.isShowing() && !popupItems.isEmpty()) {
                        if (popupSelection >= 0 && event.getActionMasked() == MotionEvent.ACTION_UP) {
                            commitChar(popupItems.get(popupSelection).getText().toString());
                        }
                        consumedByPopup = true;
                    }
                    dismissPopup();
                    if (!consumedByPopup
                            && event.getActionMasked() == MotionEvent.ACTION_UP
                            && key.action != Action.DELETE) {
                        handleKey(key, view);
                    }
                    return true;
                default:
                    return false;
            }
        });
    }

    private void onLongPress(TextView anchor, Key key) {
        if (TextUtils.isEmpty(key.longPress)) {
            return;
        }
        showAlternates(anchor, key.longPress);
    }

    // ------------------------------------------------------------- key logic

    private void handleKey(Key key, View view) {
        switch (key.action) {
            case CHAR:
                commitChar(isShifted() ? key.label.toUpperCase() : key.label);
                if (shiftState == SHIFT_ON) {
                    shiftState = SHIFT_OFF;
                    refreshLetterCase();
                }
                break;
            case SHIFT:
                onShift();
                break;
            case DELETE:
                onDelete();
                break;
            case SPACE:
                composer.reset();
                commitRaw(" ");
                break;
            case ENTER:
                composer.reset();
                onEnter();
                break;
            case SYMBOLS:
                composer.reset();
                layer = LAYER_SYMBOLS;
                rebuildKeys();
                break;
            case MORE_SYMBOLS:
                composer.reset();
                layer = LAYER_MORE;
                rebuildKeys();
                break;
            case LETTERS:
                composer.reset();
                layer = LAYER_LETTERS;
                rebuildKeys();
                break;
            case LANGUAGE:
                vietnameseOn = !vietnameseOn;
                prefs.edit().putBoolean(PREF_VIETNAMESE, vietnameseOn).apply();
                composer.reset();
                paintLanguageKey();
                break;
            default:
                break;
        }
    }

    private void commitChar(String text) {
        if (text.length() != 1) {
            composer.reset();
            commitRaw(text);
            return;
        }
        char ch = text.charAt(0);

        boolean compose = vietnameseOn
                && layer == LAYER_LETTERS
                && WordComposer.isWordChar(ch);
        if (!compose) {
            composer.reset();
            commitRaw(text);
            return;
        }
        applyEdit(composer.push(ch));
    }

    private void onDelete() {
        if (!composer.isEmpty()) {
            applyEdit(composer.pop());
            return;
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }
        CharSequence selected = ic.getSelectedText(0);
        if (selected != null && selected.length() > 0) {
            ic.commitText("", 1);
        } else {
            ic.deleteSurroundingText(1, 0);
        }
    }

    private void onEnter() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }
        EditorInfo info = getCurrentInputEditorInfo();
        if (info != null) {
            int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
            boolean suppressed = (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
            if (!suppressed && action != EditorInfo.IME_ACTION_NONE
                    && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
                ic.performEditorAction(action);
                return;
            }
        }
        ic.commitText("\n", 1);
    }

    private void onShift() {
        long now = System.currentTimeMillis();
        if (shiftState != SHIFT_OFF && now - lastShiftTapAt < DOUBLE_SHIFT_MS) {
            shiftState = SHIFT_LOCK;
        } else if (shiftState == SHIFT_OFF) {
            shiftState = SHIFT_ON;
        } else {
            shiftState = SHIFT_OFF;
        }
        lastShiftTapAt = now;
        refreshLetterCase();
    }

    private boolean isShifted() {
        return shiftState != SHIFT_OFF;
    }

    private void applyEdit(WordComposer.Edit edit) {
        if (edit.isNoop()) {
            return;
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) {
            return;
        }
        ic.beginBatchEdit();
        if (edit.deleteCount > 0) {
            ic.deleteSurroundingText(edit.deleteCount, 0);
        }
        if (!edit.insertText.isEmpty()) {
            ic.commitText(edit.insertText, 1);
        }
        ic.endBatchEdit();
    }

    private void commitRaw(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }

    // ----------------------------------------------------------- key visuals

    private void refreshLetterCase() {
        if (keysContainer == null) {
            return;
        }
        paintShiftKey();
        for (int r = 0; r < keysContainer.getChildCount(); r++) {
            View rowView = keysContainer.getChildAt(r);
            if (!(rowView instanceof LinearLayout)) {
                continue;
            }
            LinearLayout row = (LinearLayout) rowView;
            for (int c = 0; c < row.getChildCount(); c++) {
                View child = row.getChildAt(c);
                if (!(child instanceof TextView) || child == shiftKeyView || child == languageKeyView) {
                    continue;
                }
                TextView tv = (TextView) child;
                CharSequence label = tv.getText();
                if (label.length() == 1 && Character.isLetter(label.charAt(0))) {
                    tv.setText(isShifted()
                            ? label.toString().toUpperCase()
                            : label.toString().toLowerCase());
                }
            }
        }
    }

    private void paintShiftKey() {
        if (shiftKeyView == null) {
            return;
        }
        shiftKeyView.setText(shiftState == SHIFT_LOCK ? "⇪" : "⇧");
        shiftKeyView.setBackgroundResource(
                shiftState == SHIFT_OFF ? R.drawable.key_special : R.drawable.key_accent);
    }

    private void paintLanguageKey() {
        if (languageKeyView == null) {
            return;
        }
        languageKeyView.setText(vietnameseOn ? "VI" : "EN");
        languageKeyView.setBackgroundResource(
                vietnameseOn ? R.drawable.key_accent : R.drawable.key_special);
    }

    // --------------------------------------------------------------- popups

    private void showPreview(TextView anchor, Key key) {
        if (!prefs.getBoolean(PREF_POPUP, true)) {
            return;
        }
        if (key.action != Action.CHAR || " ".equals(key.label)) {
            return;
        }
        TextView label = new TextView(this);
        label.setText(displayLabel(key));
        label.setTextColor(getColorCompat(R.color.kb_text));
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        label.setGravity(Gravity.CENTER);
        label.setBackgroundResource(R.drawable.popup_bg);
        label.setPadding(dp(14), dp(6), dp(14), dp(10));
        showPopupAbove(anchor, label, anchor.getWidth());
        popupItems.clear();
        popupSelection = -1;
    }

    private void showAlternates(TextView anchor, String alternates) {
        dismissPopup();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setBackgroundResource(R.drawable.popup_bg);
        box.setPadding(dp(4), dp(4), dp(4), dp(4));

        popupItems.clear();
        for (int i = 0; i < alternates.length(); i++) {
            TextView item = new TextView(this);
            String c = String.valueOf(alternates.charAt(i));
            item.setText(isShifted() ? c.toUpperCase() : c);
            item.setTextColor(getColorCompat(R.color.kb_text));
            item.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(14), dp(8), dp(14), dp(8));
            box.addView(item);
            popupItems.add(item);
        }
        popupSelection = 0;
        highlightPopup();
        showPopupAbove(anchor, box, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private void showPopupAbove(View anchor, View content, int width) {
        dismissPopup();
        popup = new PopupWindow(content, width, ViewGroup.LayoutParams.WRAP_CONTENT, false);
        popup.setClippingEnabled(false);
        content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int[] loc = new int[2];
        anchor.getLocationInWindow(loc);
        int height = content.getMeasuredHeight();
        try {
            popup.showAtLocation(anchor, Gravity.NO_GRAVITY, loc[0], loc[1] - height - dp(4));
        } catch (RuntimeException ignored) {
            popup = null;
        }
    }

    private void updatePopupSelection(View anchor, MotionEvent event) {
        if (popupItems.isEmpty()) {
            return;
        }
        float x = event.getRawX();
        int[] loc = new int[2];
        popupItems.get(0).getLocationOnScreen(loc);
        int best = -1;
        for (int i = 0; i < popupItems.size(); i++) {
            TextView item = popupItems.get(i);
            int[] il = new int[2];
            item.getLocationOnScreen(il);
            if (x >= il[0] && x <= il[0] + item.getWidth()) {
                best = i;
                break;
            }
        }
        if (best >= 0 && best != popupSelection) {
            popupSelection = best;
            highlightPopup();
        }
    }

    private void highlightPopup() {
        for (int i = 0; i < popupItems.size(); i++) {
            popupItems.get(i).setBackgroundColor(
                    i == popupSelection ? getColorCompat(R.color.kb_key_accent) : Color.TRANSPARENT);
        }
    }

    private void dismissPopup() {
        if (popup != null) {
            try {
                popup.dismiss();
            } catch (RuntimeException ignored) {
                // window already gone
            }
            popup = null;
        }
        popupItems.clear();
        popupSelection = -1;
    }

    // -------------------------------------------------------------- helpers

    private Mode readMode() {
        return "vni".equals(prefs.getString(PREF_MODE, "telex")) ? Mode.VNI : Mode.TELEX;
    }

    @SuppressWarnings("deprecation")
    private void feedback() {
        if (prefs.getBoolean(PREF_VIBRATE, true) && vibrator != null && vibrator.hasVibrator()) {
            // The one-shot form is deprecated on API 26+ but still honoured,
            // and it keeps this file compilable against older platform jars.
            vibrator.vibrate(12);
        }
        if (prefs.getBoolean(PREF_SOUND, false) && audioManager != null) {
            audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.4f);
        }
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }

    @SuppressWarnings("deprecation")
    private int getColorCompat(int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getResources().getColor(resId, getTheme());
        }
        return getResources().getColor(resId);
    }

    /** Exposed for the settings screen so it can show a live example. */
    public static String preview(String raw, Mode mode) {
        return VietnameseEngine.transform(raw, mode);
    }
}
