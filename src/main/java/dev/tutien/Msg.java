package dev.tutien;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class Msg {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private Msg() {}

    public static Component mm(String s) { return MM.deserialize(s); }

    /** Component cho ten/lore vat pham (tat in nghieng mac dinh). */
    public static Component item(String s) { return MM.deserialize(s).decoration(TextDecoration.ITALIC, false); }

    public static void send(CommandSender to, String s) { to.sendMessage(mm(s)); }
}
