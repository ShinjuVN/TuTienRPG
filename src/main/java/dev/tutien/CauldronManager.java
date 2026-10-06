package dev.tutien;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Lightable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

/** Luyen dan trong vac (WATER_CAULDRON dat tren nguon nhiet). Nem nguyen lieu vao vac. */
public class CauldronManager {
    private record Req(Set<Material> mats, int amount) {}
    private record Recipe(Pill pill, List<Req> reqs) {}

    private static final Set<Material> SEEDS = EnumSet.of(Material.WHEAT_SEEDS, Material.BEETROOT_SEEDS,
            Material.MELON_SEEDS, Material.PUMPKIN_SEEDS);

    private static final List<Recipe> RECIPES = List.of(
            new Recipe(Pill.HOI_NGUYEN, List.of(
                    new Req(EnumSet.of(Material.GOLDEN_APPLE), 6),
                    new Req(EnumSet.of(Material.GOLDEN_CARROT), 16),
                    new Req(EnumSet.of(Material.DANDELION), 1))),
            new Recipe(Pill.LINH_DICH, List.of(
                    new Req(EnumSet.of(Material.ENCHANTED_GOLDEN_APPLE), 1),
                    new Req(EnumSet.of(Material.GOLDEN_APPLE), 3),
                    new Req(SEEDS, 10))),
            new Recipe(Pill.COI_NGUON, List.of(
                    new Req(EnumSet.of(Material.ENCHANTED_GOLDEN_APPLE), 3),
                    new Req(EnumSet.of(Material.EXPERIENCE_BOTTLE), 64))));

    private static class Session {
        Location loc;
        final Map<Material, Integer> items = new EnumMap<>(Material.class);
        UUID owner;
        BukkitTask task;
    }

    private final TuTienPlugin pl;
    private final Set<Item> tracked = new HashSet<>();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Random rng = new Random();

    public CauldronManager(TuTienPlugin pl) { this.pl = pl; }

    public void start() { Bukkit.getScheduler().runTaskTimer(pl, this::scan, 20L, 5L); }

    public void track(Item item) { tracked.add(item); }

    private static boolean isIngredient(Material m) {
        for (Recipe r : RECIPES) for (Req q : r.reqs()) if (q.mats().contains(m)) return true;
        return false;
    }

    private static boolean hot(Block cauldron) {
        Block d = cauldron.getRelative(BlockFace.DOWN);
        Material m = d.getType();
        if (m == Material.CAMPFIRE || m == Material.SOUL_CAMPFIRE) {
            return d.getBlockData() instanceof Lightable lt && lt.isLit();
        }
        return m == Material.FIRE || m == Material.SOUL_FIRE || m == Material.LAVA || m == Material.MAGMA_BLOCK;
    }

