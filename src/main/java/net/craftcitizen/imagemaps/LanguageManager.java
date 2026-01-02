package net.craftcitizen.imagemaps;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import net.md_5.bungee.api.ChatColor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

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

        // Tenta carregar e atualizar os arquivos padrão
        updateAndLoadLocale("en_us");
        updateAndLoadLocale("pt_br");

        // Carrega quaisquer outros arquivos .yml na pasta lang
        File[] files = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String localeName = file.getName().replace(".yml", "").toLowerCase();
                // Se já carregamos via updateAndLoadLocale, pula
                if (locales.containsKey(localeName)) continue;
                
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                locales.put(localeName, config);
            }
        }
    }

    private void updateAndLoadLocale(String localeName) {
        String resourcePath = "lang/" + localeName + ".yml";
        File file = new File(plugin.getDataFolder(), resourcePath);

        // 1. Se não existe, salva o padrão do .jar
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }

        // 2. Carrega o arquivo do disco
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // 3. Verifica chaves faltantes comparando com o arquivo interno do .jar
        InputStream internalStream = plugin.getResource(resourcePath);
        if (internalStream != null) {
            boolean changed = false;
            YamlConfiguration internalConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(internalStream, StandardCharsets.UTF_8));
            
            Set<String> keys = internalConfig.getKeys(true); // Pega todas as chaves, incluindo aninhadas
            for (String key : keys) {
                if (!config.contains(key)) {
                    config.set(key, internalConfig.get(key));
                    changed = true;
                }
            }

            // 4. Se houve mudanças (novas chaves adicionadas), salva o arquivo
            if (changed) {
                try {
                    config.save(file);
                    plugin.getLogger().info("Updated language file: " + localeName + ".yml with missing keys.");
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save updated language file: " + localeName + ".yml", e);
                }
            }
        }

        locales.put(localeName, config);
    }

    public String getMessage(CommandSender sender, String key, Object... args) {
        String locale = defaultLocale;
        
        if (sender instanceof Player) {
            try {
                // Tenta pegar o locale do jogador via Spigot API (1.12+)
                // O método getLocale() retorna ex: "en_US", "pt_BR" -> convertemos para "en_us"
                locale = ((Player) sender).getLocale().toLowerCase();
            } catch (NoSuchMethodError e) {
                // Fallback para versões muito antigas
                locale = defaultLocale;
            }
        }

        // Tenta pegar a config do idioma exato
        YamlConfiguration config = locales.get(locale);
        
        // Se não tiver o idioma exato, tenta fallback para o padrão
        if (config == null) {
            config = locales.get(defaultLocale);
        }

        // Se CRITICAMENTE não tiver nem o padrão carregado
        if (config == null) {
            return "Error: Language system critical failure for key " + key;
        }

        String message = config.getString(key);
        
        // Se a chave não existir no idioma do jogador, tenta no default (fallback de chave)
        if (message == null && !locale.equals(defaultLocale)) {
            YamlConfiguration defaultConfig = locales.get(defaultLocale);
            if (defaultConfig != null) {
                message = defaultConfig.getString(key);
            }
        }

        // Se ainda for nulo, tenta buscar direto do recurso interno como último recurso
        if (message == null) {
            return "Missing translation: " + key;
        }

        // Formata cores
        message = ChatColor.translateAlternateColorCodes('&', message);
        
        // Formata argumentos (String.format)
        if (args.length > 0) {
            try {
                message = String.format(message, args);
            } catch (Exception e) {
                // Loga erro mas retorna mensagem sem formatação para não crashar
                plugin.getLogger().warning("Error formatting message key: " + key + " with args: " + java.util.Arrays.toString(args));
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
             try {
                locale = ((Player) sender).getLocale().toLowerCase();
            } catch (Exception e) {
                locale = defaultLocale;
            }
        }
        
        YamlConfiguration config = locales.get(locale);
        if (config == null) config = locales.get(defaultLocale);
        if (config == null) return key;
        
        String msg = config.getString(key);
        if (msg == null && !locale.equals(defaultLocale)) {
             YamlConfiguration defConfig = locales.get(defaultLocale);
             if (defConfig != null) msg = defConfig.getString(key);
        }
        
        return msg != null ? ChatColor.translateAlternateColorCodes('&', msg) : key;
    }
}