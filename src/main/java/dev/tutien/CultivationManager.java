package dev.tutien;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Tu luyen: giu Shift dung yen 10s = 1 chu thien. */
public class CultivationManager {
    private static final int STEP = 5; // tick moi lan quet
    private final TuTienPlugin pl;
    private final Random rng = new Random();

    public CultivationManager(TuTienPlugin pl) { this.pl = pl; }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(pl, this::tick, 20L, STEP);
        Bukkit.getScheduler().runTaskTimer(pl, this::mist, 40L, 20L);
    }

    // ---------- mat do linh khi ----------
    public double density(Location l) {
        FileConfiguration c = pl.getConfig();
        String w = l.getWorld().getName();
        double base = c.getDouble("density.world-base." + w, c.getDouble("density.default-base", 30000));
        long rx = Math.floorDiv(l.getBlockX(), 128), rz = Math.floorDiv(l.getBlockZ(), 128);
        long h = rx * 341873128712L + rz * 132897987541L + w.hashCode() * 31L;
        h ^= (h >>> 33); h *= 0xff51afd7ed558ccdL; h ^= (h >>> 33);
        double f = ((h >>> 11) & 0xFFFFFFL) / (double) 0x1000000;
        return Math.max(0, Math.min(100000, base * (0.2 + 1.6 * f)));
    }

    private void mist() {
        double th = pl.getConfig().getDouble("density.mist-threshold", 30000);
        for (Player p : Bukkit.getOnlinePlayers()) {
            double dens = density(p.getLocation());
            if (dens < th) continue;
            int count = (int) (6 + (dens - th) / Math.max(1, 100000 - th) * 30);
            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 0.3, 0), count, 5, 0.2, 5, 0.0);
        }
    }

    // ---------- vong lap chinh ----------
    private void tick() {
        int cycle = pl.getConfig().getInt("cultivation.cycle-ticks", 200);
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerData d = pl.data().get(p);
            boolean ok = p.isSneaking() && d.selectedTech != null && !p.isDead() && !p.isInsideVehicle()
                    && p.isOnGround() && p.getGameMode() != GameMode.SPECTATOR && !pl.trib().busy(p);
            if (!ok) { d.cycleTicks = 0; d.lastPos = null; continue; }
            Location l = p.getLocation();
            var pos = l.toVector();
            if (d.lastPos != null && d.lastPos.distanceSquared(pos) > 1.0E-6) d.cycleTicks = 0; // di chuyen -> huy
            d.lastPos = pos;
            d.cycleTicks += STEP;
            p.getWorld().spawnParticle(Particle.ENCHANT, l.clone().add(0, 1, 0), 6, 0.5, 0.8, 0.5, 0.4);
            if (d.cycleTicks >= cycle) complete(p, d);
        }
    }

    /** Nhan sat thuong / bi gian doan -> huy chu thien. */
    public void interrupt(Player p) {
        PlayerData d = pl.data().get(p);
        d.cycleTicks = 0;
        d.lastPos = null;
    }

    public Component status(Player p) {
        PlayerData d = pl.data().get(p);
        if (d.cycleTicks <= 0) return null;
        int cycle = pl.getConfig().getInt("cultivation.cycle-ticks", 200);
        int sec = d.cycleTicks / 20, total = cycle / 20;
        Technique t = Technique.byId(d.selectedTech);
        return Msg.mm("<gold>" + (t == null ? "" : t.display + " ") + "<yellow>" + sec + "/" + total + "s");
    }

    public void cycleTechnique(Player p) {
        PlayerData d = pl.data().get(p);
        List<String> list = new ArrayList<>(d.techniques.keySet());
        if (list.isEmpty()) { pl.hud().flash(p, "<red>Bạn chưa học công pháp nào", 2500); return; }
        int idx = list.indexOf(d.selectedTech);
        d.selectedTech = list.get((idx + 1) % list.size());
        d.cycleTicks = 0;
        Technique t = Technique.byId(d.selectedTech);
        pl.hud().flash(p, "<gold>Công pháp: <yellow>" + (t == null ? d.selectedTech : t.display), 3000);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
    }

    // ---------- hoan thanh chu thien ----------
    private void complete(Player p, PlayerData d) {
        FileConfiguration c = pl.getConfig();
        d.cycleTicks = 0;
        Technique t = Technique.byId(d.selectedTech);
        if (t == null) return;
        double dens = density(p.getLocation());
        double minF = c.getDouble("cultivation.min-mastery-factor", 0.2);
        double limit = t.cap() * (minF + (1 - minF) * d.masteryPct(t));
        double env = dens * c.getDouble("cultivation.density-to-qi", 0.05);
        double absorb = Math.min(limit, env) * Roots.root(d.linhCan).absorbMul();
        if (absorb < 1) { pl.hud().flash(p, "<gray>Linh khí nơi đây quá loãng...", 3000); return; }

        int gain = c.getInt("cultivation.mastery-per-cycle", 1);
        d.techniques.merge(t.id, gain, (a, b) -> Math.min(t.maxMastery(), a + b));

        int[] next = Realm.next(d.realm, d.stage);
        double max = Realm.qiMax(d.realm, d.stage);
        double excess = 0;
        if (next == null) {
            excess = absorb;
        } else {
            double need = max - d.qi;
            if (absorb <= need) d.qi += absorb;
            else { d.qi = max; excess = absorb - need; }
        }

        String msg = "<aqua>+" + fmt(absorb) + " linh khí";
        if (excess > 0) {
            double hmax = pl.hp().maxHp(p);
            double pct = p.getHealth() / hmax;
            if (pct >= 0.5 && pct <= 0.98) {
                double add = excess * c.getDouble("hp.qi-to-hp", 1.0);
                double before = p.getHealth();
                pl.hp().heal(p, add);
                msg += " <red>(+" + fmt(p.getHealth() - before) + " HP)";
            }
        }
        pl.hud().flash(p, msg + " <gray>(" + fmt(d.qi) + "/" + fmt(max) + ")", 3000);
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);

        if (next != null && d.qi >= max - 1e-6) {
            tryBreakthrough(p, d);
        } else if (d.realm >= 1 && rng.nextDouble() < c.getDouble("cultivation.heart-demon-chance", 0.03)) {
            pl.trib().startHeartDemon(p, null);
        }
    }

    private void tryBreakthrough(Player p, PlayerData d) {
        if (pl.trib().busy(p)) return;
        int[] n = Realm.next(d.realm, d.stage);
        if (n == null) return;
        boolean crossing = d.realm == 0 || d.stage == Realm.MAX_STAGE;
        int bolts = Realm.bolts(n[0], n[1], crossing);
        Runnable advance = () -> advance(p, n, crossing);
        Runnable afterDemon = bolts > 0 ? () -> pl.trib().startLightning(p, bolts, n[0], advance) : advance;
        if (crossing && d.realm >= 1) pl.trib().startHeartDemon(p, afterDemon);
        else afterDemon.run();
    }

    private void advance(Player p, int[] n, boolean crossing) {
        if (!p.isOnline() || p.isDead()) return;
        PlayerData d = pl.data().get(p);
        d.realm = n[0];
        d.stage = n[1];
        d.qi = 0;
        pl.hp().applyMax(p, false);
        pl.hp().fill(p);
        p.showTitle(Title.title(Msg.mm("<gold>ĐỘT PHÁ THÀNH CÔNG"), Msg.mm("<yellow>" + Realm.full(d.realm, d.stage)),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(800))));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.7f);
        p.getWorld().spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1, 0), 80, 0.6, 1, 0.6, 0.1);
        if (crossing) {
            Bukkit.broadcast(Msg.mm("<gold>✦ <yellow>" + p.getName() + " <gold>đã đột phá lên <white>" + Realm.name(d.realm) + "<gold>!"));
        }
    }

    public static String fmt(double v) {
        return String.format(java.util.Locale.US, "%,d", Math.round(v));
    }
}
