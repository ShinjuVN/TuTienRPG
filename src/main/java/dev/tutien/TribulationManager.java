package dev.tutien;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

/** Tam Ma Kiep (ao anh) va Loi Kiep. */
public class TribulationManager {
    private static class State {
        boolean heart;
        BukkitTask task;
        Runnable onSuccess;
        final List<Zombie> illusions = new ArrayList<>();
        long deadline;
        int done, total;
        double pct;
    }

    private final TuTienPlugin pl;
    private final NamespacedKey illusionKey;
    private final Map<UUID, State> states = new HashMap<>();

    public TribulationManager(TuTienPlugin pl) {
        this.pl = pl;
        this.illusionKey = new NamespacedKey(pl, "illusion");
    }

    public NamespacedKey illusionKey() { return illusionKey; }

    public boolean busy(Player p) { return states.containsKey(p.getUniqueId()); }

    public Component status(Player p) {
        State s = states.get(p.getUniqueId());
        if (s == null) return null;
        if (s.heart) {
            long left = Math.max(0, (s.deadline - System.currentTimeMillis()) / 1000);
            return Msg.mm("<dark_purple>Tâm Ma Kiếp <white>" + s.illusions.size() + " <gray>ảo ảnh · <red>" + left + "s");
        }
        return Msg.mm("<yellow>⚡ Lôi Kiếp <white>" + s.done + "/" + s.total);
    }

