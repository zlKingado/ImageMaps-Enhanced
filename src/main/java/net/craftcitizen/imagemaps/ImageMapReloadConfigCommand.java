package net.craftcitizen.imagemaps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import java.util.Arrays;
import java.util.List;

public class ImageMapReloadConfigCommand extends ImageMapSubCommand {

    public ImageMapReloadConfigCommand(ImageMaps plugin) {
        super("imagemaps.admin", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        Float newStrength = null;
        String newAlgorithm = null;
        List<String> validAlgorithms = Arrays.asList("FLOYD", "ATKINSON", "BURKES", "SIERRA");

        if (args.length >= 2) {
            try {
                newStrength = Float.parseFloat(args[1]);
                if (newStrength < 0.0f || newStrength > 1.0f) {
                     // Você pode adicionar chaves específicas para esses erros de validação
                     getPlugin().sendMsg(sender, "cmd_must_specify_filename"); // Placeholder temporário
                     return null;
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }

        if (args.length >= 3) {
            String inputAlgo = args[2].toUpperCase();
            if (validAlgorithms.contains(inputAlgo)) {
                newAlgorithm = inputAlgo;
            } else {
                return null;
            }
        }

        getPlugin().reloadPluginConfig(sender, newStrength, newAlgorithm);
        return null;
    }

    @Override
    public void help(CommandSender sender) {
    }
}