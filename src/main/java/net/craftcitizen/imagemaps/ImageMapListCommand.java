package net.craftcitizen.imagemaps;

import de.craftlancer.core.Utils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.io.File;

public class ImageMapListCommand extends ImageMapSubCommand {

    public ImageMapListCommand(ImageMaps plugin) {
        super("imagemaps.list", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        String[] fileList = new File(plugin.getDataFolder(), "images").list();
        if (fileList == null) fileList = new String[0];
        
        long page = args.length >= 2 ? Utils.parseIntegerOrDefault(args[1], 0) - 1 : 0;
        int numPages = (int) Math.ceil((double) fileList.length / Utils.ELEMENTS_PER_PAGE);

        getPlugin().sendMsg(sender, "list_header", page + 1, numPages > 0 ? numPages : 1);

        boolean even = false;
        for (String filename : Utils.paginate(fileList, page)) {
            // Cria a linha base
            BaseComponent message = new TextComponent("");

            // Nome do Arquivo (Clicável para Info)
            TextComponent nameComp = new TextComponent(" " + filename);
            nameComp.setColor(even ? ChatColor.GRAY : ChatColor.WHITE);
            nameComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                new ComponentBuilder(getPlugin().getLang().getRawMessage(sender, "msg_click_info")).create()));
            nameComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/imagemap info \"%s\"", filename)));
            
            // Botão [Place]
            BaseComponent placeAction = createButton(sender, "list_action_place", ChatColor.GREEN, 
                String.format("/imagemap place \"%s\"", filename), "msg_click_place");

            // Botão [Glow] (Troca para luminoso num raio de 15 blocos)
            BaseComponent glowAction = createButton(sender, "list_action_glow", ChatColor.AQUA, 
                String.format("/imagemap swap glowing 15 \"%s\"", filename), "msg_click_glow");

            // Botão [Normal] (Troca para normal num raio de 15 blocos)
            BaseComponent normalAction = createButton(sender, "list_action_normal", ChatColor.YELLOW, 
                String.format("/imagemap swap normal 15 \"%s\"", filename), "msg_click_normal");
            
            // Botão [Delete]
            BaseComponent deleteAction = createButton(sender, "list_action_delete", ChatColor.RED, 
                String.format("/imagemap delete \"%s\"", filename), "msg_click_delete");

            message.addExtra(nameComp);
            message.addExtra("\n      "); // Quebra de linha e indentação
            message.addExtra(placeAction);
            message.addExtra(" ");
            message.addExtra(glowAction);
            message.addExtra(" ");
            message.addExtra(normalAction);
            message.addExtra(" ");
            message.addExtra(deleteAction);
            message.addExtra("\n"); // Espaço extra entre itens

            sender.spigot().sendMessage(message);
            even = !even;
        }

        // Navegação
        BaseComponent navigation = new TextComponent();
        navigation.addExtra("\n");
        
        String prevText = String.format(getPlugin().getLang().getRawMessage(sender, "list_nav_prev"), Math.max(page, 1));
        BaseComponent prevPage = new TextComponent(prevText);
        prevPage.setColor(ChatColor.YELLOW);
        prevPage.setBold(true);
        prevPage.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/imagemap list " + Math.max(page, 1)));
        
        String nextText = String.format(getPlugin().getLang().getRawMessage(sender, "list_nav_next"), Math.min(page + 2, numPages));
        BaseComponent nextPage = new TextComponent(nextText);
        nextPage.setColor(ChatColor.YELLOW);
        nextPage.setBold(true);
        nextPage.setClickEvent(new ClickEvent(Action.RUN_COMMAND, "/imagemap list " + Math.min(page + 2, numPages)));

        navigation.addExtra(prevPage);
        navigation.addExtra(ChatColor.DARK_GRAY + "  |  ");
        navigation.addExtra(nextPage);
        
        sender.spigot().sendMessage(navigation);
        return null;
    }

    private BaseComponent createButton(CommandSender sender, String labelKey, ChatColor color, String command, String hoverKey) {
        String text = getPlugin().getLang().getRawMessage(sender, labelKey);
        String hoverText = getPlugin().getLang().getRawMessage(sender, hoverKey);
        
        TextComponent btn = new TextComponent(text);
        btn.setColor(color);
        btn.setBold(true);
        btn.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        btn.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
        return btn;
    }

    @Override
    public void help(CommandSender sender) {
    }
}