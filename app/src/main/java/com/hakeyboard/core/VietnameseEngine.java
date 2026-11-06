package com.hakeyboard.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java Vietnamese input engine (Telex + VNI). No Android dependencies,
 * so it can be unit-tested with plain javac/java.
 *
 * The engine is stateless: {@link #transform(String, Mode)} takes the whole
 * raw key sequence typed for the current syllable and rebuilds the composed
 * Vietnamese text from scratch. Replaying from the raw buffer on every key is
 * what makes "undo" behaviour fall out for free — typing a tone key twice
 * ("ass" -> "as") or a modifier twice ("aaa" -> "aa") just re-runs the rules.
 */
public final class VietnameseEngine {

    public enum Mode { TELEX, VNI }

    // Diacritic marks that live on the letter itself (not the tone).
    private static final int MARK_NONE = 0;
    private static final int MARK_CIRCUMFLEX = 1; // â ê ô
    private static final int MARK_BREVE = 2;      // ă
    private static final int MARK_HORN = 3;       // ơ ư
    private static final int MARK_STROKE = 4;     // đ

    // Tones.
    public static final int TONE_NONE = 0;
    public static final int TONE_ACUTE = 1; // sắc
    public static final int TONE_GRAVE = 2; // huyền
    public static final int TONE_HOOK = 3;  // hỏi
    public static final int TONE_TILDE = 4; // ngã
    public static final int TONE_DOT = 5;   // nặng

    /** base+mark -> the six forms, indexed by tone (0..5). */
    private static final Map<String, String> FORMS = new HashMap<>();

    static {
        FORMS.put("a0", "aáàảãạ"); // a á à ả ã ạ
        FORMS.put("a1", "âấầẩẫậ"); // â ấ ầ ẩ ẫ ậ
        FORMS.put("a2", "ăắằẳẵặ"); // ă ắ ằ ẳ ẵ ặ
        FORMS.put("e0", "eéèẻẽẹ"); // e é è ẻ ẽ ẹ
        FORMS.put("e1", "êếềểễệ"); // ê ế ề ể ễ ệ
        FORMS.put("i0", "iíìỉĩị"); // i í ì ỉ ĩ ị
        FORMS.put("o0", "oóòỏõọ"); // o ó ò ỏ õ ọ
        FORMS.put("o1", "ôốồổỗộ"); // ô ố ồ ổ ỗ ộ
        FORMS.put("o3", "ơớờởỡợ"); // ơ ớ ờ ở ỡ ợ
        FORMS.put("u0", "uúùủũụ"); // u ú ù ủ ũ ụ
        FORMS.put("u3", "ưứừửữự"); // ư ứ ừ ử ữ ự
        FORMS.put("y0", "yýỳỷỹỵ"); // y ý ỳ ỷ ỹ ỵ
        FORMS.put("d4", "đ"); // đ
    }

    private static final String VOWELS = "aeiouy";

    private VietnameseEngine() {
    }

    /** One letter of the syllable being composed. */
    private static final class Letter {
        char base;      // lowercase ASCII letter
        int mark;
        boolean upper;
        /** true when this letter was produced by a bare 'w' (Telex) typed with
         *  no vowel to attach to, so a second 'w' can turn "ư" back into "w". */
        boolean fromBareW;

        Letter(char base, int mark, boolean upper) {
            this.base = base;
            this.mark = mark;
            this.upper = upper;
        }

        boolean isVowel() {
            return VOWELS.indexOf(base) >= 0;
        }
    }

    /**
     * Rebuilds the composed Vietnamese text for a raw key sequence.
     *
     * @param raw  everything the user has typed for the current syllable
     * @param mode TELEX or VNI
     */
    public static String transform(String raw, Mode mode) {
        List<Letter> letters = new ArrayList<>();
        int[] tone = new int[] { TONE_NONE };

        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (mode == Mode.TELEX) {
                handleTelexKey(letters, tone, ch);
            } else {
                handleVniKey(letters, tone, ch);
            }
        }
        return render(letters, tone[0]);
    }

    /** True if this key could change the composition rather than just append. */
    public static boolean isComposingKey(char ch, Mode mode) {
        char lower = Character.toLowerCase(ch);
        if (mode == Mode.TELEX) {
            return "sfrxjzwaeoud".indexOf(lower) >= 0;
        }
        return lower >= '0' && lower <= '9';
    }

    // ---------------------------------------------------------------- Telex

    private static void handleTelexKey(List<Letter> letters, int[] tone, char ch) {
        char lower = Character.toLowerCase(ch);
        boolean upper = Character.isUpperCase(ch);

        int toneOfKey = telexTone(lower);
        if (toneOfKey > 0 && hasVowel(letters)) {
            if (tone[0] == toneOfKey) {
                tone[0] = TONE_NONE;       // same tone key twice -> undo
                letters.add(new Letter(lower, MARK_NONE, upper));
            } else {
                tone[0] = toneOfKey;
            }
            return;
        }

        if (lower == 'z' && hasVowel(letters)) {
            if (tone[0] != TONE_NONE) {
                tone[0] = TONE_NONE;
                return;
            }
            letters.add(new Letter(lower, MARK_NONE, upper));
            return;
        }

        if (lower == 'w') {
            applyW(letters, upper);
            return;
        }

        // Doubling: aa -> â, ee -> ê, oo -> ô, dd -> đ.
        if ("aeod".indexOf(lower) >= 0 && !letters.isEmpty()) {
            Letter last = letters.get(letters.size() - 1);
            int wanted = (lower == 'd') ? MARK_STROKE : MARK_CIRCUMFLEX;
            if (last.base == lower) {
                if (last.mark == MARK_NONE) {
                    last.mark = wanted;
                    return;
                }
                if (last.mark == wanted) {
                    last.mark = MARK_NONE;  // typed three times -> undo
                    letters.add(new Letter(lower, MARK_NONE, upper));
                    return;
                }
            }
        }

        letters.add(new Letter(lower, MARK_NONE, upper));
    }

    private static int telexTone(char lower) {
        switch (lower) {
            case 's': return TONE_ACUTE;
            case 'f': return TONE_GRAVE;
            case 'r': return TONE_HOOK;
            case 'x': return TONE_TILDE;
            case 'j': return TONE_DOT;
            default: return TONE_NONE;
        }
    }

    /**
     * Telex 'w': aw->ă, ow->ơ, uw->ư, bare w->ư, and the combined "uo"+w->ươ
     * that makes words like "được" / "mười" typeable the way people expect.
     */
    private static void applyW(List<Letter> letters, boolean upper) {
        // "uo" pair first, so "uow" marks both letters at once.
        int second = lastVowelIndex(letters, -1);
        if (second >= 0) {
            int first = lastVowelIndex(letters, second - 1);
            if (first >= 0 && first == second - 1) {
                Letter a = letters.get(first);
                Letter b = letters.get(second);
                boolean isUo = a.base == 'u' && b.base == 'o';
                if (isUo && !(a.mark == MARK_HORN && b.mark == MARK_HORN)) {
                    a.mark = MARK_HORN;
                    b.mark = MARK_HORN;
                    return;
                }
            }
        }

        int idx = lastWTargetIndex(letters);
        if (idx >= 0) {
            Letter target = letters.get(idx);
            int wanted = (target.base == 'a') ? MARK_BREVE : MARK_HORN;
            if (target.mark == wanted) {
                if (target.fromBareW) {
                    letters.remove(idx);            // "ww" -> literal w
                } else {
                    target.mark = MARK_NONE;        // undo the mark
                }
                letters.add(new Letter('w', MARK_NONE, upper));
            } else {
                target.mark = wanted;
            }
            return;
        }

        Letter bare = new Letter('u', MARK_HORN, upper);
        bare.fromBareW = true;
        letters.add(bare);
    }

    /** Last letter that 'w' can mark: a (breve) or o/u (horn). */
    private static int lastWTargetIndex(List<Letter> letters) {
        for (int i = letters.size() - 1; i >= 0; i--) {
            char b = letters.get(i).base;
            if (b == 'a' || b == 'o' || b == 'u') {
                return i;
            }
            if (letters.get(i).isVowel()) {
                return -1; // a vowel that 'w' cannot mark (e, i, y) blocks it
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ VNI

    private static void handleVniKey(List<Letter> letters, int[] tone, char ch) {
        if (ch < '0' || ch > '9') {
            letters.add(new Letter(Character.toLowerCase(ch), MARK_NONE, Character.isUpperCase(ch)));
            return;
        }

        switch (ch) {
            case '1': case '2': case '3': case '4': case '5': {
                int t = ch - '0';
                if (!hasVowel(letters)) {
                    letters.add(new Letter(ch, MARK_NONE, false));
                    return;
                }
                if (tone[0] == t) {
                    tone[0] = TONE_NONE;
                    letters.add(new Letter(ch, MARK_NONE, false));
                } else {
                    tone[0] = t;
                }
                return;
            }
            case '0': {
                if (tone[0] != TONE_NONE) {
                    tone[0] = TONE_NONE;
                } else {
                    letters.add(new Letter(ch, MARK_NONE, false));
                }
                return;
            }
            case '6': applyVniMark(letters, ch, MARK_CIRCUMFLEX, "aeo"); return;
            case '7': applyVniHorn(letters, ch); return;
            case '8': applyVniMark(letters, ch, MARK_BREVE, "a"); return;
            case '9': applyVniMark(letters, ch, MARK_STROKE, "d"); return;
            default:
                letters.add(new Letter(ch, MARK_NONE, false));
        }
    }

    private static void applyVniMark(List<Letter> letters, char digit, int wanted, String bases) {
        for (int i = letters.size() - 1; i >= 0; i--) {
            Letter l = letters.get(i);
            if (bases.indexOf(l.base) >= 0) {
                if (l.mark == wanted) {
                    l.mark = MARK_NONE;
                    letters.add(new Letter(digit, MARK_NONE, false));
                } else {
                    l.mark = wanted;
                }
                return;
            }
        }
        letters.add(new Letter(digit, MARK_NONE, false));
    }

    /** VNI '7': o->ơ, u->ư, and "uo"+7 -> ươ. */
    private static void applyVniHorn(List<Letter> letters, char digit) {
        int second = lastVowelIndex(letters, -1);
        if (second >= 0) {
            int first = lastVowelIndex(letters, second - 1);
            if (first >= 0 && first == second - 1) {
                Letter a = letters.get(first);
                Letter b = letters.get(second);
                if (a.base == 'u' && b.base == 'o' && !(a.mark == MARK_HORN && b.mark == MARK_HORN)) {
                    a.mark = MARK_HORN;
                    b.mark = MARK_HORN;
                    return;
                }
            }
        }
        applyVniMark(letters, digit, MARK_HORN, "ou");
    }

    // -------------------------------------------------------------- helpers

    private static boolean hasVowel(List<Letter> letters) {
        for (Letter l : letters) {
            if (l.isVowel()) {
                return true;
            }
        }
        return false;
    }

    /** Index of the last vowel at or before {@code from} (-1 means "from the end"). */
    private static int lastVowelIndex(List<Letter> letters, int from) {
        int start = (from < 0) ? letters.size() - 1 : from;
        for (int i = start; i >= 0; i--) {
            if (letters.get(i).isVowel()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Decides which letter carries the tone mark, following standard Vietnamese
     * orthography (old/traditional placement style: hòa, thủy, khỏe).
     */
    private static int toneTargetIndex(List<Letter> letters) {
        List<Integer> vowels = new ArrayList<>();
        for (int i = 0; i < letters.size(); i++) {
            if (letters.get(i).isVowel()) {
                vowels.add(i);
            }
        }
        if (vowels.isEmpty()) {
            return -1;
        }

        // "qu" and "gi" are onsets: their u/i is not the nucleus, as long as
        // another vowel follows ("quả", "già" vs "gì" where i IS the nucleus).
        List<Integer> nucleus = new ArrayList<>(vowels);
        if (vowels.size() > 1) {
            int first = vowels.get(0);
            Letter fl = letters.get(first);
            if (first > 0) {
                char prev = letters.get(first - 1).base;
                if ((prev == 'q' && fl.base == 'u') || (prev == 'g' && fl.base == 'i')) {
                    nucleus.remove(0);
                }
            }
        }
        if (nucleus.isEmpty()) {
            nucleus = vowels;
        }

        // A letter that already carries a diacritic wins; with two (ươ) the
        // second one takes the tone: được, mười, rượu.
        int marked = -1;
        for (int idx : nucleus) {
            if (letters.get(idx).mark != MARK_NONE) {
                marked = idx;
            }
        }
        if (marked >= 0) {
            return marked;
        }

        if (nucleus.size() == 1) {
            return nucleus.get(0);
        }
        if (nucleus.size() >= 3) {
            return nucleus.get(1);
        }

        // Two bare vowels: closed syllable -> second, open syllable -> first.
        int last = nucleus.get(nucleus.size() - 1);
        boolean closed = last < letters.size() - 1;
        return closed ? nucleus.get(1) : nucleus.get(0);
    }

    private static String render(List<Letter> letters, int tone) {
        int toneIdx = (tone == TONE_NONE) ? -1 : toneTargetIndex(letters);
        StringBuilder sb = new StringBuilder(letters.size());
        for (int i = 0; i < letters.size(); i++) {
            Letter l = letters.get(i);
            int applied = (i == toneIdx) ? tone : TONE_NONE;
            sb.append(renderLetter(l, applied));
        }
        return sb.toString();
    }

    private static char renderLetter(Letter l, int tone) {
        String forms = FORMS.get("" + l.base + l.mark);
        char out;
        if (forms == null) {
            out = l.base;                       // plain consonant
        } else if (tone < forms.length()) {
            out = forms.charAt(tone);
        } else {
            out = forms.charAt(0);
        }
        return l.upper ? Character.toUpperCase(out) : out;
    }
}
