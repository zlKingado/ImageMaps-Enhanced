package net.craftcitizen.imagemaps;

import de.craftlancer.core.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class ImageMapReloadCommand extends ImageMapSubCommand {

    public ImageMapReloadCommand(ImageMaps plugin) {
        super("imagemap.reload", plugin, true);
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

        if (filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            getPlugin().sendMsg(sender, "cmd_filename_illegal");
            return null;
        }

        if (getPlugin().reloadImage(filename))
            getPlugin().sendMsg(sender, "cmd_reload_success");
        else
            getPlugin().sendMsg(sender, "cmd_reload_failed");

        return null;
    }

    @Override
    public void help(CommandSender sender) {
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2)
            return Utils.getMatches(args[1], new File(plugin.getDataFolder(), "images").list());

        return Collections.emptyList();
    }

}