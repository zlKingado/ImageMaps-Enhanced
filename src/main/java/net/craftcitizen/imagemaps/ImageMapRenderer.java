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
import java.util.HashMap;
import java.util.Map;

public class ImageMapRenderer extends MapRenderer {
    private final ImageMaps plugin;
    private final int x;
    private final int y;
    private final double scale;
    
    // Cache de pixels processados (Bytes de cor do Minecraft)
    private byte[] cachedPixels = null;
    private boolean needsUpdate = true;

    // === SISTEMA DE CORES AVANÇADO ===
    // Armazena todas as cores disponíveis na versão atual do servidor
    private static final Color[] MINECRAFT_PALETTE = new Color[256];
    private static final boolean[] PALETTE_VALID = new boolean[256];
    
    // Cache reverso para acelerar a busca (RGB -> Byte MC)
    // Limpa automaticamente se ficar muito grande para evitar vazamento de memória
    private static final Map<Integer, Byte> COLOR_CACHE = new HashMap<>(4096);

    static {
        // Inicialização Dinâmica: Escaneia a API do Bukkit para descobrir todas as cores suportadas.
        // Isso garante que se o Minecraft atualizar e adicionar novas cores, o plugin suportará automaticamente.
        try {
            for (int i = 0; i < 256; i++) {
                try {
                    // Pega a cor oficial que o Spigot reporta para este ID
                    @SuppressWarnings("deprecation")
                    Color c = MapPalette.getColor((byte) i);
                    
                    // Cores muito transparentes ou nulas são ignoradas
                    if (c != null && c.getAlpha() > 128) {
                        MINECRAFT_PALETTE[i] = c;
                        PALETTE_VALID[i] = true;
                    } else {
                        PALETTE_VALID[i] = false;
                    }
                } catch (Throwable t) {
                    PALETTE_VALID[i] = false;
                }
            }
            // Garante que o transparente (0) seja inválido para busca, pois tratamos alpha separadamente
            PALETTE_VALID[0] = false; 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        
        String algo = plugin.getDitherAlgorithm();
        float strength = plugin.getDitherStrength();
        boolean isComparing = plugin.isComparing();
        String leftAlgo = plugin.getCompareLeft();
        String rightAlgo = plugin.getCompareRight();

        this.cachedPixels = calculatePixels(input, x, y, scale, algo, strength, isComparing, leftAlgo, rightAlgo);
        this.needsUpdate = true;
    }

    public void setPixels(byte[] pixels) {
        this.cachedPixels = pixels;
        this.needsUpdate = true;
    }

    public static byte[] calculatePixels(BufferedImage input, int x, int y, double scale, 
                                         String algorithm, float strength, 
                                         boolean comparing, String leftAlgo, String rightAlgo) {
        if (input == null) return null;

        int totalImageWidth = (int) Math.round(input.getWidth() * scale);

        int startX = (int) Math.floor(x * ImageMaps.MAP_WIDTH / scale);
        int startY = (int) Math.floor(y * ImageMaps.MAP_HEIGHT / scale);

        if (startX >= input.getWidth() || startY >= input.getHeight()) return null;

        int endX = (int) Math.ceil(Math.min(input.getWidth(), ((x + 1) * ImageMaps.MAP_WIDTH / scale)));
        int endY = (int) Math.ceil(Math.min(input.getHeight(), ((y + 1) * ImageMaps.MAP_HEIGHT / scale)));

        int subWidth = endX - startX;
        int subHeight = endY - startY;

        if (subWidth <= 0 || subHeight <= 0) return null;

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

        // Limpa o cache periodicamente para evitar uso excessivo de RAM em operações longas
        if (COLOR_CACHE.size() > 10000) COLOR_CACHE.clear();

        return ditherImage(finalImage, x, totalImageWidth, algorithm, strength, comparing, leftAlgo, rightAlgo);
    }

    public void refresh() {
        this.needsUpdate = true;
    }

    @Override
    public void render(MapView view, MapCanvas canvas, Player player) {
        if (cachedPixels != null && needsUpdate) {
            for (int i = 0; i < cachedPixels.length; i++) {
                int px = i % ImageMaps.MAP_WIDTH;
                int py = i / ImageMaps.MAP_WIDTH;
                canvas.setPixel(px, py, cachedPixels[i]);
            }
            needsUpdate = false;
        }
    }

    private static byte[] ditherImage(BufferedImage img, int mapX, int totalWidth, 
                                      String defaultAlgo, float defaultStrength,
                                      boolean debugging, String leftAlgo, String rightAlgo) {
        int width = img.getWidth();
        int height = img.getHeight();
        byte[] pixels = new byte[ImageMaps.MAP_WIDTH * ImageMaps.MAP_HEIGHT];
        
        Arrays.fill(pixels, (byte) 0);

        float[][][] buffer = new float[width][height][3];
        int globalSplitX = totalWidth / 2;

        // Carrega a imagem no buffer float para cálculos de erro
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int rgb = img.getRGB(i, j);
                buffer[i][j][0] = (rgb >> 16) & 0xFF; // R
                buffer[i][j][1] = (rgb >> 8) & 0xFF;  // G
                buffer[i][j][2] = rgb & 0xFF;         // B
            }
        }

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                if (i >= ImageMaps.MAP_WIDTH || j >= ImageMaps.MAP_HEIGHT) continue;

