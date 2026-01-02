package net.craftcitizen.imagemaps;

import de.craftlancer.core.command.HelpCommand;
import de.craftlancer.core.command.SubCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

public class ImageMapHelpCommand extends HelpCommand {

    public ImageMapHelpCommand(Plugin plugin, Map<String, SubCommand> map) {
        super("imagemaps.help", plugin, map);
    }
    
    private ImageMaps getPluginIm() {
        return (ImageMaps) getPlugin();
    }

    @Override
    public void help(CommandSender sender) {
        ImageMaps pl = getPluginIm();
        
        // Cabeçalho Bonito
        sender.sendMessage(ChatColor.DARK_GRAY + "§m                                                        ");
        sender.sendMessage("      " + ChatColor.of("#3498db") + ChatColor.BOLD + "ImageMaps Enhanced " + ChatColor.GRAY + "v" + pl.getDescription().getVersion());
        sender.sendMessage(ChatColor.DARK_GRAY + "§m                                                        ");

        // Lista de Comandos Interativos
        sendInteractiveCommand(sender, "/imagemap place <arq>", "help_place", "/imagemap place ");
        sendInteractiveCommand(sender, "/imagemap download <nome> <url>", "help_download", "/imagemap download ");
        sendInteractiveCommand(sender, "/imagemap list", "help_list", "/imagemap list");
        
        // Novo: Botão de Swap na ajuda
        sendInteractiveCommand(sender, "/imagemap swap <tipo> <raio>", "help_swap", "/imagemap swap ");
        
        sendInteractiveCommand(sender, "/imagemap algorithms", "help_algorithms", "/imagemap algorithms");
        sendInteractiveCommand(sender, "/imagemap compare", "help_compare", "/imagemap compare");
        sendInteractiveCommand(sender, "/imagemap info <arq>", "help_info", "/imagemap info ");
        sendInteractiveCommand(sender, "/imagemap reload <arq>", "help_reload", "/imagemap reload ");
        sendInteractiveCommand(sender, "/imagemap delete <arq>", "help_delete", "/imagemap delete ");
        
        // Rodapé
        sender.sendMessage("");
        if (sender instanceof Player) {
            String footerText = pl.getLang().getRawMessage(sender, "help_github_label");
            String hoverText = pl.getLang().getRawMessage(sender, "help_github_hover");
            
            TextComponent footer = new TextComponent(footerText);
            footer.setColor(ChatColor.DARK_GRAY);
            footer.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/zlKingado/ImageMaps-Enhanced"));
            footer.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
            sender.spigot().sendMessage(footer);
        }
        sender.sendMessage(ChatColor.DARK_GRAY + "§m                                                        ");
    }

    private void sendInteractiveCommand(CommandSender sender, String syntax, String helpKey, String suggestCommand) {
        String desc = getPluginIm().getLang().getRawMessage(sender, helpKey);

        TextComponent line = new TextComponent("");
        
        // Botão [CMD]
        TextComponent cmdBtn = new TextComponent(" ➤ ");
        cmdBtn.setColor(ChatColor.GOLD);
        cmdBtn.setBold(true);
        
        // Sintaxe
        TextComponent syntaxText = new TextComponent(syntax.split(" ")[1]); // Pega só o subcomando (ex: place)
        syntaxText.setColor(ChatColor.AQUA);
        syntaxText.setBold(true);
        syntaxText.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggestCommand));
        syntaxText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
            new ComponentBuilder(ChatColor.YELLOW + syntax + "\n" + ChatColor.GRAY + desc).create()));

        // Descrição curta
        TextComponent descText = new TextComponent(" - " + desc);
        descText.setColor(ChatColor.GRAY);
        descText.setBold(false);

        line.addExtra(cmdBtn);
        line.addExtra(syntaxText);
        line.addExtra(descText);

        sender.spigot().sendMessage(line);
    }
}