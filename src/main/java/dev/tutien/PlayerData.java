package dev.tutien;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerData {
    public final UUID uuid;
    public int realm = 0, stage = 0;
    public String linhCan, theChat;
    public double qi = 0, hp = -1;
    public String selectedTech, selectedSkill;
    public final Map<String, Integer> techniques = new LinkedHashMap<>();   // id -> diem thong thao
    public final Set<String> skills = new LinkedHashSet<>();

    // runtime
    public int cycleTicks = 0;
    public Vector lastPos;

    public PlayerData(UUID uuid) { this.uuid = uuid; }

    public double masteryPct(Technique t) {
        return Math.min(1.0, techniques.getOrDefault(t.id, 0) / (double) t.maxMastery());
    }

    public static PlayerData load(File dir, UUID id) {
        PlayerData d = new PlayerData(id);
        File f = new File(dir, id + ".yml");
        if (f.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            d.realm = y.getInt("realm", 0);
            d.stage = y.getInt("stage", 0);
            d.linhCan = y.getString("linhcan");
            d.theChat = y.getString("thechat");
            d.qi = y.getDouble("qi", 0);
            d.hp = y.getDouble("hp", -1);
            d.selectedTech = y.getString("selected-tech");
            d.selectedSkill = y.getString("selected-skill");
            var ts = y.getConfigurationSection("techniques");
            if (ts != null) for (String k : ts.getKeys(false)) d.techniques.put(k, ts.getInt(k));
            d.skills.addAll(y.getStringList("skills"));
        }
        return d;
    }

    public void save(File dir) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("realm", realm);
        y.set("stage", stage);
        y.set("linhcan", linhCan);
        y.set("thechat", theChat);
        y.set("qi", qi);
        y.set("hp", hp);
        y.set("selected-tech", selectedTech);
        y.set("selected-skill", selectedSkill);
        for (var e : techniques.entrySet()) y.set("techniques." + e.getKey(), e.getValue());
        y.set("skills", new ArrayList<>(skills));
        try {
            dir.mkdirs();
            y.save(new File(dir, uuid + ".yml"));
        } catch (IOException ex) {
            TuTienPlugin.get().getLogger().warning("Khong luu duoc du lieu " + uuid + ": " + ex.getMessage());
        }
    }
}
