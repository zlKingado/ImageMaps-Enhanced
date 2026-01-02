package net.craftcitizen.imagemaps;

import de.craftlancer.core.command.HelpCommand;
import de.craftlancer.core.command.SubCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class ImageMapHelpCommand extends HelpCommand {

    public ImageMapHelpCommand(Plugin plugin, Map<String, SubCommand> map) {
        super("imagemaps.help", plugin, map);
    }
    
    private ImageMaps getPluginIm() {
        return (ImageMaps) getPlugin();
    }

    @Override
    public void help(CommandSender sender) {
        ImageMaps pl = getPluginIm();
        
        String placeUsage = "/imagemap place <filename> ";
        if (pl.isGlowingSupported()) {
            placeUsage += "[frameInvisible] [frameFixed] [frameGlowing] [size]";
        } else if (pl.isInvisibilitySupported()) {
            placeUsage += "[frameInvisible] [frameFixed] [size]";
        } else {
            placeUsage += "[size]";
        }
        
        sendHelpLine(sender, placeUsage, "help_place");
        
        pl.sendMsg(sender, "help_place_size");
        pl.sendMsg(sender, "help_place_scaling");

        sendHelpLine(sender, "/imagemap download <filename> <sourceURL>", "help_download");
        sendHelpLine(sender, "/imagemap delete <filename>", "help_delete");
        sendHelpLine(sender, "/imagemap info <filename>", "help_info");
        sendHelpLine(sender, "/imagemap reload <filename>", "help_reload");
        sendHelpLine(sender, "/imagemap reloadconfig [strength] [algorithm]", "help_reloadconfig");
        sendHelpLine(sender, "/imagemap cleanup", "help_cleanup");
        sendHelpLine(sender, "/imagemap list [page]", "help_list");
        sendHelpLine(sender, "/imagemap algorithms", "help_algorithms");
        sendHelpLine(sender, "/imagemap compare [off] [Algo1 Algo2]", "help_compare");
        sendHelpLine(sender, "/imagemap help [command]", "help_help");
    }

    private void sendHelpLine(CommandSender sender, String usage, String helpKey) {
        BaseComponent combined = new TextComponent();

        BaseComponent comp1 = new TextComponent(usage);
        comp1.setColor(ChatColor.WHITE);
        
        String desc = getPluginIm().getLang().getRawMessage(sender, helpKey);
        BaseComponent comp2 = new TextComponent(" - " + desc);
        comp2.setColor(ChatColor.GRAY);

        combined.addExtra(comp1);
        combined.addExtra(comp2);
        
        // CORREÇÃO: Usar sender.spigot().sendMessage() para componentes de chat
        sender.spigot().sendMessage(combined);
    }
}