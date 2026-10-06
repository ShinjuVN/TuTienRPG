package dev.tutien;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import java.util.List;

/** Tao va nhan dien vat pham cua plugin (PersistentDataContainer). */
public final class Items {
    public static final String T_TECH = "technique", T_SKILL = "skill", T_PILL = "pill", T_TOOL = "tool";
    public static final String TRAC_LINH = "trac_linh_thach", CAN_CO = "can_co_ban";

    private static NamespacedKey TYPE, ID;

    private Items() {}

    public static void init(TuTienPlugin pl) {
        TYPE = new NamespacedKey(pl, "item_type");
        ID = new NamespacedKey(pl, "item_id");
    }

    private static ItemStack make(Material m, int amt, String type, String id, String name, List<String> lore) {
        ItemStack it = new ItemStack(m, Math.max(1, amt));
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Msg.item(name));
        meta.lore(lore.stream().map(Msg::item).toList());
        meta.getPersistentDataContainer().set(TYPE, PersistentDataType.STRING, type);
        meta.getPersistentDataContainer().set(ID, PersistentDataType.STRING, id);
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack technique(Technique t, int amt) {
        return make(Material.BOOK, amt, T_TECH, t.id, "<gold>" + t.display,
                List.of("<gray>Cấp độ: <yellow>" + t.tier, "<gray>Chuột phải để lĩnh ngộ công pháp."));
    }

    public static ItemStack skill(Skill s) {
        return make(Material.PAPER, 1, T_SKILL, s.id, "<aqua>" + s.display,
                List.of("<gray>Phẩm cấp: <yellow>" + s.tier, "<gray>Hệ: <white>" + s.element,
                        "<gray>Chuột phải để lĩnh ngộ võ kỹ."));
    }

    public static ItemStack pill(Pill p, int amt) {
        return make(p.material, amt, T_PILL, p.id, "<green>" + p.display,
                List.of("<gray>Hồi <red>" + (int) (p.healPct * 100) + "% HP<gray>.", "<gray>Chuột phải để dùng."));
    }

    public static ItemStack tool(String id) {
        if (TRAC_LINH.equals(id)) {
            return make(Material.AMETHYST_SHARD, 1, T_TOOL, id, "<light_purple>Trắc Linh Thạch",
                    List.of("<gray>Chuột phải (vào người khác hoặc không khí)", "<gray>để đo căn cốt và tu vi."));
        }
        if (CAN_CO.equals(id)) {
            return make(Material.COMPASS, 1, T_TOOL, id, "<gold>Căn Cơ Bàn",
                    List.of("<gray>Hiển thị đầy đủ căn cốt và tu vi.", "<gray>Chuột phải để dùng."));
        }
        return null;
    }

    /** Dung cho /tuluyen give. @return null neu type/id khong hop le */
    public static ItemStack byTypeId(String type, String id, int amt) {
        switch (type.toLowerCase()) {
            case "congphap" -> { Technique t = Technique.byId(id); return t == null ? null : technique(t, amt); }
            case "vo_ky", "voky" -> { Skill s = Skill.byId(id); if (s == null) return null; ItemStack i = skill(s); i.setAmount(amt); return i; }
            case "danduoc" -> { Pill p = Pill.byId(id); return p == null ? null : pill(p, amt); }
            case "daocu" -> { ItemStack i = tool(id.toLowerCase()); if (i != null) i.setAmount(amt); return i; }
            default -> { return null; }
        }
    }

    public static String type(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(TYPE, PersistentDataType.STRING);
    }

    public static String id(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(ID, PersistentDataType.STRING);
    }

    public static Component unused() { return Component.empty(); }
}
