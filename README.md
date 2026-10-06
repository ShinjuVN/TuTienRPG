# TuTienRPG (Paper/Purpur 1.21.11, Java 21)

Build: `gradle build` -> `build/libs/TuTienRPG.jar` (chép vào `plugins/`).

## Phím (cần Paper 1.21.2+, dùng PlayerInputEvent)
- Shift giữ 10s đứng yên: tu luyện 1 chu thiên
- Ctrl+Shift: đổi công pháp | Ctrl+WASD: lướt (mặc định TẮT, bật bằng `dash.enabled: true`; đổi phím bằng `dash.keybind`)
- Ctrl+F: đổi võ kỹ | Ctrl+Chuột phải: thi triển võ kỹ
- Ctrl = phím Sprint của client (mặc định Ctrl). Nếu người chơi đổi keybind Sprint thì các phím trên đổi theo.

## Lệnh
- /guidebook (mọi người)
- /tuluyen give|reload|user ... (OP hoặc `tutiendeveloper.admin`)

## Lưu ý thiết kế
- MÁU VANILLA LÀM GỐC: MAX_HEALTH = HP RPG (1 attribute modifier `tutien:rpg_max_health`, cộng thêm vào base 20), máu hiện tại = player.getHealth(). Thanh HP chỉ đọc 2 giá trị này; chết, thông báo chết, totem, Health Boost, plugin miễn thương... đều hoạt động như vanilla.
- BẮT BUỘC: nâng giới hạn máu trong spigot.yml: `settings.attribute.maxHealth.max: 1000000` rồi restart (mặc định 2048, plugin sẽ cảnh báo trong log nếu bị chặn).
- Client dùng health scale 20 (setHealthScaled) nên người không có pack vẫn thấy 10 tim đúng tỷ lệ.
- Sát thương nhận vào nhân hp.damage-scale; hồi máu vanilla nhân hp.regen-scale (theo % máu tối đa); VOID giết thẳng.
- Sát thương từ plugin (võ kỹ, lôi kiếp, nổ vạc) đi qua player.damage(DamageSource) nên có thông báo chết riêng và đi qua plugin miễn thương.
- Modifier max health được lưu vào dữ liệu người chơi; nếu gỡ plugin hãy dùng /attribute để gỡ modifier này.
- Công thức ngoài đề bài do plugin tự đặt: Trắc Linh Thạch, Căn Cơ Bàn (craft thường), "bông hoa vàng" = Bồ công anh, "seed" = 4 loại hạt giống.
- Vạc: WATER_CAULDRON đặt trên lửa trại đang cháy / lửa / lava / magma. Ném (Q) nguyên liệu vào vạc.
- Mapping Đại Cảnh Giới 1-10: Luyện Khí, Trúc Cơ, Kim Đan, Nguyên Anh, Hóa Thần, Luyện Hư, Hợp Thể, Đại Thừa, Độ Kiếp, Đại Đế.
- Chưa được biên dịch thử trong môi trường tạo file; nếu Gradle báo lỗi API, gửi log để sửa.

## Sát thương đầu ra
- Hệ số = HP RPG mục tiêu / 20 * `damage-output.factor` (tối thiểu 1), áp cho đánh thường, cung/tên, và quy đổi võ kỹ (`Skill.damage` = số đòn căn bản * base-hit).
- PvP: dùng hệ số của người tấn công (không nhân thêm hp.damage-scale). Quái: nhân nếu `apply-to-mobs: true`.

## Dash
- Dash dùng vận tốc thật (không teleport) nên mượt; nếu vẫn khựng khi combat thì để `dash.enabled: false`.
- Với keybind SPRINT, mọi lần bắt đầu chạy bằng Ctrl + hướng đều có thể kích dash; nên dùng CHANNEL nếu bật.

## Dash bằng phím khác (Tab...)
Client vanilla KHÔNG gửi phím Tab hay phím tùy ý cho server (Paper chỉ biết W/A/S/D, Space, Shift, Ctrl). Muốn dùng Tab cần client mod:
1. Đặt `dash.keybind: CHANNEL` (hoặc BOTH).
2. Mod bắt Tab, gửi custom payload kênh `tutien:dash`, nội dung 1 byte: bit0=W, bit1=S, bit2=A, bit3=D (lấy từ phím di chuyển đang giữ).
Server vẫn kiểm cooldown, va chạm và trạng thái nên không tin client.
