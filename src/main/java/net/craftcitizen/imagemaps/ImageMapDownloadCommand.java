package net.craftcitizen.imagemaps;

import de.craftlancer.core.LambdaRunnable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class ImageMapDownloadCommand extends ImageMapSubCommand {

    public ImageMapDownloadCommand(ImageMaps plugin) {
        super("imagemaps.download", plugin, true);
    }

    @Override
    protected String execute(CommandSender sender, Command cmd, String label, String[] args) {
        if (!checkSender(sender)) {
            getPlugin().sendMsg(sender, "cmd_no_permission");
            return null;
        }

        if (args.length < 3) {
            // Em vez de mensagem fixa, usamos a chave do yml
            // Mensagem original: "You must specify a file name and a download link."
            getPlugin().sendMsg(sender, "cmd_must_specify_filename"); // Adaptado, ou crie uma chave específica se quiser precisão
            return null;
        }

        String filename = args[1];
        String url = args[2];

        if (filename.contains("/") || filename.contains("\\") || filename.contains(":")) {
            getPlugin().sendMsg(sender, "cmd_filename_illegal");
            return null;
        }

        new LambdaRunnable(() -> download(sender, url, filename)).runTaskAsynchronously(plugin);
        return null;
    }

    private void download(CommandSender sender, String input, String filename) {
        try {
            URL srcURL = new URL(input);

            if (!srcURL.getProtocol().startsWith("http")) {
                getPlugin().sendMsg(sender, "cmd_download_invalid_url");
                return;
            }

            URLConnection connection = srcURL.openConnection();

            if (!(connection instanceof HttpURLConnection)) {
                getPlugin().sendMsg(sender, "cmd_download_invalid_url");
                return;
            }

            connection.setRequestProperty("User-Agent", "ImageMaps/0");

            if (((HttpURLConnection) connection).getResponseCode() != 200) {
                getPlugin().sendMsg(sender, "cmd_download_failed", ((HttpURLConnection) connection).getResponseCode());
                return;
            }

            String mimeType = connection.getHeaderField("Content-type");
            if (!(mimeType.startsWith("image/"))) {
                getPlugin().sendMsg(sender, "cmd_download_not_image", mimeType);
                return;
            }

            try (InputStream str = connection.getInputStream()) {
                BufferedImage image = ImageIO.read(str);
                if (image == null) {
                    getPlugin().sendMsg(sender, "cmd_download_not_image_content");
                    return;
                }

                File outFile = new File(plugin.getDataFolder(), "images" + File.separatorChar + filename);
                boolean fileExisted = outFile.exists();
                ImageIO.write(image, "PNG", outFile);

                if (fileExisted) {
                    getPlugin().sendMsg(sender, "cmd_download_exists");
                    getPlugin().reloadImage(filename);
                }
            }
            catch (IllegalArgumentException ex) {
                getPlugin().sendMsg(sender, "cmd_download_failed", -1);
                return;
            }
            getPlugin().sendMsg(sender, "cmd_download_complete");
        }
        catch (MalformedURLException ex) {
            getPlugin().sendMsg(sender, "cmd_download_invalid_url");
        }
        catch (IOException ex) {
            // Em caso de erro técnico, pode manter log no console ou mensagem genérica
            ex.printStackTrace();
            getPlugin().sendMsg(sender, "cmd_download_failed", 0);
        }
    }

    @Override
    public void help(CommandSender sender) {
        // Você pode adicionar chaves de help no yml se quiser traduzir o help também
    }
}