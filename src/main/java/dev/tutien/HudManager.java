package dev.tutien;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ve thanh HP bang Action Bar + font tutien:hud (resource pack).
 * Frame 102px (advance 103), phan ruot bat dau o x=3, rong toi da 96px.
 */
public class HudManager {
    private static final Key FONT = Key.key("tutien", "hud");
    private static final Key DEFAULT = Key.key("minecraft", "default");
    private static final int FRAME_ADV = 103, FILL_OFFSET = 3, FILL_MAX = 96;

    private record Flash(Component c, long until) {}

    private final TuTienPlugin pl;
    private final Set<UUID> packLoaded = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Flash> flashes = new ConcurrentHashMap<>();

    public HudManager(TuTienPlugin pl) { this.pl = pl; }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(pl, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isDead()) continue;
                p.sendActionBar(build(p));
            }
        }, 20L, 5L);
    }

    public void setPack(Player p, boolean loaded) {
        if (loaded) packLoaded.add(p.getUniqueId()); else packLoaded.remove(p.getUniqueId());
    }

    public void remove(Player p) {
        packLoaded.remove(p.getUniqueId());
        flashes.remove(p.getUniqueId());
    }

    public void flash(Player p, String mini, long ms) {
        flashes.put(p.getUniqueId(), new Flash(Msg.mm(mini), System.currentTimeMillis() + ms));
    }

    private boolean hasPack(Player p) {
        String url = pl.getConfig().getString("resource-pack.url", "");
        return url.isBlank() || packLoaded.contains(p.getUniqueId());
    }

    /** Chuoi ky tu dich con tro theo n pixel (am = lui). */
    static String shift(int n) {
        if (n == 0) return "";
        int base = n < 0 ? 0xE100 : 0xE110;
        int a = Math.abs(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            int v = 1 << i;
            while (a >= v) { sb.append((char) (base + i)); a -= v; }
        }
        return sb.toString();
    }

    private static int width(String s) {
        int w = 0;
        for (char c : s.toCharArray()) w += c == ',' ? 2 : c == ' ' ? 4 : 6;
        return w;
    }

    private Component status(Player p) {
        Flash f = flashes.get(p.getUniqueId());
        if (f != null) {
            if (System.currentTimeMillis() < f.until()) return f.c();
            flashes.remove(p.getUniqueId());
        }
        Component t = pl.trib().status(p);
        if (t != null) return t;
        return pl.cultivation().status(p);
    }

    public Component build(Player p) {
        double max = Math.max(1, pl.hp().maxHp(p));
        double cur = Math.max(0, Math.min(p.getHealth(), max));
        String nums = String.format(Locale.US, "%,d / %,d", (long) Math.ceil(cur), (long) max);
        Component status = status(p);
        Component out;

        if (hasPack(p)) {
            int f = (int) Math.round(FILL_MAX * cur / max);
            if (cur > 0 && f == 0) f = 1;
            StringBuilder sb = new StringBuilder();
            sb.append('\uE000').append(shift(FILL_OFFSET - FRAME_ADV));          // khung, lui ve x=3
            for (int k = 6; k >= 0; k--) {                                        // ruot do: nhi phan 64..1
                if ((f & (1 << k)) != 0) sb.append((char) (0xE001 + k)).append(shift(-1));
            }
            sb.append(shift(FRAME_ADV - FILL_OFFSET - f));                        // ve cuoi khung (x=103)
            int tw = width(nums);
            int x0 = (FRAME_ADV - tw) / 2;
            sb.append(shift(x0 - FRAME_ADV));                                     // lui ve giua de viet so
            Component bar = Component.text(sb.toString()).font(FONT).color(NamedTextColor.WHITE);
            Component digits = Component.text(nums).font(DEFAULT).color(NamedTextColor.WHITE);
            Component tail = Component.text(shift(FRAME_ADV - x0 - tw)).font(FONT).color(NamedTextColor.WHITE);
            out = Component.text().append(bar).append(digits).append(tail).build();
        } else {
            int bars = (int) Math.round(20 * cur / max);
            out = Msg.mm("<red>❤ <white>" + nums + " <dark_gray>[<red>" + "|".repeat(bars)
                    + "<gray>" + "|".repeat(20 - bars) + "<dark_gray>]");
        }
        if (status != null) {
            out = Component.text().append(out)
                    .append(Component.text(shift(10)).font(FONT)).append(status).build();
        }
        return out;
    }
}
