import com.hakeyboard.core.VietnameseEngine;
import com.hakeyboard.core.VietnameseEngine.Mode;

/** Plain-java test runner: javac + java, no Android, no JUnit. */
public class EngineTest {

    private static int pass = 0;
    private static int fail = 0;

    private static void check(String raw, String expected, Mode mode) {
        String got = VietnameseEngine.transform(raw, mode);
        if (got.equals(expected)) {
            pass++;
        } else {
            fail++;
            System.out.println("FAIL [" + mode + "] \"" + raw + "\" -> \"" + got
                    + "\"  (expected \"" + expected + "\")");
        }
    }

    private static void telex(String raw, String expected) { check(raw, expected, Mode.TELEX); }
    private static void vni(String raw, String expected) { check(raw, expected, Mode.VNI); }

    public static void main(String[] args) {
        // --- Telex: core words -------------------------------------------
        telex("vieejt", "việt");
        telex("tieengs", "tiếng");
        telex("Vieejt", "Việt");
        telex("dduowcj", "được");
        telex("dduwowcj", "được");
        telex("nguyeenx", "nguyễn");
        telex("hoaf", "hòa");
        telex("xoaif", "xoài");
        telex("thuyr", "thủy");
        telex("quys", "quý");
        telex("quar", "quả");
        telex("giaf", "già");
        telex("gif", "gì");
        telex("cuar", "của");
        telex("khoer", "khỏe");
        telex("khoais", "khoái");
        telex("tuaanf", "tuần");
        telex("cuoocj", "cuộc");
        telex("muowif", "mười");
        telex("ruwowuj", "rượu");
        telex("toans", "toán");
        telex("myx", "mỹ");
        telex("chaof", "chào");
        telex("nhaf", "nhà");
        telex("ddi", "đi");
        telex("awn", "ăn");
        telex("sawps", "sắp");
        telex("ngayf", "ngày");
        telex("hocj", "học");
        telex("hoocj", "hộc");
        telex("truwowngf", "trường");
        telex("nguowif", "người");

        // --- Telex: undo / literal behaviour ------------------------------
        telex("ass", "as");
        telex("aaa", "aa");
        telex("w", "ư");
        telex("ww", "w");
        telex("as", "á");
        telex("asz", "a");
        telex("sa", "sa");
        telex("fan", "fan");
        telex("tra", "tra");

        // --- VNI ----------------------------------------------------------
        vni("vie6t5", "việt");
        vni("tie6ng1", "tiếng");
        vni("d9u7o7c5", "được");
        vni("d9uo7c5", "được");
        vni("nguye6n4", "nguyễn");
        vni("hoa2", "hòa");
        vni("xoai2", "xoài");
        vni("thuy3", "thủy");
        vni("quy1", "quý");
        vni("qua3", "quả");
        vni("cua3", "của");
        vni("cuo6c5", "cuộc");
        vni("muo7i2", "mười");
        vni("toan1", "toán");
        vni("a1", "á");
        vni("a11", "a1");
        vni("a10", "a");
        vni("d9i", "đi");
        vni("a8n", "ăn");

        System.out.println();
        System.out.println("PASS " + pass + " / " + (pass + fail));
        if (fail > 0) {
            System.out.println("FAILED: " + fail);
            System.exit(1);
        }
        System.out.println("All tests passed.");
    }
}
