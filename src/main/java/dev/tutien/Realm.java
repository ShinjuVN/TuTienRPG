package dev.tutien;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;

/** Dai Canh Gioi (0 = Pham Nhan, 1..10) va cac bang so lieu doc tu config. */
public final class Realm {
    public static final String[] NAMES = {"Phàm Nhân", "Luyện Khí", "Trúc Cơ", "Kim Đan", "Nguyên Anh",
            "Hóa Thần", "Luyện Hư", "Hợp Thể", "Đại Thừa", "Độ Kiếp", "Đại Đế"};
    public static final int MAX_REALM = 10, MAX_STAGE = 9;

    private Realm() {}

    public static String name(int realm) {
        return NAMES[Math.max(0, Math.min(MAX_REALM, realm))];
    }

    public static String full(int realm, int stage) {
        return realm <= 0 ? NAMES[0] : NAMES[Math.min(realm, MAX_REALM)] + " Tầng " + stage;
    }

    /** @return {realm, stage} ke tiep hoac null neu da toi dinh */
    public static int[] next(int realm, int stage) {
        if (realm <= 0) return new int[]{1, 1};
        if (stage < MAX_STAGE) return new int[]{realm, stage + 1};
        if (realm < MAX_REALM) return new int[]{realm + 1, 1};
        return null;
    }

    private static double at(List<Double> l, int i, double def) {
        if (l == null || l.isEmpty()) return def;
        return l.get(Math.max(0, Math.min(l.size() - 1, i)));
    }

    public static double baseHp(int realm) {
        FileConfiguration c = TuTienPlugin.get().getConfig();
        return at(c.getDoubleList("hp.base"), realm, 100);
    }

    public static double qiMax(int realm, int stage) {
        FileConfiguration c = TuTienPlugin.get().getConfig();
        double base = at(c.getDoubleList("qi.base"), realm, 100);
        double g = c.getDouble("qi.stage-growth", 0.15);
        return base * (1 + g * Math.max(0, stage - 1));
    }

    /** So dao loi kiep khi thang cap len (nr, ns). 0 = khong co. */
    public static int bolts(int nr, int ns, boolean crossing) {
        if (nr == 3 && ns == 1) return 3;                 // Truc Co 9 -> Kim Dan 1
        if (nr >= 4) return (crossing ? 3 : 1) + (nr - 4) / 2; // Nguyen Anh tro len: MOI lan thang cap
        return 0;
    }
}
