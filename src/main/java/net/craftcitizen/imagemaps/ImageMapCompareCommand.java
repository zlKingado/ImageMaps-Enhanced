package net.craftcitizen.imagemaps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.Arrays;
import java.util.List;

public class ImageMapCompareCommand extends ImageMapSubCommand {

    public ImageMapCompareCommand(ImageMaps plugin) {
        super("imagemaps.admin", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        List<String> validAlgos = Arrays.asList("FLOYD", "ATKINSON", "BURKES", "SIERRA", "NONE", "CURRENT");

        // /imagemap compare
        if (args.length == 1) {
            getPlugin().setComparisonMode(true, "NONE", "CURRENT");
            getPlugin().sendMsg(sender, "cmd_compare_active", "NONE", "CURRENT");
            return null;
        }

        // /imagemap compare off
        if (args.length == 2 && args[1].equalsIgnoreCase("off")) {
            getPlugin().setComparisonMode(false, "NONE", "CURRENT");
            getPlugin().sendMsg(sender, "cmd_compare_disabled");
            return null;
        }

        // /imagemap compare <Algo1> <Algo2>
        if (args.length >= 3) {
            String algo1 = args[1].toUpperCase();
            String algo2 = args[2].toUpperCase();

            if (!validAlgos.contains(algo1) || !validAlgos.contains(algo2)) {
                 return null;
            }

            getPlugin().setComparisonMode(true, algo1, algo2);
            getPlugin().sendMsg(sender, "cmd_compare_comparing", algo1, algo2);
            return null;
        }

        return null;
    }

    @Override
    public void help(CommandSender sender) {
    }
}