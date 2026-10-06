package dev.tutien;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

public class GuidebookCommand implements CommandExecutor {
    private static final List<String> PAGES = List.of(
            "<dark_red><bold>CỔ GIỚI\nHUYỀN HUYỄN</bold>\n<dark_gray>~ Sách nhập môn tu tiên ~\n\n<black>Thuở hồng hoang, trời đất hỗn mang, linh khí tràn ngập. Kẻ nghịch thiên cải mệnh bước lên đường tu đạo, tranh một tia sinh cơ giữa vạn kiếp luân hồi.",
            "<dark_red><bold>CẢNH GIỚI</bold>\n\n<black>Mười đại cảnh giới, mỗi cảnh giới chín tầng:\nLuyện Khí, Trúc Cơ, Kim Đan, Nguyên Anh, Hóa Thần, Luyện Hư, Hợp Thể, Đại Thừa, Độ Kiếp, rồi Đại Đế.\n\nPhàm nhân chưa nhập đạo thì chưa có tu vi.",
            "<dark_red><bold>CĂN CỐT</bold>\n\n<black>Sinh ra ai cũng mang một <bold>linh căn</bold> và một <bold>thể chất</bold>. Linh căn quyết định tốc độ hấp thu linh khí, thể chất quyết định khí huyết.\n\nDùng Trắc Linh Thạch hoặc Căn Cơ Bàn để xem căn cốt.",
            "<dark_red><bold>TU LUYỆN</bold>\n\n<black>Nhấn <bold>Ctrl + Shift</bold> để chọn công pháp, tên hiện trên thanh thông tin.\n\nGiữ <bold>Shift</bold>, đứng yên hoàn toàn 10 giây là một chu thiên. Bị thương hay nhúc nhích thì chu thiên tan biến.",
            "<dark_red><bold>LINH KHÍ</bold>\n\n<black>Mỗi vùng đất có mật độ linh khí khác nhau, nơi sương mù trắng giăng kín là linh địa dồi dào. Công pháp càng cao, thông thạo càng sâu thì hấp thu càng nhiều.\n\nLinh khí dư thừa khi khí huyết chưa đầy sẽ hóa thành sinh lực.",
            "<dark_red><bold>CÔNG PHÁP</bold>\n\n<black>Hấp Tinh Pháp (Phàm)\nQuy Nguyên Quyết (Sơ)\nPhệ Lôi Thần Công (Trung)\nHỗn Độn Thôn Thiên Kinh (Đỉnh)\n\nCầm bí tịch, <bold>chuột phải</bold> để lĩnh ngộ. Tu luyện càng lâu, thông thạo càng cao.",
            "CHIEN_DAU_PLACEHOLDER",
            "<dark_red><bold>ĐẠO CỤ</bold>\n\n<dark_purple>Trắc Linh Thạch<black>\nLapis ở bốn hướng, Mảnh Thạch Anh Tím ở giữa.\n\n<gold>Căn Cơ Bàn<black>\nVàng ở bốn góc, Kim Cương ở bốn cạnh, La Bàn ở giữa.\n\nChuột phải vào người khác hoặc vào không khí để xem.",
            "<dark_red><bold>LUYỆN ĐAN</bold>\n\n<black>Đặt Vạc đầy nước lên nguồn lửa (lửa trại, dung nham, khối magma) rồi <bold>ném</bold> nguyên liệu vào. Ném dư để luyện nhiều viên cùng lúc, nhưng càng nhiều lò càng dễ nổ!",
            "<dark_green><bold>Hồi Nguyên Đan</bold>\n<black>6 Táo vàng\n16 Cà rốt vàng\n1 Bồ công anh\n\n<dark_green><bold>Linh Dịch Sự Sống</bold>\n<black>1 Táo vàng phù phép\n3 Táo vàng\n10 Hạt giống",
            "<dark_green><bold>Cội Nguồn Sinh Thủy</bold>\n<black>3 Táo vàng phù phép\n64 Lọ kinh nghiệm\n\n<dark_red><bold>KIẾP NẠN</bold>\n<black>Đột phá sẽ đón Tâm Ma Kiếp (ảo ảnh quấy nhiễu đạo tâm) và Lôi Kiếp. Hãy sẵn sàng đan dược!");

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { Msg.send(sender, "<red>Chỉ người chơi mới dùng được lệnh này."); return true; }
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.title(Msg.mm("Cổ Giới Huyền Huyễn"));
        meta.author(Msg.mm("Cổ Giới"));
        boolean dash = TuTienPlugin.get().getConfig().getBoolean("dash.enabled", false);
        String chienDau = "<dark_red><bold>CHIẾN ĐẤU</bold>\n\n<black>"
                + (dash ? "<bold>Ctrl + W/A/S/D</bold> (hoặc phím server quy định): lướt.\n" : "")
                + "<bold>Ctrl + F</bold>: đổi võ kỹ.\n<bold>Ctrl + chuột phải</bold>: thi triển võ kỹ.\n\nVõ kỹ có sáu phẩm: Hạ, Trung, Thượng, Thiên Giai, Thánh Cấp, Cấp Đế.";
        List<Component> pages = PAGES.stream().map(x -> Msg.mm(x.equals("CHIEN_DAU_PLACEHOLDER") ? chienDau : x)).toList();
        meta.pages(pages);
        book.setItemMeta(meta);
        for (ItemStack left : p.getInventory().addItem(book).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
        Msg.send(p, "<gold>Bạn nhận được <yellow>Cổ Giới Huyền Huyễn<gold>.");
        return true;
    }
}
