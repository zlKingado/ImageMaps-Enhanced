package net.craftcitizen.imagemaps;

import de.craftlancer.core.LambdaRunnable;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.entity.Player;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.Color;

public class ImageMapRenderer extends MapRenderer {
    private ImageMaps plugin;

    private BufferedImage image = null;
    private boolean shouldRender = true;

    private final int x;
    private final int y;
    private final double scale;
    
    // Armazena a largura total da imagem original para cálculos globais
    private int totalImageWidth = 0;

    public ImageMapRenderer(ImageMaps plugin, BufferedImage image, int x, int y, double scale) {
        this.plugin = plugin;
        this.x = x;
        this.y = y;
        this.scale = scale;
        recalculateInput(image);
    }

    public void recalculateInput(BufferedImage input) {
        // Salva a largura total da imagem original antes de recortar
        this.totalImageWidth = (int) Math.round(input.getWidth() * scale);

        if (x * ImageMaps.MAP_WIDTH > Math.round(input.getWidth() * scale)
            || y * ImageMaps.MAP_HEIGHT > Math.round(input.getHeight() * scale))
            return;

        int x1 = (int) Math.floor(x * ImageMaps.MAP_WIDTH / scale);
        int y1 = (int) Math.floor(y * ImageMaps.MAP_HEIGHT / scale);

        int x2 = (int) Math.ceil(Math.min(input.getWidth(), ((x + 1) * ImageMaps.MAP_WIDTH / scale)));
        int y2 = (int) Math.ceil(Math.min(input.getHeight(), ((y + 1) * ImageMaps.MAP_HEIGHT / scale)));

        if (x2 - x1 <= 0 || y2 - y1 <= 0)
            return;

        this.image = input.getSubimage(x1, y1, x2 - x1, y2 - y1);

        if (scale != 1D) {
            BufferedImage resized = new BufferedImage(ImageMaps.MAP_WIDTH, ImageMaps.MAP_HEIGHT,
                                                      input.getType() == 0 ? image.getType() : input.getType());
            AffineTransform at = new AffineTransform();
            at.scale(scale, scale);
            AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
            this.image = scaleOp.filter(this.image, resized);
        }

        shouldRender = true;
    }

    public void refresh() {
        this.shouldRender = true;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (image != null && shouldRender) {
            new LambdaRunnable(() -> {
                ditherImage(canvas, image);
            }).runTaskLater(plugin, System.nanoTime() % 60);
            
            shouldRender = false;
        }
    }

    private void ditherImage(MapCanvas canvas, BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        
        float[][][] buffer = new float[width][height][3];
        
        boolean debugging = plugin.isComparing();
        
        // Ponto de divisão GLOBAL (metade da imagem inteira na parede)
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
                
                // Calcula a posição X absoluta deste pixel na imagem completa
                // (Índice do Mapa * 128) + Pixel Atual
                int globalX = (this.x * ImageMaps.MAP_WIDTH) + i;

                // Se estiver comparando, reseta erro na linha divisória global
                if (debugging && globalX == globalSplitX) {
                    buffer[i][j][0] = (img.getRGB(i,j) >> 16) & 0xFF;
                    buffer[i][j][1] = (img.getRGB(i,j) >> 8) & 0xFF;
                    buffer[i][j][2] = img.getRGB(i,j) & 0xFF;
                }

                int oldR = clamp(Math.round(buffer[i][j][0]));
                int oldG = clamp(Math.round(buffer[i][j][1]));
                int oldB = clamp(Math.round(buffer[i][j][2]));
                
                int pixelRGB = img.getRGB(i, j);
                int alpha = (pixelRGB >> 24) & 0xFF;
                
                if (alpha < 128) {
                    canvas.setPixel(i, j, (byte) 0);
                    continue;
                }

                // @SuppressWarnings("deprecation")
                byte index = MapPalette.matchColor(oldR, oldG, oldB);
                canvas.setPixel(i, j, index);
                
                // Desenha a linha preta guia EXATAMENTE no meio da imagem completa
                if (debugging && globalX == globalSplitX) {
                    canvas.setPixel(i, j, (byte) 119); 
                    continue;
                }

                String activeAlgo;
                float activeStrength;
                
                if (debugging) {
                    // Decide o lado baseado na coordenada GLOBAL
                    String selection = (globalX < globalSplitX) ? plugin.getCompareLeft() : plugin.getCompareRight();
                    
                    if (selection.equals("NONE")) {
                        continue; 
                    } else if (selection.equals("CURRENT")) {
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

                Color paletteColor = MapPalette.getColor(index);
                int newR = paletteColor.getRed();
                int newG = paletteColor.getGreen();
                int newB = paletteColor.getBlue();

                float errR = (oldR - newR) * activeStrength;
                float errG = (oldG - newG) * activeStrength;
                float errB = (oldB - newB) * activeStrength;

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