                int globalX = (mapX * ImageMaps.MAP_WIDTH) + i;

                // Linha divisória de Debug
                if (debugging && globalX == globalSplitX) {
                    int arrayIndex = j * ImageMaps.MAP_WIDTH + i;
                    pixels[arrayIndex] = (byte) 119; // Preto (geralmente id 119 no MC moderno)
                    continue;
                }

                // Pega a cor atual do buffer (que pode ter acumulado erro de pixels anteriores)
                int oldR = clamp(Math.round(buffer[i][j][0]));
                int oldG = clamp(Math.round(buffer[i][j][1]));
                int oldB = clamp(Math.round(buffer[i][j][2]));
                
                int pixelRGB = img.getRGB(i, j);
                int alpha = (pixelRGB >> 24) & 0xFF;
                
                int arrayIndex = j * ImageMaps.MAP_WIDTH + i;

                if (alpha < 128) {
                    pixels[arrayIndex] = (byte) 0; // Transparente
                    continue;
                }

                // === NOVA LÓGICA DE COR ===
                // Busca a cor mais próxima na paleta completa (0-255)
                byte index = findBestColor(oldR, oldG, oldB);
                
                pixels[arrayIndex] = index;

                // Seleção de Algoritmo para Comparação
                String activeAlgo;
                float activeStrength = defaultStrength;
                
                if (debugging) {
                    String selection = (globalX < globalSplitX) ? leftAlgo : rightAlgo;
                    
                    if (selection.equals("NONE")) {
                         pixels[arrayIndex] = (byte) 0;
                         continue;
                    }
                    if (selection.equals("CURRENT")) {
                        activeAlgo = defaultAlgo;
                    } else {
                        activeAlgo = selection;
                    }
                } else {
                    activeAlgo = defaultAlgo;
                }

                if (activeAlgo.equals("RAW")) {
                    continue;
                }

                // Calcula o erro baseado na cor que o Minecraft REALMENTE vai mostrar
                Color paletteColor = MINECRAFT_PALETTE[Byte.toUnsignedInt(index)];
                // Fallback seguro caso a paleta tenha buracos (raro)
                if (paletteColor == null) paletteColor = new Color(oldR, oldG, oldB);

                int newR = paletteColor.getRed();
                int newG = paletteColor.getGreen();
                int newB = paletteColor.getBlue();

                float errR = (oldR - newR) * activeStrength;
                float errG = (oldG - newG) * activeStrength;
                float errB = (oldB - newB) * activeStrength;