    // ---------- Tam Ma ----------
    public void startHeartDemon(Player p, Runnable onSuccess) {
        if (busy(p) || p.isDead()) return;
        PlayerData d = pl.data().get(p);
        State s = new State();
        s.heart = true;
        s.onSuccess = onSuccess;
        s.deadline = System.currentTimeMillis() + pl.getConfig().getInt("heart-demon.time-seconds", 30) * 1000L;
        states.put(p.getUniqueId(), s);
        pl.cultivation().interrupt(p);

        double hp = pl.getConfig().getDouble("heart-demon.illusion-health", 12.0);
        int n = 3 + d.realm / 2;
        World w = p.getWorld();
        for (int i = 0; i < n; i++) {
            double ang = 2 * Math.PI * i / n;
            Location l = p.getLocation().add(Math.cos(ang) * 4, 0, Math.sin(ang) * 4);
            if (!l.getBlock().isPassable() || !l.clone().add(0, 1, 0).getBlock().isPassable()) l = p.getLocation();
            Zombie z = w.spawn(l, Zombie.class, zz -> {
                zz.setBaby(false);
                zz.customName(Msg.mm("<dark_purple>☠ Tâm Ma Ảo Ảnh"));
                zz.setCustomNameVisible(true);
                zz.setShouldBurnInDay(false);
                zz.setCanPickupItems(false);
                zz.setPersistent(false);
                zz.setGlowing(true);
                zz.getEquipment().clear();
                var mh = zz.getAttribute(Attribute.MAX_HEALTH);
                if (mh != null) mh.setBaseValue(hp);
                zz.setHealth(hp);
                zz.getPersistentDataContainer().set(illusionKey, PersistentDataType.STRING, p.getUniqueId().toString());
                zz.setTarget(p);
            });
            s.illusions.add(z);
        }
        p.showTitle(Title.title(Msg.mm("<dark_purple>TÂM MA KIẾP"), Msg.mm("<gray>Diệt sạch ảo ảnh trước khi đạo tâm sụp đổ!"),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500))));
        p.playSound(p.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.6f);

        s.task = org.bukkit.Bukkit.getScheduler().runTaskTimer(pl, () -> {
            if (!p.isOnline() || p.isDead()) { cancel(p); return; }
            s.illusions.removeIf(z -> z.isDead() || !z.isValid());
            pl.cultivation().interrupt(p);
            if (s.illusions.isEmpty()) { succeed(p); return; }
            if (System.currentTimeMillis() > s.deadline) { failHeart(p); return; }
            p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 80, 0, false, false, false));
            p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 80, 0, false, false, false));
            for (Zombie z : s.illusions) if (z.getTarget() == null) z.setTarget(p);
        }, 10L, 10L);
    }

    private void failHeart(Player p) {
        State s = end(p);
        if (s == null) return;
        applyPenalty(p, s);
        p.showTitle(Title.title(Msg.mm("<red>ĐẠO TÂM THẤT THỦ"), Msg.mm("<gray>Linh khí đan điền hao tổn..."),
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(3), Duration.ofMillis(500))));
        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_HURT, 1f, 0.6f);
    }

    // ---------- Loi Kiep ----------
    public void startLightning(Player p, int bolts, int nextRealm, Runnable onSuccess) {
        if (busy(p) || p.isDead()) return;
        State s = new State();
        s.total = bolts;
        s.onSuccess = onSuccess;
        s.pct = pl.getConfig().getDouble("lightning.damage-pct", 0.22) + 0.01 * nextRealm;
        states.put(p.getUniqueId(), s);
        pl.cultivation().interrupt(p);
        p.showTitle(Title.title(Msg.mm("<yellow>⚡ LÔI KIẾP ⚡"), Msg.mm("<gray>" + bolts + " đạo lôi kiếp sắp giáng xuống"),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500))));
        p.playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 0.5f);
        long interval = Math.max(20, pl.getConfig().getInt("lightning.interval-ticks", 160));

        s.task = org.bukkit.Bukkit.getScheduler().runTaskTimer(pl, () -> {
            if (!p.isOnline() || p.isDead()) { cancel(p); return; }
            PlayerData d = pl.data().get(p);
            Location l = p.getLocation();
            p.getWorld().strikeLightningEffect(l);
            p.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, l.clone().add(0, 1, 0), 60, 0.7, 1, 0.7, 0.2);
            Technique t = Technique.byId(d.selectedTech);
            double resist = t == null ? 0 : Math.min(0.8, t.leiResist);
            double dmg = pl.hp().maxHp(p) * s.pct * (1 - resist);
            s.done++;
            pl.hp().hurt(p, dmg, org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.LIGHTNING_BOLT).build()); // co the chet -> PlayerDeathEvent goi cancel()
            if (p.isDead() || !busy(p)) return;
            p.showTitle(Title.title(Component.empty(), Msg.mm("<yellow>Đạo lôi kiếp thứ " + s.done + "/" + s.total),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(300))));
            if (s.done >= s.total) succeed(p);
        }, 60L, interval);
    }

    // ---------- chung ----------
    private void succeed(Player p) {
        State s = end(p);
        if (s == null) return;
        if (s.heart) {
            p.showTitle(Title.title(Msg.mm("<green>PHÁ TÂM MA"), Msg.mm("<gray>Đạo tâm kiên định"),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500))));
        }
        if (s.onSuccess != null) s.onSuccess.run();
    }

    /** Huy kiep (chet / thoat game) -> bi phat. */
    public void cancel(Player p) {
        State s = end(p);
        if (s != null) applyPenalty(p, s);
    }

    private void applyPenalty(Player p, State s) {
        PlayerData d = pl.data().get(p);
        double loss = s.heart ? pl.getConfig().getDouble("heart-demon.fail-qi-loss", 0.25)
                : pl.getConfig().getDouble("lightning.fail-qi-loss", 0.5);
        d.qi = Math.max(0, d.qi * (1 - loss));
    }

    private State end(Player p) {
        State s = states.remove(p.getUniqueId());
        if (s == null) return null;
        if (s.task != null) s.task.cancel();
        for (Zombie z : s.illusions) if (z.isValid()) z.remove();
        if (p.isOnline()) {
            p.removePotionEffect(PotionEffectType.NAUSEA);
            p.removePotionEffect(PotionEffectType.DARKNESS);
        }
        return s;
    }

    public void shutdown() {
        for (UUID id : new ArrayList<>(states.keySet())) {
            Player p = org.bukkit.Bukkit.getPlayer(id);
            State s = states.remove(id);
            if (s == null) continue;
            if (s.task != null) s.task.cancel();
            for (Zombie z : s.illusions) if (z.isValid()) z.remove();
            if (p != null) applyPenalty(p, s);
        }
    }
}
