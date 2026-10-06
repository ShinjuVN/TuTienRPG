package dev.tutien;

import java.util.Arrays;

/** 4 cap do cong phap. */
public enum Technique {
    HAP_TINH("hap_tinh_phap", "Hấp Tinh Pháp", "Phàm", 150, 100, 0.0),
    QUY_NGUYEN("quy_nguyen_quyet", "Quy Nguyên Quyết", "Sơ", 500, 300, 0.0),
    PHE_LOI("phe_loi_than_cong", "Phệ Lôi Thần Công", "Trung", 1500, 800, 0.25),
    HON_DON("hon_don_thon_thien_kinh", "Hỗn Độn Thôn Thiên Kinh", "Đỉnh", 5000, 2000, 0.15);

    public final String id, display, tier;
    private final double cap;
    private final int maxMastery;
    public final double leiResist;

    Technique(String id, String display, String tier, double cap, int maxMastery, double leiResist) {
        this.id = id; this.display = display; this.tier = tier;
        this.cap = cap; this.maxMastery = maxMastery; this.leiResist = leiResist;
    }

    public double cap() {
        return TuTienPlugin.get().getConfig().getDouble("techniques." + id + ".cap", cap);
    }

    public int maxMastery() {
        return Math.max(1, TuTienPlugin.get().getConfig().getInt("techniques." + id + ".max-mastery", maxMastery));
    }

    public static Technique byId(String id) {
        if (id == null) return null;
        return Arrays.stream(values()).filter(t -> t.id.equalsIgnoreCase(id)).findFirst().orElse(null);
    }
}