                // Aplica o erro nos vizinhos
                applyDither(activeAlgo, buffer, i, j, errR, errG, errB, width, height);
            }
        }
        return pixels;
    }

    /**
     * Encontra a cor mais próxima na paleta do Minecraft usando distância ponderada.
     * Inclui cache para performance.
     */
    private static byte findBestColor(int r, int g, int b) {
        // Chave única para o cache baseada no RGB
        int rgbKey = ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        
        Byte cached = COLOR_CACHE.get(rgbKey);
        if (cached != null) {
            return cached;
        }

        byte bestColor = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 4; i < 256; i++) { // Começa do 4 para pular cores base transparentes/reservadas antigas
            if (!PALETTE_VALID[i]) continue;

            Color c = MINECRAFT_PALETTE[i];
            
            // Fórmula "Redmean" para distância de cor (Melhor percepção humana que Euclidiana simples)
            // O olho humano é mais sensível a brilho e verde.
            long rmean = ((long)r + (long)c.getRed()) / 2;
            long r2 = (long)r - (long)c.getRed();
            long g2 = (long)g - (long)c.getGreen();
            long b2 = (long)b - (long)c.getBlue();
            
            // Pesos: 2, 4, 3 + ajuste redmean
            double weightR = 2 + (rmean / 256.0);
            double weightG = 4.0;
            double weightB = 2 + ((255 - rmean) / 256.0);
            
            double distance = (weightR * r2 * r2) + (weightG * g2 * g2) + (weightB * b2 * b2);

            if (distance < minDistance) {
                minDistance = distance;
                bestColor = (byte) i;
                
                // Se achou uma cor exata, para tudo
                if (distance == 0) break;
            }
        }

        COLOR_CACHE.put(rgbKey, bestColor);
        return bestColor;
    }

    private static void applyDither(String algo, float[][][] buffer, int x, int y, float errR, float errG, float errB, int width, int height) {
        switch (algo) {
            case "ATKINSON":
                distribute(buffer, x + 1, y, errR, errG, errB, 1.0f/8.0f, width, height);
                distribute(buffer, x + 2, y, errR, errG, errB, 1.0f/8.0f, width, height);
                distribute(buffer, x - 1, y + 1, errR, errG, errB, 1.0f/8.0f, width, height);
                distribute(buffer, x, y + 1, errR, errG, errB, 1.0f/8.0f, width, height);
                distribute(buffer, x + 1, y + 1, errR, errG, errB, 1.0f/8.0f, width, height);
                distribute(buffer, x, y + 2, errR, errG, errB, 1.0f/8.0f, width, height);
                break;
            case "BURKES":
                distribute(buffer, x + 1, y, errR, errG, errB, 8.0f/32.0f, width, height);
                distribute(buffer, x + 2, y, errR, errG, errB, 4.0f/32.0f, width, height);
                distribute(buffer, x - 2, y + 1, errR, errG, errB, 2.0f/32.0f, width, height);
                distribute(buffer, x - 1, y + 1, errR, errG, errB, 4.0f/32.0f, width, height);
                distribute(buffer, x, y + 1, errR, errG, errB, 8.0f/32.0f, width, height);
                distribute(buffer, x + 1, y + 1, errR, errG, errB, 4.0f/32.0f, width, height);
                distribute(buffer, x + 2, y + 1, errR, errG, errB, 2.0f/32.0f, width, height);
                break;
            case "SIERRA":
                distribute(buffer, x + 1, y, errR, errG, errB, 2.0f/4.0f, width, height);
                distribute(buffer, x - 1, y + 1, errR, errG, errB, 1.0f/4.0f, width, height);
                distribute(buffer, x, y + 1, errR, errG, errB, 1.0f/4.0f, width, height);
                break;
            case "FLOYD":
            default:
                distribute(buffer, x + 1, y, errR, errG, errB, 7.0f/16.0f, width, height);
                distribute(buffer, x - 1, y + 1, errR, errG, errB, 3.0f/16.0f, width, height);
                distribute(buffer, x, y + 1, errR, errG, errB, 5.0f/16.0f, width, height);
                distribute(buffer, x + 1, y + 1, errR, errG, errB, 1.0f/16.0f, width, height);
                break;
        }
    }

    private static void distribute(float[][][] buffer, int x, int y, float errR, float errG, float errB, float factor, int width, int height) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            buffer[x][y][0] += errR * factor;
            buffer[x][y][1] += errG * factor;
            buffer[x][y][2] += errB * factor;
        }
    }

    private static int clamp(int val) {
        return Math.max(0, Math.min(255, val));
    }
}