package net.craftcitizen.imagemaps;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class ImageMapRenderer extends MapRenderer {
    private final ImageMaps plugin;
    private final int x;
    private final int y;
    private final double scale;
    
    // OTIMIZAÇÃO: Cache de pixels processados (Bytes de cor do Minecraft)
    private byte[] cachedPixels = null;
    private boolean needsUpdate = true;
    
    private int totalImageWidth = 0;

    public ImageMapRenderer(ImageMaps plugin, BufferedImage image, int x, int y, double scale) {
        super(true); 
        this.plugin = plugin;
        this.x = x;
        this.y = y;
        this.scale = scale;
        recalculateInput(image);
    }

    public void recalculateInput(BufferedImage input) {
        if (input == null) return;

        this.totalImageWidth = (int) Math.round(input.getWidth() * scale);

        int startX = (int) Math.floor(x * ImageMaps.MAP_WIDTH / scale);
        int startY = (int) Math.floor(y * ImageMaps.MAP_HEIGHT / scale);

        if (startX >= input.getWidth() || startY >= input.getHeight()) return;

        int endX = (int) Math.ceil(Math.min(input.getWidth(), ((x + 1) * ImageMaps.MAP_WIDTH / scale)));
        int endY = (int) Math.ceil(Math.min(input.getHeight(), ((y + 1) * ImageMaps.MAP_HEIGHT / scale)));

        int subWidth = endX - startX;
        int subHeight = endY - startY;

        if (subWidth <= 0 || subHeight <= 0) return;

        BufferedImage subImage = input.getSubimage(startX, startY, subWidth, subHeight);

        BufferedImage finalImage = subImage;
        if (scale != 1.0) {
            BufferedImage resized = new BufferedImage(ImageMaps.MAP_WIDTH, ImageMaps.MAP_HEIGHT,
                    input.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : input.getType());
            AffineTransform at = new AffineTransform();
            at.scale(scale, scale);
            AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
            finalImage = scaleOp.filter(subImage, resized);
        }

        // Limpa cache anterior explicitamente
        this.cachedPixels = null;
        // Gera novos pixels
        this.cachedPixels = ditherImage(finalImage);
        // Marca para renderização
        this.needsUpdate = true;
    }

    public void refresh() {
        this.needsUpdate = true;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        // Só desenha se houver dados E se precisar de atualização (economiza CPU)
        if (cachedPixels != null && needsUpdate) {
            for (int i = 0; i < cachedPixels.length; i++) {
                int px = i % ImageMaps.MAP_WIDTH;
                int py = i / ImageMaps.MAP_WIDTH;
                canvas.setPixel(px, py, cachedPixels[i]);
            }
            needsUpdate = false;
        }
    }

    private byte[] ditherImage(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        byte[] pixels = new byte[ImageMaps.MAP_WIDTH * ImageMaps.MAP_HEIGHT];
        
        Arrays.fill(pixels, (byte) 0);

        float[][][] buffer = new float[width][height][3];
        boolean debugging = plugin.isComparing();
        int globalSplitX = totalImageWidth / 2;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = img.getRGB(i, j);
                buffer[i][j][0] = (rgb >> 16) & 0xFF;
                buffer[i][j][1] = (rgb >> 8) & 0xFF;
                buffer[i][j][2] = rgb & 0xFF;
            }
        }

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (i >= ImageMaps.MAP_WIDTH || j >= ImageMaps.MAP_HEIGHT) continue;

                int globalX = (this.x * ImageMaps.MAP_WIDTH) + i;

                if (debugging && globalX == globalSplitX) {
                    // Linha preta divisória no modo debug
                    int arrayIndex = j * ImageMaps.MAP_WIDTH + i;
                    pixels[arrayIndex] = (byte) 119; 
                    continue;
                }

                // Pega cor do buffer (que pode ter erro acumulado de pixels anteriores)
                int oldR = clamp(Math.round(buffer[i][j][0]));
                int oldG = clamp(Math.round(buffer[i][j][1]));
                int oldB = clamp(Math.round(buffer[i][j][2]));
                
                int pixelRGB = img.getRGB(i, j);
                int alpha = (pixelRGB >> 24) & 0xFF;
                
                int arrayIndex = j * ImageMaps.MAP_WIDTH + i;

                if (alpha < 128) {
                    pixels[arrayIndex] = (byte) 0;
                    continue;
                }

                // Encontra a cor mais próxima na paleta do Minecraft
                @SuppressWarnings("deprecation")
                byte index = MapPalette.matchColor(oldR, oldG, oldB);
                
                pixels[arrayIndex] = index;

                // Define qual algoritmo usar neste pixel
                String activeAlgo;
                float activeStrength;
                
                if (debugging) {
                    String selection = (globalX < globalSplitX) ? plugin.getCompareLeft() : plugin.getCompareRight();
                    
                    if (selection.equals("NONE")) {
                         pixels[arrayIndex] = (byte) 0;
                         continue;
                    }
                    
                    if (selection.equals("CURRENT")) {
                        activeAlgo = plugin.getDitherAlgorithm();
                        activeStrength = plugin.getDitherStrength();
                    } else {
                        activeAlgo = selection;
                        activeStrength = plugin.getDitherStrength();
                    }
                } else {
                    activeAlgo = plugin.getDitherAlgorithm();
                    activeStrength = plugin.getDitherStrength();
                }

                // Se for RAW, paramos aqui! Não calcula erro nem distribui.
                if (activeAlgo.equals("RAW")) {
                    continue;
                }

                // Calcula erro de quantização (diferença entre cor original e cor da paleta)
                Color paletteColor = MapPalette.getColor(index);
                int newR = paletteColor.getRed();
                int newG = paletteColor.getGreen();
                int newB = paletteColor.getBlue();

                float errR = (oldR - newR) * activeStrength;
                float errG = (oldG - newG) * activeStrength;
                float errB = (oldB - newB) * activeStrength;

                // Distribui o erro para vizinhos
                switch (activeAlgo) {
                    case "ATKINSON":
                        distribute(buffer, i + 1, j, errR, errG, errB, 1.0f/8.0f, width, height);
                        distribute(buffer, i + 2, j, errR, errG, errB, 1.0f/8.0f, width, height);
                        distribute(buffer, i - 1, j + 1, errR, errG, errB, 1.0f/8.0f, width, height);
                        distribute(buffer, i, j + 1, errR, errG, errB, 1.0f/8.0f, width, height);
                        distribute(buffer, i + 1, j + 1, errR, errG, errB, 1.0f/8.0f, width, height);
                        distribute(buffer, i, j + 2, errR, errG, errB, 1.0f/8.0f, width, height);
                        break;
                    case "BURKES":
                        distribute(buffer, i + 1, j, errR, errG, errB, 8.0f/32.0f, width, height);
                        distribute(buffer, i + 2, j, errR, errG, errB, 4.0f/32.0f, width, height);
                        distribute(buffer, i - 2, j + 1, errR, errG, errB, 2.0f/32.0f, width, height);
                        distribute(buffer, i - 1, j + 1, errR, errG, errB, 4.0f/32.0f, width, height);
                        distribute(buffer, i, j + 1, errR, errG, errB, 8.0f/32.0f, width, height);
                        distribute(buffer, i + 1, j + 1, errR, errG, errB, 4.0f/32.0f, width, height);
                        distribute(buffer, i + 2, j + 1, errR, errG, errB, 2.0f/32.0f, width, height);
                        break;
                    case "SIERRA":
                        distribute(buffer, i + 1, j, errR, errG, errB, 2.0f/4.0f, width, height);
                        distribute(buffer, i - 1, j + 1, errR, errG, errB, 1.0f/4.0f, width, height);
                        distribute(buffer, i, j + 1, errR, errG, errB, 1.0f/4.0f, width, height);
                        break;
                    case "FLOYD":
                    default:
                        distribute(buffer, i + 1, j, errR, errG, errB, 7.0f/16.0f, width, height);
                        distribute(buffer, i - 1, j + 1, errR, errG, errB, 3.0f/16.0f, width, height);
                        distribute(buffer, i, j + 1, errR, errG, errB, 5.0f/16.0f, width, height);
                        distribute(buffer, i + 1, j + 1, errR, errG, errB, 1.0f/16.0f, width, height);
                        break;
                }
            }
        }
        return pixels;
    }

    private void distribute(float[][][] buffer, int x, int y, float errR, float errG, float errB, float factor, int width, int height) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            buffer[x][y][0] += errR * factor;
            buffer[x][y][1] += errG * factor;
            buffer[x][y][2] += errB * factor;
        }
    }

    private int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}