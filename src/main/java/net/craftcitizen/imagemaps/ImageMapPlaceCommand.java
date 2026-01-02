package net.craftcitizen.imagemaps;

import de.craftlancer.core.Utils;
import de.craftlancer.core.util.Tuple;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImageMapPlaceCommand extends ImageMapSubCommand {

    public ImageMapPlaceCommand(ImageMaps plugin) {
        super("imagemaps.place", plugin, false);
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
        boolean isInvisible = false;
        boolean isFixed = false;
        boolean isGlowing = false;
        Tuple<Integer, Integer> scale;

        if (getPlugin().isInvisibilitySupported()) {
            isInvisible = args.length >= 3 && Boolean.parseBoolean(args[2]);
            isFixed = args.length >= 4 && Boolean.parseBoolean(args[3]);
            if (getPlugin().isGlowingSupported()) {
                isGlowing = args.length >= 5 && Boolean.parseBoolean(args[4]);
                scale = args.length >= 6 ? parseScale(args[5]) : new Tuple<>(-1, -1);
            }
            else {
                scale = args.length >= 5 ? parseScale(args[4]) : new Tuple<>(-1, -1);
            }
        }
        else {
            scale = args.length >= 3 ? parseScale(args[2]) : new Tuple<>(-1, -1);
        }

        if (filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            getPlugin().sendMsg(sender, "cmd_filename_illegal");
            return null;
        }

        if (!getPlugin().hasImage(filename)) {
            getPlugin().sendMsg(sender, "cmd_no_image_exists");
            return null;
        }

        Player player = (Player) sender;
        player.setMetadata(ImageMaps.PLACEMENT_METADATA,
                           new FixedMetadataValue(getPlugin(),
                                                  new PlacementData(filename, isInvisible, isFixed, isGlowing, scale)));

        Tuple<Integer, Integer> size = getPlugin().getImageSize(filename, scale);
        
        getPlugin().sendMsg(sender, "cmd_started_placing", args[1], size.getKey(), size.getValue());
        getPlugin().sendMsg(sender, "cmd_right_click_corner");
        
        return null;
    }

    @Override
    public void help(CommandSender sender) {
        // Help messages can also be translated similarly if keys are added
    }

    private static Tuple<Integer, Integer> parseScale(String string) {
        String[] tmp = string.split("x");

        if (tmp.length < 2)
            return new Tuple<>(-1, -1);

        return new Tuple<>(Utils.parseIntegerOrDefault(tmp[0], -1), Utils.parseIntegerOrDefault(tmp[1], -1));
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length > 2 && !getPlugin().isInvisibilitySupported()
            || args.length > 4 && !getPlugin().isGlowingSupported()) {
            return Collections.emptyList();
        }

        switch (args.length) {
            case 2:
                return Utils.getMatches(args[1], new File(plugin.getDataFolder(), "images").list());
            case 3:
                return Utils.getMatches(args[2], Arrays.asList("true", "false"));
            case 4:
                return Utils.getMatches(args[3], Arrays.asList("true", "false"));
            case 5:
                return Utils.getMatches(args[4], Arrays.asList("true", "false"));
            default:
                return Collections.emptyList();
        }
    }
}