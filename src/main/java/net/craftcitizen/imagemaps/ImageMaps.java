package net.craftcitizen.imagemaps;

import com.google.common.io.Files;
import de.craftlancer.core.LambdaRunnable;
import de.craftlancer.core.SemanticVersion;
import de.craftlancer.core.Utils;
import de.craftlancer.core.util.MessageLevel;
import de.craftlancer.core.util.MessageUtil;
import de.craftlancer.core.util.Tuple;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ImageMaps extends JavaPlugin implements Listener {
    private static final String MAPS_YML = "maps.yml";
    private static final String CONFIG_VERSION_KEY = "storageVersion";
    private static final int CONFIG_VERSION = 1;
    private static final long AUTOSAVE_PERIOD = 18000L; // 15 minutes

    public static final String PLACEMENT_METADATA = "imagemaps.place";

    public static final int MAP_WIDTH = 128;
    public static final int MAP_HEIGHT = 128;
    private static final String IMAGES_DIR = "images";

    private Map<String, BufferedImage> imageCache = new HashMap<>();
    private Map<ImageMap, Integer> maps = new HashMap<>();

    private Material toggleItem;
    private float ditherStrength;
    private String ditherAlgorithm;
    
    // Variáveis para o Modo Comparação
    private boolean comparing = false;
    private String compareLeft = "NONE";
    private String compareRight = "CURRENT";
    
    // Gerenciador de Idiomas
    private LanguageManager languageManager;

    static {
        ConfigurationSerialization.registerClass(ImageMap.class);
    }

    @Override
    public void onEnable() {
        // Inicializa o gerenciador de idiomas
        languageManager = new LanguageManager(this);
        
        // Removemos o MessageUtil antigo pois vamos usar nosso sistema de tradução
        /*
        BaseComponent prefix = new TextComponent(new ComponentBuilder("[").color(ChatColor.GRAY).append("ImageMaps")
                                                                          .color(ChatColor.AQUA).append("]")
                                                                          .color(ChatColor.GRAY).create());
        MessageUtil.registerPlugin(this, prefix, ChatColor.GRAY, ChatColor.YELLOW, ChatColor.RED, ChatColor.DARK_RED,
                                   ChatColor.DARK_AQUA);
        */

        if (!new File(getDataFolder(), IMAGES_DIR).exists())
            new File(getDataFolder(), IMAGES_DIR).mkdirs();

        saveDefaultConfig();
        loadConfigValues();

        getCommand("imagemap").setExecutor(new ImageMapCommandHandler(this));
        getServer().getPluginManager().registerEvents(this, this);

        loadMaps();

        new LambdaRunnable(this::saveMaps).runTaskTimer(this, AUTOSAVE_PERIOD, AUTOSAVE_PERIOD);
    }
    
    // Método público para acessar o LanguageManager
    public LanguageManager getLang() {
        return languageManager;
    }
    
    // Método auxiliar para enviar mensagens traduzidas
    public void sendMsg(CommandSender sender, String key, Object... args) {
        sender.sendMessage(languageManager.getMessage(sender, key, args));
    }

    private void loadConfigValues() {
        toggleItem = Material.matchMaterial(getConfig().getString("toggleItem", Material.WOODEN_HOE.name()));
        if (toggleItem == null) {
            toggleItem = Material.WOODEN_HOE;
            getLogger().warning("Given toggleItem is invalid, defaulting to WOODEN_HOE");
        }
        ditherStrength = (float) getConfig().getDouble("ditherStrength", 0.6);
        ditherAlgorithm = getConfig().getString("ditherAlgorithm", "FLOYD").toUpperCase();
    }

    public void reloadPluginConfig(CommandSender sender, Float newStrength, String newAlgorithm) {
        boolean changed = false;

        if (newStrength != null) {
            getConfig().set("ditherStrength", newStrength);
            this.ditherStrength = newStrength;
            changed = true;
        }

        if (newAlgorithm != null) {
            getConfig().set("ditherAlgorithm", newAlgorithm);
            this.ditherAlgorithm = newAlgorithm;
            changed = true;
        }

        if (changed) {
            saveConfig();
            sendMsg(sender, "cmd_config_saved", ditherStrength, ditherAlgorithm);
        } else {
            reloadConfig();
            loadConfigValues();
            languageManager.loadLocales(); // Recarrega idiomas também
            sendMsg(sender, "cmd_config_reloaded", ditherStrength, ditherAlgorithm);
        }
        
        refreshAllMaps();
    }
    
    public void setComparisonMode(boolean active, String left, String right) {
        this.comparing = active;
        this.compareLeft = left;
        this.compareRight = right;
        refreshAllMaps();
    }
    
    public boolean isComparing() { return comparing; }
    public String getCompareLeft() { return compareLeft; }
    public String getCompareRight() { return compareRight; }

    private void refreshAllMaps() {
        for (Integer id : maps.values()) {
            @SuppressWarnings("deprecation")
            MapView map = Bukkit.getMap(id);
            if (map != null) {
                for (MapRenderer r : map.getRenderers()) {
                    if (r instanceof ImageMapRenderer) {
                        ((ImageMapRenderer) r).refresh();
                    }
                }
            }
        }
    }

    @Override
    public void onDisable() {
        saveMaps();
    }
    
    public float getDitherStrength() {
        return ditherStrength;
    }
    
    public String getDitherAlgorithm() {
        return ditherAlgorithm;
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFrameProperty(PlayerInteractEntityEvent event) {
        if (!isInvisibilitySupported())
            return;

        if (event.getRightClicked().getType() != EntityType.ITEM_FRAME
            && (!isGlowingSupported() || event.getRightClicked().getType() != EntityType.GLOW_ITEM_FRAME))
            return;

        ItemFrame frame = (ItemFrame) event.getRightClicked();
        Player p = event.getPlayer();

        if (p.getInventory().getItemInMainHand().getType() != toggleItem)
            return;

        if (p.isSneaking()) {
            if (p.hasPermission("imagemaps.toggleFixed")) {
                event.setCancelled(true);
                frame.setFixed(!frame.isFixed());
                String state = frame.isFixed() ? getLang().getRawMessage(p, "fixed") : getLang().getRawMessage(p, "unfixed");
                sendMsg(p, "toggle_fixed", state);
            }
        }
        else if (p.hasPermission("imagemaps.toggleVisible")) {
            event.setCancelled(true);
            frame.setVisible(!frame.isVisible());
            String state = frame.isVisible() ? getLang().getRawMessage(p, "visible") : getLang().getRawMessage(p, "invisible");
            sendMsg(p, "toggle_visible", state);
        }
    }

    public boolean isInvisibilitySupported() {
        SemanticVersion version = Utils.getMCVersion();
        return version.getMajor() >= 1 && version.getMinor() >= 16;
    }

    public boolean isGlowingSupported() {
        SemanticVersion version = Utils.getMCVersion();
        return version.getMajor() >= 1 && version.getMinor() >= 17;
    }

    public boolean isUpDownFaceSupported() {
        SemanticVersion version = Utils.getMCVersion();

        if (version.getMajor() < 1)
            return false;
        if (version.getMajor() == 1 && version.getMinor() == 14 && version.getRevision() >= 4)
            return true;
        return version.getMinor() > 14;
    }

    public boolean isSetTrackingSupported() {
        SemanticVersion version = Utils.getMCVersion();

        return version.getMajor() >= 1 && version.getMinor() >= 14;
    }

    private void saveMaps() {
        FileConfiguration config = new YamlConfiguration();
        config.set(CONFIG_VERSION_KEY, CONFIG_VERSION);
        
        Map<Integer, ImageMap> saveMap = new HashMap<>();
        for (Entry<ImageMap, Integer> entry : maps.entrySet()) {
            saveMap.put(entry.getValue(), entry.getKey());
        }
        config.set("maps", saveMap);

        BukkitRunnable saveTask = new LambdaRunnable(() -> {
            try {
                config.save(new File(getDataFolder(), MAPS_YML));
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });

        if (isEnabled())
            saveTask.runTaskAsynchronously(this);
        else
            saveTask.run();
    }

    private void loadMaps() {
        File configFile = new File(getDataFolder(), MAPS_YML);

        if (!configFile.exists())
            return;

        Configuration config = YamlConfiguration.loadConfiguration(configFile);
        int version = config.getInt(CONFIG_VERSION_KEY, -1);

        if (version == -1)
            config = convertLegacyMaps(config);

        ConfigurationSection section = config.getConfigurationSection("maps");
        if (section != null)
            section.getValues(false).forEach((a, b) -> {
                int id = Integer.parseInt(a);
                ImageMap imageMap = (ImageMap) b;
                @SuppressWarnings("deprecation")
                MapView map = Bukkit.getMap(id);
                BufferedImage image = getImage(imageMap.getFilename());
                maps.put(imageMap, id);

                if (image == null) {
                    getLogger().warning(() -> "Image file " + imageMap.getFilename() + " not found!");
                    return;
                }
                if (map == null) {
                    getLogger().warning(() -> "Map " + id + " referenced but does not exist!");
                    return;
                }

                if (isSetTrackingSupported())
                    map.setTrackingPosition(false);
                map.getRenderers().forEach(map::removeRenderer);
                map.addRenderer(new ImageMapRenderer(this, image, imageMap.getX(), imageMap.getY(),
                                                     imageMap.getScale()));
            });
    }

    public int cleanupMaps() {
        int start = maps.size();

        maps.entrySet().removeIf(a -> {
            @SuppressWarnings("deprecation")
            MapView map = Bukkit.getMap(a.getValue().intValue());
            BufferedImage image = getImage(a.getKey().getFilename());

            return map == null || image == null;
        });

        return start - maps.size();
    }

    private Configuration convertLegacyMaps(Configuration config) {
        getLogger().info("Converting maps from Version <1.0");

        try {
            Files.copy(new File(getDataFolder(), MAPS_YML), new File(getDataFolder(), MAPS_YML + ".backup"));
        }
        catch (IOException e) {
            getLogger().severe("Failed to backup maps.yml!");
            e.printStackTrace();
        }

        Map<Integer, ImageMap> map = new HashMap<>();

        for (String key : config.getKeys(false)) {
            int id = Integer.parseInt(key);
            String image = config.getString(key + ".image");
            int x = config.getInt(key + ".x") / MAP_WIDTH;
            int y = config.getInt(key + ".y") / MAP_HEIGHT;
            double scale = config.getDouble(key + ".scale", 1.0);
            map.put(id, new ImageMap(image, x, y, scale));
        }

        config = new YamlConfiguration();
        config.set(CONFIG_VERSION_KEY, CONFIG_VERSION);
        config.createSection("maps", map);
        return config;
    }

    public boolean hasImage(String filename) {
        if (imageCache.containsKey(filename.toLowerCase()))
            return true;

        File file = new File(getDataFolder(), IMAGES_DIR + File.separatorChar + filename);

        return file.exists() && getImage(filename) != null;
    }

    public BufferedImage getImage(String filename) {
        if (filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            getLogger().warning("Someone tried to get image with illegal characters in file name.");
            return null;
        }

        if (imageCache.containsKey(filename.toLowerCase()))
            return imageCache.get(filename.toLowerCase());

        File file = new File(getDataFolder(), IMAGES_DIR + File.separatorChar + filename);
        BufferedImage image = null;

        if (!file.exists())
            return null;

        try {
            image = ImageIO.read(file);
            imageCache.put(filename.toLowerCase(), image);
        }
        catch (IOException e) {
            getLogger().log(Level.SEVERE, String.format("Error while trying to read image %s.", file.getName()), e);
        }

        if (image == null)
            getLogger().log(Level.WARNING, () -> String.format("Failed to read file as image %s.", file.getName()));

        return image;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!player.hasMetadata(PLACEMENT_METADATA))
            return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            player.removeMetadata(PLACEMENT_METADATA, this);
            sendMsg(player, "action_cancelled");
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        PlacementData data = (PlacementData) player.getMetadata(PLACEMENT_METADATA).get(0).value();
        PlacementResult result = placeImage(player, event.getClickedBlock(), event.getBlockFace(), data);

        switch (result) {
            case INVALID_FACING:
                sendMsg(player, "error_invalid_facing"); // Adicionar no yml se quiser, ou usar mensagem genérica
                break;
            case INVALID_DIRECTION:
                sendMsg(player, "error_invalid_direction");
                break;
            case EVENT_CANCELLED:
                sendMsg(player, "action_cancelled_plugin");
                break;
            case INSUFFICIENT_SPACE:
                sendMsg(player, "error_insufficient_space");
                break;
            case INSUFFICIENT_WALL:
                sendMsg(player, "error_insufficient_wall");
                break;
            case OVERLAPPING_ENTITY:
                sendMsg(player, "error_overlapping");
                break;
            case SUCCESS:
                break;
        }

        player.removeMetadata(PLACEMENT_METADATA, this);
        event.setCancelled(true);
    }

    private PlacementResult placeImage(Player player, Block block, BlockFace face, PlacementData data) {
        if (!isAxisAligned(face)) {
            getLogger().severe("Someone tried to create an image with an invalid block facing");
            return PlacementResult.INVALID_FACING;
        }

        if (face.getModY() != 0 && !isUpDownFaceSupported())
            return PlacementResult.INVALID_FACING;

        Block b = block.getRelative(face);
        BufferedImage image = getImage(data.getFilename());
        Tuple<Integer, Integer> size = getImageSize(data.getFilename(), data.getSize());
        BlockFace widthDirection = calculateWidthDirection(player, face);
        BlockFace heightDirection = calculateHeightDirection(player, face);

        if (widthDirection == null || heightDirection == null)
            return PlacementResult.INVALID_DIRECTION;

        // check for space
        for (int x = 0; x < size.getKey(); x++)
            for (int y = 0; y < size.getValue(); y++) {
                Block frameBlock = b.getRelative(widthDirection, x).getRelative(heightDirection, y);

                if (!block.getRelative(widthDirection, x).getRelative(heightDirection, y).getType().isSolid())
                    return PlacementResult.INSUFFICIENT_WALL;
                if (frameBlock.getType().isSolid())
                    return PlacementResult.INSUFFICIENT_SPACE;
                if (!b.getWorld().getNearbyEntities(frameBlock.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5,
                                                    Hanging.class::isInstance)
                      .isEmpty())
                    return PlacementResult.OVERLAPPING_ENTITY;
            }

        ImagePlaceEvent event = new ImagePlaceEvent(player, block, widthDirection, heightDirection, size.getKey(),
                                                    size.getValue(), data);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled())
            return PlacementResult.EVENT_CANCELLED;

        // spawn item frame
        for (int x = 0; x < size.getKey(); x++)
            for (int y = 0; y < size.getValue(); y++) {
                Class<? extends ItemFrame> itemFrameClass = data.isGlowing() ? GlowItemFrame.class : ItemFrame.class;
                ItemFrame frame = block.getWorld().spawn(
                                                         b.getRelative(widthDirection, x)
                                                          .getRelative(heightDirection, y).getLocation(),
                                                         itemFrameClass);
                frame.setFacingDirection(face);
                frame.setItem(getMapItem(image, x, y, data));
                frame.setRotation(facingToRotation(heightDirection, widthDirection));

                if (isInvisibilitySupported()) {
                    frame.setFixed(data.isFixed());
                    frame.setVisible(!data.isInvisible());
                }
            }

        return PlacementResult.SUCCESS;
    }

    public boolean deleteImage(String filename) {
        File file = new File(getDataFolder(), IMAGES_DIR + File.separatorChar + filename);

        boolean fileDeleted = false;
        if (file.exists()) {
            fileDeleted = file.delete();
        }

        imageCache.remove(filename.toLowerCase());

        Iterator<Entry<ImageMap, Integer>> it = maps.entrySet().iterator();
        while (it.hasNext()) {
            Entry<ImageMap, Integer> entry = it.next();
            ImageMap imageMap = entry.getKey();
            if (!imageMap.getFilename().equalsIgnoreCase(filename)) {
                continue;
            }

            @SuppressWarnings("deprecation")
            MapView map = Bukkit.getMap(entry.getValue());

            if (map == null) {
                continue;
            }
            map.getRenderers().forEach(map::removeRenderer);
            it.remove();
        }

        saveMaps();
        return fileDeleted;
    }

    @SuppressWarnings("deprecation")
    public boolean reloadImage(String filename) {
        if (!imageCache.containsKey(filename.toLowerCase()))
            return false;

        imageCache.remove(filename.toLowerCase());
        BufferedImage image = getImage(filename);

        if (image == null) {
            getLogger().warning(() -> "Failed to reload image: " + filename);
            return false;
        }
        
        for (Entry<ImageMap, Integer> entry : maps.entrySet()) {
            if (!entry.getKey().getFilename().equalsIgnoreCase(filename)) continue;
            
            MapView map = Bukkit.getMap(entry.getValue());
            if (map == null) continue;
            
            for (MapRenderer renderer : map.getRenderers()) {
                if (renderer instanceof ImageMapRenderer) {
                    ((ImageMapRenderer) renderer).recalculateInput(image);
                }
            }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private ItemStack getMapItem(BufferedImage image, int x, int y, PlacementData data) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);

        ImageMap imageMap = new ImageMap(data.getFilename(), x, y, getScale(image, data.getSize()));
        if (maps.containsKey(imageMap)) {
            MapMeta meta = (MapMeta) item.getItemMeta();
            meta.setMapId(maps.get(imageMap));
            item.setItemMeta(meta);
            return item;
        }

        MapView map = getServer().createMap(getServer().getWorlds().get(0));
        map.getRenderers().forEach(map::removeRenderer);
        map.addRenderer(new ImageMapRenderer(this, image, x, y, getScale(image, data.getSize())));
        if (isSetTrackingSupported())
            map.setTrackingPosition(false);

        MapMeta meta = ((MapMeta) item.getItemMeta());
        meta.setMapView(map);
        item.setItemMeta(meta);
        maps.put(imageMap, map.getId());

        return item;
    }

    public Tuple<Integer, Integer> getImageSize(String filename, Tuple<Integer, Integer> size) {
        BufferedImage image = getImage(filename);

        if (image == null)
            return new Tuple<>(0, 0);

        double finalScale = getScale(image, size);
        int finalX = (int) ((MAP_WIDTH - 1 + Math.ceil(image.getWidth() * finalScale)) / MAP_WIDTH);
        int finalY = (int) ((MAP_HEIGHT - 1 + Math.ceil(image.getHeight() * finalScale)) / MAP_HEIGHT);

        return new Tuple<>(finalX, finalY);
    }

    public double getScale(String filename, Tuple<Integer, Integer> size) {
        return getScale(getImage(filename), size);
    }

    public double getScale(BufferedImage image, Tuple<Integer, Integer> size) {
        if (image == null)
            return 1.0;

        int baseX = image.getWidth();
        int baseY = image.getHeight();

        double finalScale = 1D;

        if (size != null) {
            int targetX = size.getKey() * MAP_WIDTH;
            int targetY = size.getValue() * MAP_HEIGHT;

            double scaleX = size.getKey() > 0 ? (double) targetX / baseX : Double.MAX_VALUE;
            double scaleY = size.getValue() > 0 ? (double) targetY / baseY : Double.MAX_VALUE;

            finalScale = Math.min(scaleX, scaleY);
            if (finalScale >= Double.MAX_VALUE)
                finalScale = 1D;
        }

        return finalScale;
    }

    private static Rotation facingToRotation(BlockFace heightDirection, BlockFace widthDirection) {
        switch (heightDirection) {
            case WEST:
                return Rotation.CLOCKWISE_45;
            case NORTH:
                return widthDirection == BlockFace.WEST ? Rotation.CLOCKWISE : Rotation.NONE;
            case EAST:
                return Rotation.CLOCKWISE_135;
            case SOUTH:
                return widthDirection == BlockFace.WEST ? Rotation.CLOCKWISE : Rotation.NONE;
            default:
                return Rotation.NONE;
        }
    }

    private static BlockFace calculateWidthDirection(Player player, BlockFace face) {
        float yaw = (360.0f + player.getLocation().getYaw()) % 360.0f;
        switch (face) {
            case NORTH:
                return BlockFace.WEST;
            case SOUTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.NORTH;
            case WEST:
                return BlockFace.SOUTH;
            case UP:
            case DOWN:
                if (Utils.isBetween(yaw, 45.0, 135.0))
                    return BlockFace.NORTH;
                else if (Utils.isBetween(yaw, 135.0, 225.0))
                    return BlockFace.EAST;
                else if (Utils.isBetween(yaw, 225.0, 315.0))
                    return BlockFace.SOUTH;
                else
                    return BlockFace.WEST;
            default:
                return null;
        }
    }

    private static BlockFace calculateHeightDirection(Player player, BlockFace face) {
        float yaw = (360.0f + player.getLocation().getYaw()) % 360.0f;
        switch (face) {
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
                return BlockFace.DOWN;
            case UP:
                if (Utils.isBetween(yaw, 45.0, 135.0))
                    return BlockFace.EAST;
                else if (Utils.isBetween(yaw, 135.0, 225.0))
                    return BlockFace.SOUTH;
                else if (Utils.isBetween(yaw, 225.0, 315.0))
                    return BlockFace.WEST;
                else
                    return BlockFace.NORTH;
            case DOWN:
                if (Utils.isBetween(yaw, 45.0, 135.0))
                    return BlockFace.WEST;
                else if (Utils.isBetween(yaw, 135.0, 225.0))
                    return BlockFace.NORTH;
                else if (Utils.isBetween(yaw, 225.0, 315.0))
                    return BlockFace.EAST;
                else
                    return BlockFace.SOUTH;
            default:
                return null;
        }
    }

    private static boolean isAxisAligned(BlockFace face) {
        switch (face) {
            case DOWN:
            case UP:
            case WEST:
            case EAST:
            case SOUTH:
            case NORTH:
                return true;
            default:
                return false;
        }
    }
}