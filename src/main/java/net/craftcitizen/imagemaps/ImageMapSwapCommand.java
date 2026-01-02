package net.craftcitizen.imagemaps;

import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

import java.util.List;

public class ImageMapSwapCommand extends ImageMapSubCommand {

    public ImageMapSwapCommand(ImageMaps plugin) {
        super("imagemaps.swap", plugin, false);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        // Usage: /imagemap swap <normal|glowing> <radius> [filename]
        if (args.length < 3) {
            getPlugin().sendMsg(sender, "cmd_swap_usage");
            return null;
        }

        if (!getPlugin().isGlowingSupported()) {
            getPlugin().sendMsg(sender, "cmd_swap_not_supported");
            return null;
        }

        String typeStr = args[1].toLowerCase();
        boolean targetGlowing;
        if (typeStr.equals("glowing") || typeStr.equals("glow") || typeStr.equals("luminoso")) {
            targetGlowing = true;
        } else if (typeStr.equals("normal") || typeStr.equals("default")) {
            targetGlowing = false;
        } else {
            getPlugin().sendMsg(sender, "cmd_swap_invalid_type");
            return null;
        }

        int radius;
        try {
            radius = Integer.parseInt(args[2]);
            if (radius <= 0 || radius > 100) {
                getPlugin().sendMsg(sender, "cmd_swap_invalid_radius");
                return null;
            }
        } catch (NumberFormatException e) {
            getPlugin().sendMsg(sender, "cmd_swap_invalid_radius");
            return null;
        }

        String targetFilename = args.length >= 4 ? args[3] : null;
        Player player = (Player) sender;
        int count = 0;

        List<Entity> nearbyEntities = player.getNearbyEntities(radius, radius, radius);
        
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof ItemFrame)) continue;
            
            ItemFrame oldFrame = (ItemFrame) entity;
            boolean isCurrentlyGlowing = oldFrame instanceof GlowItemFrame;

            // Se já for do tipo desejado, pular
            if (isCurrentlyGlowing == targetGlowing) continue;

            ItemStack item = oldFrame.getItem();
            if (item.getType().name().contains("MAP")) { // Checa se é um mapa
                if (item.getItemMeta() instanceof MapMeta) {
                    MapMeta meta = (MapMeta) item.getItemMeta();
                    if (meta.hasMapView()) {
                        int mapId = meta.getMapView().getId();
                        
                        // Verifica se este mapa pertence ao plugin ImageMaps
                        ImageMap imageMap = getPlugin().getMapById(mapId);
                        
                        if (imageMap != null) {
                            // Filtro por nome de arquivo (opcional)
                            if (targetFilename != null && !imageMap.getFilename().equalsIgnoreCase(targetFilename)) {
                                continue;
                            }

                            // Preparar dados para troca
                            Location loc = oldFrame.getLocation();
                            BlockFace facing = oldFrame.getFacing();
                            Rotation rotation = oldFrame.getRotation();
                            boolean fixed = oldFrame.isFixed();
                            boolean visible = oldFrame.isVisible();

                            // Remover frame antigo
                            oldFrame.remove();

                            // Spawnar novo frame
                            Class<? extends ItemFrame> newClass = targetGlowing ? GlowItemFrame.class : ItemFrame.class;
                            try {
                                ItemFrame newFrame = loc.getWorld().spawn(loc, newClass);
                                newFrame.setFacingDirection(facing);
                                newFrame.setItem(item);
                                newFrame.setRotation(rotation);
                                newFrame.setFixed(fixed);
                                newFrame.setVisible(visible);
                                count++;
                            } catch (IllegalArgumentException ex) {
                                // Falha silenciosa ou log no console se necessário
                                getPlugin().getLogger().warning("Failed to swap frame at " + loc.toString());
                            }
                        }
                    }
                }
            }
        }

        // Obtém o nome traduzido do tipo (Luminoso ou Normal)
        String typeKey = targetGlowing ? "word_glowing" : "word_normal";
        String typeName = getPlugin().getLang().getRawMessage(sender, typeKey);
        
        getPlugin().sendMsg(sender, "cmd_swap_success", count, typeName);
        return null;
    }

    @Override
    public void help(CommandSender sender) {
        getPlugin().sendMsg(sender, "help_swap");
    }
}