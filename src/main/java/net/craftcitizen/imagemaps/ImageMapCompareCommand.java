package net.craftcitizen.imagemaps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImageMapCompareCommand extends ImageMapSubCommand {

    public ImageMapCompareCommand(ImageMaps plugin) {
        super("imagemaps.compare", plugin, false);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        // Se digitar apenas "/imagemap compare", desativa o modo.
        if (args.length == 1) {
            getPlugin().setComparisonMode(false, "NONE", "NONE");
            getPlugin().sendMsg(sender, "cmd_compare_disabled");
            return null;
        }

        String left = args[1].toUpperCase();
        String right = "CURRENT";

        if (args.length >= 3) {
            right = args[2].toUpperCase();
        }

        if (!isValidAlgo(left) && !left.equals("CURRENT")) {
            getPlugin().sendMsg(sender, "error_invalid_algorithm", left);
            return null;
        }
        if (!isValidAlgo(right) && !right.equals("CURRENT")) {
            getPlugin().sendMsg(sender, "error_invalid_algorithm", right);
            return null;
        }

        getPlugin().setComparisonMode(true, left, right);
        
        String displayRight = right.equals("CURRENT") ? getPlugin().getLang().getRawMessage(sender, "word_current") + " (" + getPlugin().getDitherAlgorithm() + ")" : right;
        
        getPlugin().sendMsg(sender, "cmd_compare_active", left, displayRight);
        
        if (left.equals(getPlugin().getDitherAlgorithm()) && right.equals("CURRENT")) {
            getPlugin().sendMsg(sender, "warn_compare_same");
        }

        return null;
    }
    
    private boolean isValidAlgo(String algo) {
        // RAW = Sem Algoritmo (Cru)
        // NONE = Transparente (Ocultar)
        return Arrays.asList("FLOYD", "ATKINSON", "BURKES", "SIERRA", "NONE", "RAW").contains(algo);
    }

    @Override
    public void help(CommandSender sender) {
        getPlugin().sendMsg(sender, "help_compare");
    }

    @Override
    protected List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> algos = Arrays.asList("FLOYD", "ATKINSON", "BURKES", "SIERRA", "RAW");
        
        if (args.length == 2) {
            return de.craftlancer.core.Utils.getMatches(args[1], algos);
        }
        if (args.length == 3) {
            return de.craftlancer.core.Utils.getMatches(args[2], algos);
        }
        
        return Collections.emptyList();
    }
}