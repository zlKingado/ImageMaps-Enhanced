package net.craftcitizen.imagemaps;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import net.md_5.bungee.api.ChatColor;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final ImageMaps plugin;
    private final Map<String, YamlConfiguration> locales = new HashMap<>();
    private final String defaultLocale = "en_us";

    public LanguageManager(ImageMaps plugin) {
        this.plugin = plugin;
        loadLocales();
    }

    public void loadLocales() {
        locales.clear();
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // Salva os recursos padrão se não existirem
        saveResourceIfNotExists("lang/en_us.yml");
        saveResourceIfNotExists("lang/pt_br.yml");

        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String localeName = file.getName().replace(".yml", "").toLowerCase();
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                locales.put(localeName, config);
            }
        }
    }

    private void saveResourceIfNotExists(String resourcePath) {
        if (plugin.getResource(resourcePath) != null) {
            File outFile = new File(plugin.getDataFolder(), resourcePath);
            if (!outFile.exists()) {
                plugin.saveResource(resourcePath, false);
            }
        }
    }

    public String getMessage(CommandSender sender, String key, Object... args) {
        String locale = defaultLocale;
        
        if (sender instanceof Player) {
            // Obtém o locale do jogador (ex: pt_br)
            locale = ((Player) sender).getLocale().toLowerCase();
        }

        // Tenta pegar a config do idioma exato, senão cai pro default
        YamlConfiguration config = locales.getOrDefault(locale, locales.get(defaultLocale));
        
        // Se ainda for nulo (caso crítico onde nem o default carregou), evita crash
        if (config == null) return "Error: Language file missing for key " + key;

        String message = config.getString(key);
        
        // Se a chave não existir no idioma do jogador, tenta no default (fallback)
        if (message == null && !locale.equals(defaultLocale)) {
            config = locales.get(defaultLocale);
            if (config != null) message = config.getString(key);
        }

        if (message == null) return "Missing translation: " + key;

        // Formata cores e argumentos
        message = ChatColor.translateAlternateColorCodes('&', message);
        if (args.length > 0) {
            try {
                message = String.format(message, args);
            } catch (Exception e) {
                // Previne crash se os argumentos não baterem
                e.printStackTrace();
            }
        }
        
        // Adiciona prefixo se a mensagem não for vazia
        String prefix = config.getString("prefix", "&7[&bImageMaps&7] ");
        prefix = ChatColor.translateAlternateColorCodes('&', prefix);
        
        return prefix + message;
    }
    
    // Método auxiliar para pegar mensagem crua (sem prefixo), útil para GUIs ou mensagens compostas
    public String getRawMessage(CommandSender sender, String key) {
        String locale = defaultLocale;
        if (sender instanceof Player) {
            locale = ((Player) sender).getLocale().toLowerCase();
        }
        YamlConfiguration config = locales.getOrDefault(locale, locales.get(defaultLocale));
        if (config == null) return key;
        
        String msg = config.getString(key);
        if (msg == null && !locale.equals(defaultLocale)) {
             msg = locales.get(defaultLocale).getString(key);
        }
        return msg != null ? ChatColor.translateAlternateColorCodes('&', msg) : key;
    }
}