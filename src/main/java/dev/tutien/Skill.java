package dev.tutien;

import org.bukkit.Particle;
import java.util.Arrays;

/** Vo Ky: Ha Pham 3, Trung Pham 2, Thuong Pham 2, Thien Giai 1 (Loi), Thanh Cap 1 (Hoa), Cap De 1 (Khong he). */
public enum Skill {
    HA_KIEM_KHI("ha_kiem_khi", "Kiếm Khí Trảm", "Hạ Phẩm", "Kim", 1, Shape.LINE, 2.5, 8, 1.2, 3000, Particle.SWEEP_ATTACK),
    HA_PHONG_CHUONG("ha_phong_chuong", "Phong Chưởng", "Hạ Phẩm", "Phong", 1, Shape.LINE, 2.0, 6, 2.0, 3000, Particle.CLOUD),
    HA_DIA_CHAN("ha_dia_chan", "Địa Chấn Quyền", "Hạ Phẩm", "Thổ", 1, Shape.SELF_AOE, 2.5, 0, 4.0, 5000, Particle.CRIT),
    TRUNG_BANG_TIEN("trung_bang_tien", "Băng Tiễn Liên Châu", "Trung Phẩm", "Băng", 2, Shape.BARRAGE, 1.2, 14, 1.2, 8000, Particle.SNOWFLAKE),
    TRUNG_MOC_PHONG("trung_moc_phong", "Mộc Linh Phong Ấn", "Trung Phẩm", "Mộc", 2, Shape.TARGET_AOE, 5.0, 18, 4.0, 8000, Particle.HAPPY_VILLAGER),
    THUONG_CUONG_PHONG("thuong_cuong_phong", "Cuồng Phong Trảm", "Thượng Phẩm", "Phong", 3, Shape.SELF_AOE, 7.0, 0, 6.0, 12000, Particle.CLOUD),
    THUONG_VAN_KIEM("thuong_van_kiem", "Vạn Kiếm Quy Tông", "Thượng Phẩm", "Kim", 3, Shape.TARGET_AOE, 9.0, 20, 6.0, 12000, Particle.SWEEP_ATTACK),
    THIEN_LOI_PHAN("thien_loi_phan", "Lôi Đế Phán Quyết", "Thiên Giai", "Lôi", 4, Shape.TARGET_AOE, 8.0, 25, 7.0, 20000, Particle.ELECTRIC_SPARK),
    THANH_HOA_PHUNG("thanh_hoa_phung", "Hỏa Phượng Phần Thiên", "Thánh Cấp", "Hỏa", 7, Shape.LINE, 11.0, 25, 3.5, 30000, Particle.FLAME),
    DE_HU_KHONG("de_hu_khong", "Hư Không Diệt Thế", "Cấp Đế", "Không hệ", 9, Shape.TARGET_AOE, 14.0, 30, 12.0, 60000, Particle.PORTAL);

    public enum Shape { LINE, SELF_AOE, TARGET_AOE, BARRAGE }

    public final String id, display, tier, element;
    public final int minRealm;
    public final Shape shape;
    public final double damage, range, radius; // damage = so "don can ban" (nhan voi sat thuong dau ra cua nguoi choi)
    public final long cooldownMs;
    public final Particle particle;

    Skill(String id, String display, String tier, String element, int minRealm, Shape shape,
          double damage, double range, double radius, long cooldownMs, Particle particle) {
        this.id = id; this.display = display; this.tier = tier; this.element = element; this.minRealm = minRealm;
        this.shape = shape; this.damage = damage; this.range = range; this.radius = radius;
        this.cooldownMs = cooldownMs; this.particle = particle;
    }

    public static Skill byId(String id) {
        if (id == null) return null;
        return Arrays.stream(values()).filter(s -> s.id.equalsIgnoreCase(id)).findFirst().orElse(null);
    }
}
