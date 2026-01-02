package net.craftcitizen.imagemaps;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ImageMapAlgorithmsCommand extends ImageMapSubCommand {

    public ImageMapAlgorithmsCommand(ImageMaps plugin) {
        super("imagemaps.admin", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        getPlugin().sendMsg(sender, "cmd_algorithms_header");
        
        sendAlgoInfo(sender, "FLOYD", getPlugin().getLang().getRawMessage(sender, "algo_desc_floyd"));
        sendAlgoInfo(sender, "ATKINSON", getPlugin().getLang().getRawMessage(sender, "algo_desc_atkinson"));
        sendAlgoInfo(sender, "BURKES", getPlugin().getLang().getRawMessage(sender, "algo_desc_burkes"));
        sendAlgoInfo(sender, "SIERRA", getPlugin().getLang().getRawMessage(sender, "algo_desc_sierra"));

        getPlugin().sendMsg(sender, "cmd_algorithms_footer");
        
        return null;
    }

    private void sendAlgoInfo(CommandSender sender, String name, String desc) {
        TextComponent message = new TextComponent("[" + name + "]");
        message.setColor(ChatColor.AQUA);
        message.setBold(true);
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/imagemap reloadconfig 0.8 " + name));
        
        String hoverText = String.format(getPlugin().getLang().getRawMessage(sender, "algo_click_hover"), name);
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
        
        TextComponent description = new TextComponent(" - " + desc);
        description.setColor(ChatColor.WHITE);
        description.setBold(false);
        
        message.addExtra(description);
        
        // CORREÇÃO: Usar sender.spigot().sendMessage() para componentes de chat
        sender.spigot().sendMessage(message);
    }

    @Override
    public void help(CommandSender sender) {
    }
}