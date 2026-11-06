package com.hakeyboard.ime;

/**
 * Static key layouts. Each row is an array of {@link Key}; the service turns
 * them into views, so changing the keyboard shape never touches drawing code.
 */
public final class KeyboardLayouts {

    /** What a key does when tapped. */
    public enum Action { CHAR, SHIFT, DELETE, SPACE, ENTER, SYMBOLS, LETTERS, MORE_SYMBOLS, LANGUAGE, EMOJI }

    public static final class Key {
        public final String label;
        /** Characters shown on long-press; null when there are none. */
        public final String longPress;
        public final Action action;
        public final float weight;
        public final int style; // 0 = normal, 1 = special, 2 = accent

        public Key(String label, String longPress, Action action, float weight, int style) {
            this.label = label;
            this.longPress = longPress;
            this.action = action;
            this.weight = weight;
            this.style = style;
        }

        public static Key ch(String label, String longPress) {
            return new Key(label, longPress, Action.CHAR, 1f, 0);
        }

        public static Key ch(String label) {
            return new Key(label, null, Action.CHAR, 1f, 0);
        }
    }

    public static final Key[] NUMBER_ROW = {
            Key.ch("1"), Key.ch("2"), Key.ch("3"), Key.ch("4"), Key.ch("5"),
            Key.ch("6"), Key.ch("7"), Key.ch("8"), Key.ch("9"), Key.ch("0")
    };

    public static final Key[][] LETTERS = {
            {
                    Key.ch("q", "1"), Key.ch("w", "2"), Key.ch("e", "3ê"), Key.ch("r", "4"),
                    Key.ch("t", "5"), Key.ch("y", "6"), Key.ch("u", "7ư"), Key.ch("i", "8"),
                    Key.ch("o", "9ôơ"), Key.ch("p", "0")
            },
            {
                    Key.ch("a", "âă"), Key.ch("s"), Key.ch("d", "đ"), Key.ch("f"), Key.ch("g"),
                    Key.ch("h"), Key.ch("j"), Key.ch("k"), Key.ch("l")
            },
            {
                    new Key("⇧", null, Action.SHIFT, 1.5f, 1),
                    Key.ch("z"), Key.ch("x"), Key.ch("c"), Key.ch("v"),
                    Key.ch("b"), Key.ch("n"), Key.ch("m"),
                    new Key("⌫", null, Action.DELETE, 1.5f, 1)
            },
            {
                    new Key("?123", null, Action.SYMBOLS, 1.5f, 1),
                    new Key("VI", null, Action.LANGUAGE, 1.2f, 1),
                    Key.ch(",", ":;\""),
                    new Key(" ", null, Action.SPACE, 4f, 0),
                    Key.ch(".", "?!…"),
                    new Key("⏎", null, Action.ENTER, 1.7f, 2)
            }
    };

    public static final Key[][] SYMBOLS = {
            {
                    Key.ch("1"), Key.ch("2"), Key.ch("3"), Key.ch("4"), Key.ch("5"),
                    Key.ch("6"), Key.ch("7"), Key.ch("8"), Key.ch("9"), Key.ch("0")
            },
            {
                    Key.ch("@"), Key.ch("#"), Key.ch("đ"), Key.ch("_"), Key.ch("&"),
                    Key.ch("-"), Key.ch("+"), Key.ch("("), Key.ch(")"), Key.ch("/")
            },
            {
                    new Key("=\\<", null, Action.MORE_SYMBOLS, 1.5f, 1),
                    Key.ch("*"), Key.ch("\""), Key.ch("'"), Key.ch(":"),
                    Key.ch(";"), Key.ch("!"), Key.ch("?"),
                    new Key("⌫", null, Action.DELETE, 1.5f, 1)
            },
            {
                    new Key("ABC", null, Action.LETTERS, 1.5f, 1),
                    Key.ch(","),
                    new Key(" ", null, Action.SPACE, 5f, 0),
                    Key.ch("."),
                    new Key("⏎", null, Action.ENTER, 1.7f, 2)
            }
    };

    public static final Key[][] MORE_SYMBOLS = {
            {
                    Key.ch("~"), Key.ch("`"), Key.ch("|"), Key.ch("•"), Key.ch("√"),
                    Key.ch("π"), Key.ch("÷"), Key.ch("×"), Key.ch("¶"), Key.ch("∆")
            },
            {
                    Key.ch("£"), Key.ch("¢"), Key.ch("€"), Key.ch("¥"), Key.ch("^"),
                    Key.ch("°"), Key.ch("="), Key.ch("{"), Key.ch("}"), Key.ch("\\")
            },
            {
                    new Key("?123", null, Action.SYMBOLS, 1.5f, 1),
                    Key.ch("%"), Key.ch("©"), Key.ch("®"), Key.ch("™"),
                    Key.ch("["), Key.ch("]"), Key.ch("<"),
                    new Key("⌫", null, Action.DELETE, 1.5f, 1)
            },
            {
                    new Key("ABC", null, Action.LETTERS, 1.5f, 1),
                    Key.ch(","),
                    new Key(" ", null, Action.SPACE, 5f, 0),
                    Key.ch("."),
                    new Key("⏎", null, Action.ENTER, 1.7f, 2)
            }
    };

    private KeyboardLayouts() {
    }
}
