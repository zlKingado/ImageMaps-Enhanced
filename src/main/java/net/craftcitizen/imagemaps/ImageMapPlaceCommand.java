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
        
        // Carrega os padrões da config
        boolean isInvisible = getPlugin().isDefaultInvisible();
        boolean isFixed = getPlugin().isDefaultFixed();
        boolean isGlowing = getPlugin().isDefaultGlowing();
        Tuple<Integer, Integer> scale = new Tuple<>(-1, -1);

        // Lógica de argumentos atualizada
        if (getPlugin().isInvisibilitySupported()) {
            if (args.length >= 3) isInvisible = Boolean.parseBoolean(args[2]);
            if (args.length >= 4) isFixed = Boolean.parseBoolean(args[3]);
            
            if (getPlugin().isGlowingSupported()) {
                if (args.length >= 5) isGlowing = Boolean.parseBoolean(args[4]);
                if (args.length >= 6) scale = parseScale(args[5]);
            } else {
                if (args.length >= 5) scale = parseScale(args[4]);
            }
        } else {
            if (args.length >= 3) scale = parseScale(args[2]);
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
        
        // Tradução correta das propriedades
        String yes = getPlugin().getLang().getRawMessage(sender, "word_yes");
        String no = getPlugin().getLang().getRawMessage(sender, "word_no");
        
        getPlugin().sendMsg(sender, "cmd_place_properties", 
                            isGlowing ? yes : no, 
                            isInvisible ? yes : no, 
                            isFixed ? yes : no);
        
        getPlugin().sendMsg(sender, "cmd_right_click_corner");
        
        return null;
    }

    @Override
    public void help(CommandSender sender) {
        getPlugin().sendMsg(sender, "help_place");
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