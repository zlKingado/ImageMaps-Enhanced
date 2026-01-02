package net.craftcitizen.imagemaps;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ImageMapReloadConfigCommand extends ImageMapSubCommand {

    public ImageMapReloadConfigCommand(ImageMaps plugin) {
        super("imagemaps.reloadconfig", plugin, false);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        Float strength = null;
        String algorithm = null;

        // Lógica de Argumentos Flexível:
        // 1. /imagemap reloadconfig -> Recarrega do disco
        // 2. /imagemap reloadconfig 0.8 -> Define força
        // 3. /imagemap reloadconfig RAW -> Define algoritmo
        // 4. /imagemap reloadconfig 0.8 RAW -> Define ambos
        
        if (args.length >= 2) {
            // Tenta ler o primeiro argumento como número
            try {
                strength = Float.parseFloat(args[1]);
            } catch (NumberFormatException e) {
                // Se falhar, assume que é o nome do algoritmo (ex: reloadconfig RAW)
                algorithm = args[1].toUpperCase();
            }
        }
        
        if (args.length >= 3) {
            // Se tiver 3 argumentos, o segundo (índice 2) deve ser o algoritmo
            algorithm = args[2].toUpperCase();
        }
        
        // Aplica as mudanças
        getPlugin().reloadPluginConfig(sender, strength, algorithm);
        return null;
    }

    @Override
    public void help(CommandSender sender) {
        getPlugin().sendMsg(sender, "help_reloadconfig");
    }
}