package dev.tutien;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Input;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Listeners implements Listener {
    private static class InputState { boolean dashKey; boolean combo; int bits; }

    private final TuTienPlugin pl;
    private final Map<UUID, InputState> inputs = new HashMap<>();
    private final Map<UUID, Long> toolCd = new HashMap<>();
    private final Map<UUID, Long> pillCd = new HashMap<>();

    public Listeners(TuTienPlugin pl) { this.pl = pl; }

    // ---------------- ket noi ----------------
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        pl.data().get(p);
        pl.hp().onJoin(p);
        Bukkit.getScheduler().runTaskLater(pl, () -> {
            if (!p.isOnline()) return;
            var c = pl.getConfig();
            String url = c.getString("resource-pack.url", "");
            if (!url.isBlank()) {
                p.setResourcePack(url, hexToBytes(c.getString("resource-pack.sha1", "")),
                        Msg.mm(c.getString("resource-pack.prompt", "")), c.getBoolean("resource-pack.force", true));
            }
        }, 20L);
    }

    private static byte[] hexToBytes(String h) {
        h = h.trim();
        if (h.length() != 40) return null;
        byte[] b = new byte[20];
        for (int i = 0; i < 20; i++) b[i] = (byte) Integer.parseInt(h.substring(i * 2, i * 2 + 2), 16);
        return b;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        pl.trib().cancel(p);
        pl.cultivation().interrupt(p);
        pl.data().unload(p);
        pl.hud().remove(p);
        pl.combat().forget(p);
        inputs.remove(p.getUniqueId());
    }

    @EventHandler
    public void onPack(PlayerResourcePackStatusEvent e) {
        switch (e.getStatus()) {
            case SUCCESSFULLY_LOADED -> pl.hud().setPack(e.getPlayer(), true);
            case DECLINED, FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD, DISCARDED -> pl.hud().setPack(e.getPlayer(), false);
            default -> {}
        }
    }

    // ---------------- HP (mau vanilla lam goc) ----------------
    /** Chi nhan he so sat thuong; vanilla tu tru mau va tu xu ly cai chet. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (pl.hp().isInternal()) return; // sat thuong tu plugin da tinh san
        Player attacker = attackerOf(e);
        if (!(e.getEntity() instanceof Player p)) {
            // Nguoi choi danh quai/thuc the khac: nhan he so sat thuong dau ra
            if (attacker != null && e.getEntity() instanceof LivingEntity
                    && pl.getConfig().getBoolean("damage-output.apply-to-mobs", true)) {
                e.setDamage(e.getDamage() * pl.hp().outputMultiplier(attacker));
            }
            return;
        }
        double max = pl.hp().maxHp(p);
        switch (e.getCause()) {
            case VOID -> { e.setDamage(max); return; }
            case KILL, SUICIDE -> { return; }
            default -> {}
        }
        Entity damager = e instanceof EntityDamageByEntityEvent be ? be.getDamager() : null;
        if (damager != null && damager.getPersistentDataContainer().has(pl.trib().illusionKey(), PersistentDataType.STRING)) {
            e.setDamage(max * pl.getConfig().getDouble("heart-demon.illusion-damage-pct", 0.02));
        } else if (attacker != null && attacker != p) {
            e.setDamage(e.getDamage() * pl.hp().outputMultiplier(attacker)); // PvP: dung he so cua nguoi tan cong
        } else {
            e.setDamage(e.getDamage() * pl.getConfig().getDouble("hp.damage-scale", 5.0));
        }
    }

    private static Player attackerOf(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent be)) return null;
        Entity d = be.getDamager();
        if (d instanceof Player ap) return ap;
        if (d instanceof Projectile pr && pr.getShooter() instanceof Player ap) return ap;
        return null;
    }

    /** Nhan sat thuong that su -> huy chu thien. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && e.getFinalDamage() > 0) pl.cultivation().interrupt(p);
    }

    /** Hoi mau vanilla (1 don vi = 1/20 mau toi da) nhan voi hp.regen-scale. */
    @EventHandler(ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (pl.hp().isInternalHeal()) return; // hoi mau tu dan duoc / tu luyen da tinh san
        double scale = pl.getConfig().getDouble("hp.regen-scale", 0.2);
        e.setAmount(pl.hp().maxHp(p) * (e.getAmount() / 20.0) * scale);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        pl.trib().cancel(p);
        pl.cultivation().interrupt(p);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(pl, () -> pl.hp().fill(p));
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        if (e.getEntity().getPersistentDataContainer().has(pl.trib().illusionKey(), PersistentDataType.STRING)) {
            e.getDrops().clear();
            e.setDroppedExp(0);
        }
    }

    // ---------------- phim bam ----------------
    @EventHandler
    public void onInput(PlayerInputEvent e) {
        Player p = e.getPlayer();
        Input in = e.getInput();
        InputState st = inputs.computeIfAbsent(p.getUniqueId(), k -> new InputState());
        int bits = (in.isForward() ? 1 : 0) | (in.isBackward() ? 2 : 0) | (in.isLeft() ? 4 : 0) | (in.isRight() ? 8 : 0);

        boolean combo = in.isSprint() && in.isSneak();               // Ctrl + Shift
        if (combo && !st.combo) pl.cultivation().cycleTechnique(p);

        String mode = pl.getConfig().getString("dash.keybind", "SPRINT").toUpperCase();
        boolean sprintDash = mode.equals("SPRINT") || mode.equals("BOTH");
        boolean dashKey = sprintDash && in.isSprint() && !in.isSneak(); // Ctrl (khong Shift)
        if (dashKey && bits != 0 && (!st.dashKey || (bits & ~st.bits) != 0)) {
            pl.combat().dash(p, (bits & 1) != 0, (bits & 2) != 0, (bits & 4) != 0, (bits & 8) != 0);
        }
        st.combo = combo;
        st.dashKey = dashKey;
        st.bits = bits;
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (p.getCurrentInput().isSprint() && !pl.data().get(p).skills.isEmpty()) {
            e.setCancelled(true);
            pl.combat().cycleSkill(p); // Ctrl + F
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) { pl.cauldron().track(e.getItemDrop()); }

    // ---------------- vat pham ----------------
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        PlayerData d = pl.data().get(p);

        if (p.getCurrentInput().isSprint() && !d.skills.isEmpty()) { // Ctrl + Chuot phai
            e.setCancelled(true);
            pl.combat().cast(p);
            return;
        }
        ItemStack it = e.getItem();
        String type = Items.type(it);
        if (type == null) return;
        e.setCancelled(true);
        String id = Items.id(it);
        switch (type) {
            case Items.T_TECH -> {
                Technique t = Technique.byId(id);
                if (t == null) return;
                if (d.techniques.containsKey(t.id)) { Msg.send(p, "<gray>Bạn đã lĩnh ngộ công pháp này."); return; }
                d.techniques.put(t.id, 0);
                if (d.selectedTech == null) d.selectedTech = t.id;
                consume(p);
                Msg.send(p, "<gold>Lĩnh ngộ công pháp <yellow>" + t.display + "<gold>!");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            }
            case Items.T_SKILL -> {
                Skill s = Skill.byId(id);
                if (s == null) return;
                if (!d.skills.add(s.id)) { Msg.send(p, "<gray>Bạn đã lĩnh ngộ võ kỹ này."); return; }
                if (d.selectedSkill == null) d.selectedSkill = s.id;
                consume(p);
                Msg.send(p, "<aqua>Lĩnh ngộ võ kỹ <white>" + s.display + "<aqua>!");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            }
            case Items.T_PILL -> usePill(p, d, Pill.byId(id));
            case Items.T_TOOL -> {
                if (cooling(toolCd, p, 1000)) return;
                inspect(p, p, Items.CAN_CO.equals(id));
            }
            default -> {}
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !(e.getRightClicked() instanceof Player target)) return;
        Player p = e.getPlayer();
        ItemStack it = p.getInventory().getItemInMainHand();
        if (!Items.T_TOOL.equals(Items.type(it))) return;
        e.setCancelled(true);
        if (cooling(toolCd, p, 1000)) return;
        inspect(p, target, Items.CAN_CO.equals(Items.id(it)));
    }

    private boolean cooling(Map<UUID, Long> map, Player p, long ms) {
        long now = System.currentTimeMillis();
        Long n = map.get(p.getUniqueId());
        if (n != null && now < n) return true;
        map.put(p.getUniqueId(), now + ms);
        return false;
    }

    private void consume(Player p) {
        ItemStack h = p.getInventory().getItemInMainHand();
        if (h.getAmount() <= 1) p.getInventory().setItemInMainHand(null);
        else h.setAmount(h.getAmount() - 1);
    }

    private void usePill(Player p, PlayerData d, Pill pill) {
        if (pill == null) return;
        if (d.realm < pill.minRealm) { Msg.send(p, "<red>Thân thể chưa đủ sức chịu dược lực của đan này!"); return; }
        if (d.realm > pill.maxRealm) { Msg.send(p, "<gray>Dược lực quá yếu, vô dụng với cảnh giới của ngươi."); return; }
        double max = pl.hp().maxHp(p);
        if (p.getHealth() >= max - 0.5) { Msg.send(p, "<gray>Khí huyết đang tràn đầy."); return; }
        if (cooling(pillCd, p, 1500)) return;
        double before = p.getHealth();
        pl.hp().healPct(p, pill.healPct);
        consume(p);
        Msg.send(p, "<green>Dùng <white>" + pill.display + "<green>: <red>+" + CultivationManager.fmt(p.getHealth() - before) + " HP");
        p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EAT, 1f, 1.2f);
        p.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0);
    }

    private void inspect(Player viewer, Player target, boolean full) {
        PlayerData d = pl.data().get(target);
        if (!full && d.realm > 7) {
            Msg.send(viewer, "<light_purple>[Trắc Linh Thạch] <red>Không thể kiểm tra");
            return;
        }
        Msg.send(viewer, "<gold>━━━ " + (full ? "Căn Cơ Bàn" : "Trắc Linh Thạch") + " · <white>" + target.getName() + " <gold>━━━");
        Msg.send(viewer, "<gray>Linh căn: <white>" + Roots.root(d.linhCan).name());
        Msg.send(viewer, "<gray>Thể chất: <white>" + Roots.body(d.theChat).name());
        Msg.send(viewer, "<gray>Tu vi: <yellow>" + Realm.full(d.realm, d.stage));
        if (full) {
            Msg.send(viewer, "<gray>Linh khí đan điền: <aqua>" + CultivationManager.fmt(d.qi) + " / " + CultivationManager.fmt(Realm.qiMax(d.realm, d.stage)));
            Msg.send(viewer, "<gray>HP: <red>" + CultivationManager.fmt(target.getHealth()) + " / " + CultivationManager.fmt(pl.hp().maxHp(target)));
            StringBuilder sb = new StringBuilder();
            for (var en : d.techniques.entrySet()) {
                Technique t = Technique.byId(en.getKey());
                if (t == null) continue;
                sb.append(t.display).append(" (").append(Math.round(d.masteryPct(t) * 100)).append("%) ");
            }
            Msg.send(viewer, "<gray>Công pháp: <white>" + (sb.length() == 0 ? "chưa có" : sb.toString().trim()));
        }
    }
}
