package net.craftcitizen.imagemaps;

import de.craftlancer.core.Utils;
import de.craftlancer.core.util.Tuple;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class ImageMapInfoCommand extends ImageMapSubCommand {

    public ImageMapInfoCommand(ImageMaps plugin) {
        super("imagemaps.info", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        if (args.length < 2) {
            getPlugin().sendMsg(sender, "cmd_must_specify_filename");
            return null;
        }

        String filename = args[1];
        BufferedImage image = getPlugin().getImage(filename);

        if (image == null) {
            getPlugin().sendMsg(sender, "cmd_no_image_exists");
            return null;
        }

        Tuple<Integer, Integer> size = getPlugin().getImageSize(filename, null);
        
        BaseComponent reloadAction = new TextComponent(getPlugin().getLang().getRawMessage(sender, "list_action_reload"));
        reloadAction.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                  String.format("/imagemap reload \"%s\"", filename)));
        
        BaseComponent placeAction = new TextComponent(getPlugin().getLang().getRawMessage(sender, "list_action_place"));
        placeAction.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                 String.format("/imagemap place \"%s\"", filename)));
        
        BaseComponent deleteAction = new TextComponent(getPlugin().getLang().getRawMessage(sender, "list_action_delete"));
        deleteAction.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                                  String.format("/imagemap delete \"%s\"", filename)));

        BaseComponent actions = new TextComponent(getPlugin().getLang().getRawMessage(sender, "info_actions_label"));
        actions.addExtra(reloadAction);
        actions.addExtra(" ");
        actions.addExtra(placeAction);
        actions.addExtra(" ");
        actions.addExtra(deleteAction);

        getPlugin().sendMsg(sender, "info_header");
        getPlugin().sendMsg(sender, "info_filename", filename);
        getPlugin().sendMsg(sender, "info_resolution", image.getWidth(), image.getHeight());
        getPlugin().sendMsg(sender, "info_ingame_size", size.getKey(), size.getValue());
        
        // CORREÇÃO: Usar sender.spigot().sendMessage()
        sender.spigot().sendMessage(actions);
        
        return null;
    }

    @Override
    public void help(CommandSender sender) {
        getPlugin().sendMsg(sender, "help_info");
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2)
            return Utils.getMatches(args[1], new File(plugin.getDataFolder(), "images").list());

        return Collections.emptyList();
    }
}