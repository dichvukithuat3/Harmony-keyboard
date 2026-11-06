package com.hakeyboard.core;

import com.hakeyboard.core.VietnameseEngine.Mode;

/**
 * Holds the raw keys typed for the syllable currently being composed and turns
 * each new key into the smallest edit the text field needs ("delete N chars,
 * then insert this string").
 *
 * Keeping the raw sequence — rather than the composed text — is what lets the
 * engine re-derive the whole syllable on every keystroke, so a tone typed at
 * the end still lands on the correct vowel ("vieejt" -> "việt").
 */
public final class WordComposer {

    /** Letters that can continue a Vietnamese syllable. */
    private static final String WORD_CHARS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final StringBuilder raw = new StringBuilder();
    private String composed = "";
    private Mode mode = Mode.TELEX;

    public void setMode(Mode mode) {
        this.mode = mode;
        reset();
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isEmpty() {
        return raw.length() == 0;
    }

    public String getComposed() {
        return composed;
    }

    public void reset() {
        raw.setLength(0);
        composed = "";
    }

    public static boolean isWordChar(char ch) {
        return WORD_CHARS.indexOf(ch) >= 0;
    }

    /** Adds a key and returns the edit to apply to the text field. */
    public Edit push(char ch) {
        raw.append(ch);
        return recompute();
    }

    /** Removes the last raw key (backspace while composing). */
    public Edit pop() {
        if (raw.length() > 0) {
            raw.setLength(raw.length() - 1);
        }
        return recompute();
    }

    private Edit recompute() {
        String next = VietnameseEngine.transform(raw.toString(), mode);
        Edit edit = diff(composed, next);
        composed = next;
        return edit;
    }

    /**
     * Smallest "delete + insert" that turns {@code was} into {@code now}, so we
     * never redraw the whole syllable when only the last letter changed.
     */
    private static Edit diff(String was, String now) {
        int common = 0;
        int max = Math.min(was.length(), now.length());
        while (common < max && was.charAt(common) == now.charAt(common)) {
            common++;
        }
        return new Edit(was.length() - common, now.substring(common));
    }

    /** How many characters to delete before the cursor, and what to insert. */
    public static final class Edit {
        public final int deleteCount;
        public final String insertText;

        Edit(int deleteCount, String insertText) {
            this.deleteCount = deleteCount;
            this.insertText = insertText;
        }

        public boolean isNoop() {
            return deleteCount == 0 && insertText.isEmpty();
        }
    }
}
