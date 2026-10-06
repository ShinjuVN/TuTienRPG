package dev.tutien;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class TuTienPlugin extends JavaPlugin {
    private static TuTienPlugin instance;
    private DataManager data;
    private HpManager hp;
    private HudManager hud;
    private CultivationManager cultivation;
    private TribulationManager trib;
    private CombatManager combat;
    private CauldronManager cauldron;

    public static TuTienPlugin get() { return instance; }
    public DataManager data() { return data; }
    public HpManager hp() { return hp; }
    public HudManager hud() { return hud; }
    public CultivationManager cultivation() { return cultivation; }
    public TribulationManager trib() { return trib; }
    public CombatManager combat() { return combat; }
    public CauldronManager cauldron() { return cauldron; }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        Items.init(this);
        data = new DataManager(this);
        hp = new HpManager(this);
        hud = new HudManager(this);
        cultivation = new CultivationManager(this);
        trib = new TribulationManager(this);
        combat = new CombatManager(this);
        cauldron = new CauldronManager(this);

        Bukkit.getPluginManager().registerEvents(new Listeners(this), this);
        var tl = new TuLuyenCommand(this);
        getCommand("tuluyen").setExecutor(tl);
        getCommand("tuluyen").setTabCompleter(tl);
        getCommand("guidebook").setExecutor(new GuidebookCommand());

        // Dash bang phim do client mod gui (vd Tab): 1 byte, bit0=W bit1=S bit2=A bit3=D
        getServer().getMessenger().registerIncomingPluginChannel(this, "tutien:dash", (channel, player, msg) -> {
            String mode = getConfig().getString("dash.keybind", "SPRINT").toUpperCase();
            if (!mode.equals("CHANNEL") && !mode.equals("BOTH")) return;
            if (msg.length < 1 || (msg[0] & 15) == 0) return;
            int b = msg[0];
            combat.dash(player, (b & 1) != 0, (b & 2) != 0, (b & 4) != 0, (b & 8) != 0);
        });

        registerRecipes();
        hud.start();
        cultivation.start();
        cauldron.start();
        Bukkit.getScheduler().runTaskTimer(this, () -> data.saveAll(), 6000L, 6000L);

        // server reload: nap lai nguoi dang online
        for (Player p : Bukkit.getOnlinePlayers()) hp.onJoin(p);
        getLogger().info("TuTienRPG da bat.");
    }

    @Override
    public void onDisable() {
        if (trib != null) trib.shutdown();
        if (cauldron != null) cauldron.shutdown();
        if (data != null) data.saveAll();
    }

    private void registerRecipes() {
        ShapedRecipe trac = new ShapedRecipe(new NamespacedKey(this, "trac_linh_thach"), Items.tool(Items.TRAC_LINH));
        trac.shape(" L ", "LAL", " L ");
        trac.setIngredient('L', Material.LAPIS_LAZULI);
        trac.setIngredient('A', Material.AMETHYST_SHARD);
        Bukkit.addRecipe(trac);

        ShapedRecipe canco = new ShapedRecipe(new NamespacedKey(this, "can_co_ban"), Items.tool(Items.CAN_CO));
        canco.shape("GDG", "DCD", "GDG");
        canco.setIngredient('G', Material.GOLD_INGOT);
        canco.setIngredient('D', Material.DIAMOND);
        canco.setIngredient('C', Material.COMPASS);
        Bukkit.addRecipe(canco);
    }
}
