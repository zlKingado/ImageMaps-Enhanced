package net.craftcitizen.imagemaps;

import javax.imageio.ImageIO;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ImageMapDebugInfoCommand extends ImageMapSubCommand {

    public ImageMapDebugInfoCommand(ImageMaps plugin) {
        super("imagemaps.admin", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        getPlugin().sendMsg(sender, "debug_version", getPlugin().getDescription().getVersion());
        getPlugin().sendMsg(sender, "debug_os", System.getProperty("os.name"));
        getPlugin().sendMsg(sender, "debug_imageio_params");
        getPlugin().sendMsg(sender, "debug_formats", String.join(", ", ImageIO.getReaderFormatNames()));
        getPlugin().sendMsg(sender, "debug_suffixes", String.join(", ", ImageIO.getReaderFileSuffixes()));
        getPlugin().sendMsg(sender, "debug_mime", String.join(", ", ImageIO.getReaderMIMETypes()));
        getPlugin().sendMsg(sender, "debug_cache", Boolean.toString(ImageIO.getUseCache()));
        return null;
    }

    @Override
    public void help(CommandSender sender) {
        getPlugin().sendMsg(sender, "help_debug");
    }

}