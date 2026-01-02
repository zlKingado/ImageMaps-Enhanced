package net.craftcitizen.imagemaps;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ImageMapAlgorithmsCommand extends ImageMapSubCommand {

    public ImageMapAlgorithmsCommand(ImageMaps plugin) {
        super("imagemaps.algorithms", plugin, false);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        getPlugin().sendMsg(sender, "cmd_algorithms_header");

        sendAlgorithmInfo(sender, "Floyd-Steinberg", "FLOYD", "algo_desc_floyd");
        sendAlgorithmInfo(sender, "Atkinson", "ATKINSON", "algo_desc_atkinson");
        sendAlgorithmInfo(sender, "Burkes", "BURKES", "algo_desc_burkes");
        sendAlgorithmInfo(sender, "Sierra", "SIERRA", "algo_desc_sierra");
        
        // NOVO: Adicionado RAW na lista
        sendAlgorithmInfo(sender, "Raw", "RAW", "algo_desc_raw");

        getPlugin().sendMsg(sender, "cmd_algorithms_footer");
        return null;
    }

    private void sendAlgorithmInfo(CommandSender sender, String displayName, String algoCode, String descKey) {
        String description = getPlugin().getLang().getRawMessage(sender, descKey);
        // Mensagem de hover (ex: Clique para ativar RAW (0.8))
        String hoverText = String.format(getPlugin().getLang().getRawMessage(sender, "algo_click_hover"), displayName);

        // Botão Principal
        TextComponent message = new TextComponent(" ➤ " + displayName);
        message.setColor(ChatColor.GOLD);
        message.setBold(true);
        // Ao clicar, aplica o algoritmo com força padrão 0.8
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/imagemap reloadconfig 0.8 " + algoCode));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));

        // Descrição abaixo
        TextComponent desc = new TextComponent("\n    " + description);
        desc.setColor(ChatColor.GRAY);
        desc.setBold(false);

        sender.spigot().sendMessage(message);
        sender.spigot().sendMessage(desc);
        sender.spigot().sendMessage(new TextComponent("\n")); // Espaçamento
    }

    @Override
    public void help(CommandSender sender) {
        getPlugin().sendMsg(sender, "help_algorithms");
    }
}