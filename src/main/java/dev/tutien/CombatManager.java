package dev.tutien;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

/** Dash (Ctrl + WASD) va Vo Ky (Ctrl + Chuot phai). Moi thu deu do server quyet dinh. */
public class CombatManager {
    private final TuTienPlugin pl;
    private final Map<UUID, Long> dashCd = new HashMap<>();
    private final Map<String, Long> skillCd = new HashMap<>();

    public CombatManager(TuTienPlugin pl) { this.pl = pl; }

    public void forget(Player p) {
        dashCd.remove(p.getUniqueId());
        skillCd.keySet().removeIf(k -> k.startsWith(p.getUniqueId().toString()));
    }

    // ================= DASH =================
    /** Luot bang van toc that (client tu du doan nen muot, khong teleport). Tat bang dash.enabled: false. */
    public void dash(Player p, boolean f, boolean b, boolean l, boolean r) {
        if (!pl.getConfig().getBoolean("dash.enabled", false)) return;
        long now = System.currentTimeMillis();
        Long nxt = dashCd.get(p.getUniqueId());
        if (nxt != null && now < nxt) return;
        PlayerData d = pl.data().get(p);
        if (p.isDead() || p.isInsideVehicle() || p.isFlying() || p.isGliding() || p.isSwimming() || !p.isOnGround()
                || p.getGameMode() == GameMode.SPECTATOR || pl.trib().busy(p) || d.cycleTicks > 0) return;

        double yaw = Math.toRadians(p.getLocation().getYaw());
        Vector fwd = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        Vector right = new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
        Vector dir = new Vector();
        if (f) dir.add(fwd);
        if (b) dir.subtract(fwd);
        if (r) dir.add(right);
        if (l) dir.subtract(right);
        if (dir.lengthSquared() < 1.0E-6) return;
        dir.normalize();

        double dist = Math.max(1, Math.min(6, pl.getConfig().getInt("dash.distance", 3)));
        // Cat ngan neu co vat can phia truoc (kiem tra o do cao chan va dau)
        World w = p.getWorld();
        Location base = p.getLocation();
        for (double h : new double[]{0.3, 1.5}) {
            RayTraceResult hit = w.rayTraceBlocks(base.clone().add(0, h, 0), dir, dist + 0.5, FluidCollisionMode.NEVER, true);
            if (hit != null && hit.getHitPosition() != null) {
                double hd = hit.getHitPosition().distance(base.clone().add(0, h, 0).toVector()) - 0.5;
                dist = Math.min(dist, hd);
            }
        }
        if (dist < 0.8) return;
        Location end = base.clone().add(dir.clone().multiply(dist));
        if (!safe(w, end)) return;

        dashCd.put(p.getUniqueId(), now + pl.getConfig().getLong("dash.cooldown-ms", 1500));
        // Ma sat dat ~0.546/tick => quang duong ~ v0 / 0.454
        p.setVelocity(dir.multiply(dist * 0.454));
        w.spawnParticle(Particle.CLOUD, base.add(0, 0.2, 0), 8, 0.25, 0.1, 0.25, 0.01);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.5f);
    }

    private boolean safe(World w, Location to) {
        if (!w.getWorldBorder().isInside(to)) return false;
        if (!w.isChunkLoaded(to.getBlockX() >> 4, to.getBlockZ() >> 4)) return false;
        Material m = to.getBlock().getType();
        if (m == Material.LAVA || m == Material.FIRE || m == Material.SOUL_FIRE || m == Material.MAGMA_BLOCK
                || m == Material.CACTUS || m == Material.SWEET_BERRY_BUSH) return false;
        Material below = to.clone().add(0, -0.5, 0).getBlock().getType();
        return below != Material.LAVA && below != Material.MAGMA_BLOCK;
    }

    // ================= VO KY =================
    public void cycleSkill(Player p) {
        PlayerData d = pl.data().get(p);
        List<String> list = new ArrayList<>(d.skills);
        if (list.isEmpty()) { pl.hud().flash(p, "<red>Bạn chưa học võ kỹ nào", 2500); return; }
        int idx = list.indexOf(d.selectedSkill);
        d.selectedSkill = list.get((idx + 1) % list.size());
        Skill s = Skill.byId(d.selectedSkill);
        pl.hud().flash(p, "<aqua>Võ kỹ: <white>" + (s == null ? d.selectedSkill : s.display + " <gray>(" + s.tier + ")"), 3000);
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.8f);
    }

    public void cast(Player p) {
        PlayerData d = pl.data().get(p);
        Skill s = Skill.byId(d.selectedSkill);
        if (s == null || !d.skills.contains(s.id)) { pl.hud().flash(p, "<red>Chưa chọn võ kỹ (Ctrl + F)", 2500); return; }
        if (d.realm < s.minRealm) {
            pl.hud().flash(p, "<red>Cảnh giới chưa đủ để thi triển <white>" + s.display, 2500);
            return;
        }
        if (pl.trib().busy(p) || p.isDead()) return;
        String key = p.getUniqueId() + ":" + s.id;
        long now = System.currentTimeMillis();
        Long nxt = skillCd.get(key);
        if (nxt != null && now < nxt) {
            pl.hud().flash(p, "<gray>" + s.display + " hồi chiêu: <white>" + String.format("%.1f", (nxt - now) / 1000.0) + "s", 1200);
            return;
        }
        skillCd.put(key, now + s.cooldownMs);
        pl.cultivation().interrupt(p);
        pl.hud().flash(p, "<aqua>✦ " + s.display, 1500);

        Location eye = p.getEyeLocation();
        Vector dir = eye.getDirection().normalize();
        switch (s.shape) {
            case LINE -> line(p, s, eye, dir);
            case BARRAGE -> new BukkitRunnable() {
                int n = 0;
                @Override public void run() {
                    if (!p.isOnline() || p.isDead() || ++n > 5) { cancel(); return; }
                    Location e = p.getEyeLocation();
                    line(p, s, e, e.getDirection().normalize());
                }
            }.runTaskTimer(pl, 0L, 4L);
            case SELF_AOE -> aoe(p, s, p.getLocation(), true);
            case TARGET_AOE -> aoe(p, s, target(p, eye, dir, s.range), false);
        }
    }

    private Location target(Player p, Location eye, Vector dir, double range) {
        RayTraceResult r = p.getWorld().rayTraceBlocks(eye, dir, range, FluidCollisionMode.NEVER, true);
        if (r != null && r.getHitPosition() != null) return r.getHitPosition().toLocation(p.getWorld());
        return eye.clone().add(dir.clone().multiply(range));
    }

    private boolean valid(Player caster, Entity e) {
        if (!(e instanceof LivingEntity) || e == caster || e instanceof ArmorStand || e.isDead()) return false;
        if (e instanceof Tameable t && t.isTamed() && caster.equals(t.getOwner())) return false;
        if (e instanceof Player tp) return tp.getGameMode() != GameMode.CREATIVE && tp.getGameMode() != GameMode.SPECTATOR;
        return true;
    }

    private void line(Player p, Skill s, Location eye, Vector dir) {
        World w = p.getWorld();
        double reach = s.range;
        for (double t = 0; t <= s.range; t += 0.6) {
            Location pt = eye.clone().add(dir.clone().multiply(t));
            if (pt.getBlock().getType().isSolid()) { reach = t; break; }
            w.spawnParticle(s.particle, pt, 3, 0.15, 0.15, 0.15, 0.0);
        }
        w.playSound(eye, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.8f);
        double rr = s.range + 2;
        for (Entity e : w.getNearbyEntities(eye, rr, rr, rr)) {
            if (!valid(p, e)) continue;
            LivingEntity le = (LivingEntity) e;
            Vector v = le.getLocation().add(0, le.getHeight() / 2, 0).toVector().subtract(eye.toVector());
            double t = v.dot(dir);
            if (t < 0 || t > reach) continue;
            double perp = v.clone().subtract(dir.clone().multiply(t)).length();
            if (perp <= s.radius + le.getWidth() / 2) {
                hit(p, le, s.damage);
                if (s.element.equals("Hỏa")) le.setFireTicks(100);
            }
        }
    }

    private void aoe(Player p, Skill s, Location c, boolean knock) {
        World w = c.getWorld();
        for (int i = 0; i < 48; i++) {
            double a = 2 * Math.PI * i / 48;
            Location ring = c.clone().add(Math.cos(a) * s.radius, 0.2, Math.sin(a) * s.radius);
            w.spawnParticle(s.particle, ring, 2, 0.1, 0.1, 0.1, 0.0);
        }
        w.spawnParticle(s.particle, c.clone().add(0, 1, 0), 60, s.radius / 2, 1.2, s.radius / 2, 0.05);
        if (s == Skill.THIEN_LOI_PHAN) w.strikeLightningEffect(c);
        if (s == Skill.DE_HU_KHONG) w.spawnParticle(Particle.EXPLOSION_EMITTER, c, 3, 1.5, 0.5, 1.5, 0);
        w.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.2f);
        double r = s.radius;
        for (Entity e : w.getNearbyEntities(c, r, r, r)) {
            if (!valid(p, e)) continue;
            LivingEntity le = (LivingEntity) e;
            if (le.getLocation().distance(c) > r) continue;
            hit(p, le, s.damage);
            if (knock) {
                Vector away = le.getLocation().toVector().subtract(c.toVector()).setY(0);
                if (away.lengthSquared() > 1.0E-6) away.normalize().multiply(0.9);
                le.setVelocity(away.setY(0.45));
            }
            if (s == Skill.TRUNG_MOC_PHONG) le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
        }
    }

    private void hit(Player caster, LivingEntity le, double dmg) {
        org.bukkit.damage.DamageSource src = org.bukkit.damage.DamageSource.builder(org.bukkit.damage.DamageType.PLAYER_ATTACK)
                .withCausingEntity(caster).withDirectEntity(caster).build();
        double real = dmg * pl.hp().baseHit(caster); // dmg = so don can ban cua vo ky
        pl.hp().hurtEntity(le, real, src);
    }
}
