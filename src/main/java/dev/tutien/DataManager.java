package dev.tutien;

import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {
    private final File dir;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public DataManager(TuTienPlugin plugin) {
        this.dir = new File(plugin.getDataFolder(), "data");
    }

    public PlayerData get(Player p) {
        return cache.computeIfAbsent(p.getUniqueId(), id -> {
            PlayerData d = PlayerData.load(dir, id);
            boolean fresh = d.linhCan == null || d.theChat == null;
            if (d.linhCan == null) d.linhCan = Roots.randomRoot();
            if (d.theChat == null) d.theChat = Roots.randomBody();
            if (fresh) d.save(dir);
            return d;
        });
    }

    public void save(Player p) {
        PlayerData d = cache.get(p.getUniqueId());
        if (d == null) return;
        if (!p.isDead()) d.hp = p.getHealth();
        d.save(dir);
    }

    public void saveAll() {
        for (PlayerData d : cache.values()) {
            Player p = org.bukkit.Bukkit.getPlayer(d.uuid);
            if (p != null && !p.isDead()) d.hp = p.getHealth();
            d.save(dir);
        }
    }

    public void unload(Player p) {
        PlayerData d = cache.remove(p.getUniqueId());
        if (d == null) return;
        if (!p.isDead()) d.hp = p.getHealth();
        d.save(dir);
    }
}
