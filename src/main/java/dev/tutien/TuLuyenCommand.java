package dev.tutien;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Stream;

public class TuLuyenCommand implements TabExecutor {
    private static final String PERM = "tutiendeveloper.admin";
    private static final List<String> TYPES = List.of("congphap", "vo_ky", "danduoc", "daocu");
    private final TuTienPlugin pl;

    public TuLuyenCommand(TuTienPlugin pl) { this.pl = pl; }

    private void usage(CommandSender s) {
        Msg.send(s, "<gold>/tuluyen give <player> <congphap|vo_ky|danduoc|daocu> <id> [số lượng]");
        Msg.send(s, "<gold>/tuluyen reload");
        Msg.send(s, "<gold>/tuluyen user <player> <linhcan|thechat|tuvi|congphap> ...");
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s.isOp() || s.hasPermission(PERM))) { Msg.send(s, "<red>Bạn không có quyền dùng lệnh này."); return true; }
        if (a.length == 0) { usage(s); return true; }
        switch (a[0].toLowerCase()) {
            case "reload" -> { pl.reloadConfig(); Msg.send(s, "<green>Đã reload config.yml."); }
            case "give" -> give(s, a);
            case "user" -> user(s, a);
            default -> usage(s);
        }
        return true;
    }

    private void give(CommandSender s, String[] a) {
        if (a.length < 4) { usage(s); return; }
        Player t = Bukkit.getPlayerExact(a[1]);
        if (t == null) { Msg.send(s, "<red>Người chơi không online: " + a[1]); return; }
        int amt = 1;
        if (a.length >= 5) {
            try { amt = Integer.parseInt(a[4]); } catch (NumberFormatException e) { Msg.send(s, "<red>Số lượng không hợp lệ."); return; }
            if (amt < 1 || amt > 2304) { Msg.send(s, "<red>Số lượng phải từ 1 đến 2304."); return; }
        }
        ItemStack base = Items.byTypeId(a[2], a[3], 1);
        if (base == null) { Msg.send(s, "<red>Loại hoặc ID không hợp lệ. Loại: " + String.join(", ", TYPES)); return; }
        int left = amt;
        while (left > 0) {
            ItemStack st = base.clone();
            st.setAmount(Math.min(left, st.getMaxStackSize()));
            left -= st.getAmount();
            for (ItemStack r : t.getInventory().addItem(st).values()) t.getWorld().dropItemNaturally(t.getLocation(), r);
        }
        Msg.send(s, "<green>Đã give <white>" + amt + "x " + a[3] + " <green>cho <white>" + t.getName());
    }

    private void user(CommandSender s, String[] a) {
        if (a.length < 4) { usage(s); return; }
        Player t = Bukkit.getPlayerExact(a[1]);
        if (t == null) { Msg.send(s, "<red>Người chơi không online: " + a[1]); return; }
        PlayerData d = pl.data().get(t);
        switch (a[2].toLowerCase()) {
            case "linhcan" -> {
                String id = a[3].toLowerCase();
                if (!Roots.ROOTS.containsKey(id)) { Msg.send(s, "<red>Linh căn không tồn tại. Có: " + String.join(", ", Roots.ROOTS.keySet())); return; }
                d.linhCan = id;
                Msg.send(s, "<green>Đã đặt linh căn của " + t.getName() + " = " + Roots.root(id).name());
            }
            case "thechat" -> {
                String id = a[3].toLowerCase();
                if (!Roots.BODIES.containsKey(id)) { Msg.send(s, "<red>Thể chất không tồn tại. Có: " + String.join(", ", Roots.BODIES.keySet())); return; }
                d.theChat = id;
                pl.hp().applyMax(t, true);
                Msg.send(s, "<green>Đã đặt thể chất của " + t.getName() + " = " + Roots.body(id).name());
            }
            case "tuvi" -> {
                if (a.length < 5) { Msg.send(s, "<red>/tuluyen user <player> tuvi <đại cảnh giới 1-10> <tầng 1-9>"); return; }
                int r, st;
                try { r = Integer.parseInt(a[3]); st = Integer.parseInt(a[4]); }
                catch (NumberFormatException e) { Msg.send(s, "<red>Giá trị phải là số nguyên."); return; }
                if (r < 1 || r > 10) { Msg.send(s, "<red>Đại cảnh giới phải từ 1 đến 10."); return; }
                if (st < 1 || st > 9) { Msg.send(s, "<red>Tầng phải từ 1 đến 9."); return; }
                pl.trib().cancel(t);
                d.realm = r; d.stage = st; d.qi = 0;
                pl.hp().applyMax(t, false);
                pl.hp().fill(t);
                Msg.send(s, "<green>Đã đặt tu vi của " + t.getName() + " = " + Realm.full(r, st));
            }
            case "congphap" -> congphap(s, a, t, d);
            default -> usage(s);
        }
    }

    private void congphap(CommandSender s, String[] a, Player t, PlayerData d) {
        String sub = a[3].toLowerCase();
        if (a.length < 5) { Msg.send(s, "<red>Thiếu ID công pháp."); return; }
        Technique tech = Technique.byId(a[4]);
        if (tech == null) { Msg.send(s, "<red>Công pháp không tồn tại."); return; }
        switch (sub) {
            case "add" -> {
                if (d.techniques.containsKey(tech.id)) { Msg.send(s, "<yellow>Người chơi đã có công pháp này."); return; }
                d.techniques.put(tech.id, 0);
                if (d.selectedTech == null) d.selectedTech = tech.id;
                Msg.send(s, "<green>Đã thêm " + tech.display + " cho " + t.getName());
            }
            case "remove" -> {
                if (d.techniques.remove(tech.id) == null) { Msg.send(s, "<yellow>Người chơi chưa có công pháp này."); return; }
                if (tech.id.equals(d.selectedTech)) d.selectedTech = d.techniques.keySet().stream().findFirst().orElse(null);
                Msg.send(s, "<green>Đã xóa " + tech.display + " của " + t.getName());
            }
            case "tinhthong" -> {
                if (a.length < 6) { Msg.send(s, "<red>Thiếu giá trị (số điểm hoặc %, ví dụ 50%)."); return; }
                if (!d.techniques.containsKey(tech.id)) { Msg.send(s, "<red>Người chơi chưa học công pháp này (dùng congphap add trước)."); return; }
                String v = a[5];
                int max = tech.maxMastery(), pts;
                try {
                    if (v.endsWith("%")) {
                        double pct = Double.parseDouble(v.substring(0, v.length() - 1));
                        if (pct < 0 || pct > 100) { Msg.send(s, "<red>Phần trăm phải từ 0 đến 100."); return; }
                        pts = (int) Math.round(max * pct / 100.0);
                    } else {
                        pts = Integer.parseInt(v);
                        if (pts < 0 || pts > max) { Msg.send(s, "<red>Điểm phải từ 0 đến " + max + "."); return; }
                    }
                } catch (NumberFormatException e) { Msg.send(s, "<red>Giá trị không hợp lệ."); return; }
                d.techniques.put(tech.id, pts);
                Msg.send(s, "<green>Thông thạo " + tech.display + " của " + t.getName() + " = " + pts + "/" + max + " (" + Math.round(100.0 * pts / max) + "%)");
            }
            default -> usage(s);
        }
    }

    private static List<String> filter(Stream<String> st, String prefix) {
        return st.filter(x -> x.toLowerCase().startsWith(prefix.toLowerCase())).sorted().toList();
    }

    private static Stream<String> online() { return Bukkit.getOnlinePlayers().stream().map(Player::getName); }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (!(s.isOp() || s.hasPermission(PERM))) return List.of();
        if (a.length == 1) return filter(Stream.of("give", "reload", "user"), a[0]);
        String m = a[0].toLowerCase();
        if (m.equals("give")) {
            if (a.length == 2) return filter(online(), a[1]);
            if (a.length == 3) return filter(TYPES.stream(), a[2]);
            if (a.length == 4) {
                return filter(switch (a[2].toLowerCase()) {
                    case "congphap" -> Arrays.stream(Technique.values()).map(t -> t.id);
                    case "vo_ky" -> Arrays.stream(Skill.values()).map(x -> x.id);
                    case "danduoc" -> Arrays.stream(Pill.values()).map(p -> p.id);
                    case "daocu" -> Stream.of(Items.TRAC_LINH, Items.CAN_CO);
                    default -> Stream.<String>empty();
                }, a[3]);
            }
            if (a.length == 5) return List.of("1", "16", "64");
        } else if (m.equals("user")) {
            if (a.length == 2) return filter(online(), a[1]);
            if (a.length == 3) return filter(Stream.of("linhcan", "thechat", "tuvi", "congphap"), a[2]);
            String sub = a[2].toLowerCase();
            if (a.length == 4) {
                if (sub.equals("linhcan")) return filter(Roots.ROOTS.keySet().stream(), a[3]);
                if (sub.equals("thechat")) return filter(Roots.BODIES.keySet().stream(), a[3]);
                if (sub.equals("tuvi")) return filter(IntStreamStr(1, 10), a[3]);
                if (sub.equals("congphap")) return filter(Stream.of("add", "remove", "tinhthong"), a[3]);
            }
            if (a.length == 5) {
                if (sub.equals("tuvi")) return filter(IntStreamStr(1, 9), a[4]);
                if (sub.equals("congphap")) return filter(Arrays.stream(Technique.values()).map(t -> t.id), a[4]);
            }
            if (a.length == 6 && sub.equals("congphap") && a[3].equalsIgnoreCase("tinhthong"))
                return List.of("1%", "10%", "50%", "80%", "100%");
        }
        return List.of();
    }

    private static Stream<String> IntStreamStr(int from, int to) {
        return java.util.stream.IntStream.rangeClosed(from, to).mapToObj(Integer::toString);
    }
}
