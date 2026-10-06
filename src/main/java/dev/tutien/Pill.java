package dev.tutien;

import org.bukkit.Material;
import java.util.Arrays;

/** Dan duoc. minRealm/maxRealm tinh theo Dai Canh Gioi (1..10), maxRealm = canh gioi cao nhat con dung duoc. */
public enum Pill {
    HOI_NGUYEN("hoi_nguyen_dan", "Hồi Nguyên Đan", 0.10, 3, 5, Material.MAGMA_CREAM),
    LINH_DICH("linh_dich_su_song", "Linh Dịch Sự Sống", 0.30, 6, 7, Material.GHAST_TEAR),
    COI_NGUON("coi_nguon_sinh_thuy", "Cội Nguồn Sinh Thủy", 0.50, 9, 10, Material.NAUTILUS_SHELL);

    public final String id, display;
    public final double healPct;
    public final int minRealm, maxRealm;
    public final Material material;

    Pill(String id, String display, double healPct, int minRealm, int maxRealm, Material material) {
        this.id = id; this.display = display; this.healPct = healPct;
        this.minRealm = minRealm; this.maxRealm = maxRealm; this.material = material;
    }

    public static Pill byId(String id) {
        if (id == null) return null;
        return Arrays.stream(values()).filter(p -> p.id.equalsIgnoreCase(id)).findFirst().orElse(null);
    }
}