    private static String key(Location l) {
        return l.getWorld().getName() + "," + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    private void scan() {
        Iterator<Item> it = tracked.iterator();
        while (it.hasNext()) {
            Item i = it.next();
            if (!i.isValid() || i.getTicksLived() > 1200) { it.remove(); continue; }
            Block b = i.getLocation().getBlock();
            if (b.getType() != Material.WATER_CAULDRON || !hot(b)) continue;
            ItemStack st = i.getItemStack();
            if (Items.type(st) != null || !isIngredient(st.getType())) { it.remove(); continue; }
            add(b, st, i.getThrower());
            i.remove();
            it.remove();
        }
    }

    private void add(Block b, ItemStack st, UUID thrower) {
        String k = key(b.getLocation());
        Session s = sessions.computeIfAbsent(k, x -> new Session());
        s.loc = b.getLocation();
        if (thrower != null) s.owner = thrower;
        s.items.merge(st.getType(), st.getAmount(), Integer::sum);
        if (s.task != null) s.task.cancel();
        s.task = Bukkit.getScheduler().runTaskLater(pl, () -> brew(k), pl.getConfig().getInt("cauldron.idle-ticks", 60));
        Location c = b.getLocation().add(0.5, 1.0, 0.5);
        b.getWorld().spawnParticle(Particle.BUBBLE_POP, c, 10, 0.2, 0.1, 0.2, 0.0);
        b.getWorld().playSound(c, Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1f);
        Player o = s.owner == null ? null : Bukkit.getPlayer(s.owner);
        if (o != null) pl.hud().flash(o, "<green>Vạc: <white>+" + st.getAmount() + " " + st.getType().name().toLowerCase().replace('_', ' '), 1500);
    }

    private int batches(Recipe r, Map<Material, Integer> items) {
        int n = Integer.MAX_VALUE;
        for (Req q : r.reqs()) {
            int have = 0;
            for (Material m : q.mats()) have += items.getOrDefault(m, 0);
            n = Math.min(n, have / q.amount());
        }
        return n == Integer.MAX_VALUE ? 0 : n;
    }

    private void consume(Recipe r, int n, Map<Material, Integer> items) {
        for (Req q : r.reqs()) {
            int need = q.amount() * n;
            for (Material m : q.mats()) {
                int take = Math.min(need, items.getOrDefault(m, 0));
                if (take <= 0) continue;
                need -= take;
                items.merge(m, -take, Integer::sum);
                if (items.get(m) <= 0) items.remove(m);
            }
        }
    }

    private void giveBack(Session s, Map<Material, Integer> items) {
        Location dl = s.loc.clone().add(0.5, 1.2, 0.5);
        for (var e : items.entrySet()) {
            int c = e.getValue();
            while (c > 0) {
                int amt = Math.min(c, e.getKey().getMaxStackSize());
                Item it = dl.getWorld().dropItem(dl, new ItemStack(e.getKey(), amt));
                it.setPickupDelay(20);
                it.setVelocity(new Vector(0, 0.2, 0));
                c -= amt;
            }
        }
    }

    private void brew(String k) {
        Session s = sessions.remove(k);
        if (s == null) return;
        Block b = s.loc.getBlock();
        Player owner = s.owner == null ? null : Bukkit.getPlayer(s.owner);
        if (b.getType() != Material.WATER_CAULDRON || !hot(b)) {
            giveBack(s, s.items);
            if (owner != null) Msg.send(owner, "<gray>Vạc đã nguội, nguyên liệu được trả lại.");
            return;
        }
        Recipe best = null;
        int bestN = 0;
        for (Recipe r : RECIPES) {
            int n = batches(r, s.items);
            if (n > bestN) { best = r; bestN = n; }
        }
        if (best == null) {
            giveBack(s, s.items);
            if (owner != null) Msg.send(owner, "<gray>Nguyên liệu không khớp đan phương nào, đã trả lại.");
            return;
        }

        var c = pl.getConfig();
        double pct = Math.min(c.getDouble("cauldron.explosion.max-percent", 95),
                c.getDouble("cauldron.explosion.base-percent", 1.0) * Math.pow(bestN, c.getDouble("cauldron.explosion.growth", 1.15)));
        World w = b.getWorld();
        Location center = b.getLocation().add(0.5, 0.8, 0.5);

        if (rng.nextDouble() * 100 < pct) { // NO LO
            w.createExplosion(center, 0f, false, false);
            w.spawnParticle(Particle.EXPLOSION_EMITTER, center, 2, 0.3, 0.3, 0.3, 0);
            double r = c.getDouble("cauldron.explosion.radius", 5.0);
            double dmgPct = Math.min(0.9, c.getDouble("cauldron.explosion.damage-pct", 0.15) + 0.02 * bestN);
            for (Player p : w.getNearbyPlayers(center, r)) {
                if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
                pl.hp().magic(p, pl.hp().maxHp(p) * dmgPct); // sat thuong chuan (bo qua giap)
            }
            b.setType(Material.CAULDRON);
            if (owner != null) Msg.send(owner, "<red>✖ Lò nổ! Toàn bộ nguyên liệu đã bị hủy.");
            return;
        }

        consume(best, bestN, s.items);
        giveBack(s, s.items); // tra nguyen lieu du
        int left = bestN;
        Location dl = s.loc.clone().add(0.5, 1.2, 0.5);
        while (left > 0) {
            int amt = Math.min(left, 64);
            Item it = w.dropItem(dl, Items.pill(best.pill(), amt));
            it.setPickupDelay(10);
            it.setVelocity(new Vector(0, 0.25, 0));
            left -= amt;
        }
        if (b.getBlockData() instanceof Levelled lv) {
            if (lv.getLevel() <= 1) b.setType(Material.CAULDRON);
            else { lv.setLevel(lv.getLevel() - 1); b.setBlockData(lv); }
        }
        w.spawnParticle(Particle.END_ROD, center.clone().add(0, 0.6, 0), 30, 0.2, 0.4, 0.2, 0.05);
        w.playSound(center, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.8f);
        if (owner != null) Msg.send(owner, "<green>✔ Luyện thành công <white>" + bestN + "x " + best.pill().display + "<green>!");
    }

    public void shutdown() {
        for (Session s : new ArrayList<>(sessions.values())) {
            if (s.task != null) s.task.cancel();
            giveBack(s, s.items);
        }
        sessions.clear();
    }
}
