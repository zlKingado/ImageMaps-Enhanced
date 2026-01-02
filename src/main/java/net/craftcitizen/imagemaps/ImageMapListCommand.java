package net.craftcitizen.imagemaps;

import de.craftlancer.core.Utils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;

public class ImageMapListCommand extends ImageMapSubCommand {

    public ImageMapListCommand(ImageMaps plugin) {
        super("imagemaps.list", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        String[] fileList = new File(plugin.getDataFolder(), "images").list();
        if (fileList == null) fileList = new String[0];
        
        long page = args.length >= 2 ? Utils.parseIntegerOrDefault(args[1], 0) - 1 : 0;
        int numPages = (int) Math.ceil((double) fileList.length / Utils.ELEMENTS_PER_PAGE);

        getPlugin().sendMsg(sender, "list_header", page + 1, numPages > 0 ? numPages : 1);

        boolean even = false;
        for (String filename : Utils.paginate(fileList, page)) {
            BaseComponent infoAction = new TextComponent(getPlugin().getLang().getRawMessage(sender, "list_action_info"));
            infoAction.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                    String.format("/imagemap info \"%s\"", filename)));
            
            BaseComponent reloadAction = new TextComponent(getPlugin().getLang().getRawMessage(sender, "list_action_reload"));
            reloadAction.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                      String.format("/imagemap reload \"%s\"", filename)));
            
            BaseComponent placeAction = new TextComponent(getPlugin().getLang().getRawMessage(sender, "list_action_place"));
            placeAction.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                     String.format("/imagemap place \"%s\"", filename)));
            
            BaseComponent deleteAction = new TextComponent(getPlugin().getLang().getRawMessage(sender, "list_action_delete"));
            deleteAction.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                      String.format("/imagemap delete \"%s\"", filename)));

            BaseComponent message = new TextComponent(filename);
            message.setColor(even ? ChatColor.GRAY : ChatColor.WHITE);
            message.addExtra(" ");
            message.addExtra(infoAction);
            message.addExtra(" ");
            message.addExtra(reloadAction);
            message.addExtra(" ");
            message.addExtra(placeAction);
            message.addExtra(" ");
            message.addExtra(deleteAction);

            // CORREÇÃO: Usar sender.spigot().sendMessage()
            sender.spigot().sendMessage(message);
            even = !even;
        }

        BaseComponent navigation = new TextComponent();
        
        String prevText = String.format(getPlugin().getLang().getRawMessage(sender, "list_nav_prev"), Math.max(page, 1));
        BaseComponent prevPage = new TextComponent(prevText);
        prevPage.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/imagemap list " + Math.max(page, 1)));
        
        String nextText = String.format(getPlugin().getLang().getRawMessage(sender, "list_nav_next"), Math.min(page + 2, numPages));
        BaseComponent nextPage = new TextComponent(nextText);
        nextPage.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/imagemap list " + Math.min(page + 2, numPages)));

        navigation.addExtra(prevPage);
        navigation.addExtra(" | ");
        navigation.addExtra(nextPage);
        
        // CORREÇÃO: Usar sender.spigot().sendMessage()
        sender.spigot().sendMessage(navigation);
        return null;
    }

    @Override
    public void help(CommandSender sender) {
    }
}