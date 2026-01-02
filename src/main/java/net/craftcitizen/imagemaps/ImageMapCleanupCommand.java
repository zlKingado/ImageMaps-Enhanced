package net.craftcitizen.imagemaps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ImageMapCleanupCommand extends ImageMapSubCommand {

    public ImageMapCleanupCommand(ImageMaps plugin) {
        super("imagemaps.admin", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        int removedMaps = getPlugin().cleanupMaps();
        getPlugin().sendMsg(sender, "cmd_cleanup_success", removedMaps);
        return null;
    }

    @Override
    public void help(CommandSender sender) {
    }

}