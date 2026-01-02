# ImageMaps (1.21 Enhanced Edition)

ImageMaps is an enhanced fork of the original **ImageMaps** plugin created by **SydMontague**.

This version has been modernized to support **Minecraft 1.21+** and introduces advanced image‑processing algorithms to deliver the best possible visual quality within the game’s color limitations.

---

## ✨ New Features

### 🎨 Dithering Algorithms (Smoothing)

Minecraft has a limited color palette (~244 colors). The original plugin suffered from noticeable *color banding*. This enhanced version implements multiple **Dithering algorithms** that mix pixels to create the illusion of additional colors:

* **Floyd–Steinberg** – Industry standard, excellent for photographs.
* **Atkinson (Recommended)** – Developed by Apple. Preserves high contrast and reduces noise in solid areas. Perfect for **anime, logos, and stylized art**.
* **Burkes** – Smoothing focused on horizontal lines.
* **Sierra Lite** – Fast and balanced, ideal for in‑game usage.

---

### 🛠️ Real‑Time Configuration Commands

It is no longer necessary to restart the server to adjust image quality:

* `/imagemap reloadconfig <strength> <algorithm>`
  Applies changes instantly to all visible maps.

* `/imagemap compare`
  Activates **Split Screen** mode, allowing before/after comparisons or side‑by‑side algorithm previews on the same image.

* `/imagemap algorithms`
  Lists all available algorithms with interactive descriptions.

---

### 🌐 Automatic Translation (Locale)

The plugin automatically detects the player’s client language:

* **English (en_us)** – Default
* **Portuguese (pt_br)** – Fully supported
* **Extensible** – Simply add new `.yml` files to the `lang` folder

---

### 🚀 Technical Improvements

* **Native 1.21 Compatibility** – Updated to **Java 21** and **Spigot API 1.21**
* **Bicubic Resizing** – Images are resized using high‑quality *Bicubic* filtering instead of *Nearest Neighbor*, significantly reducing jagged edges

---

## 📖 How to Use

### Installation

1. Place `ImageMaps.jar` in the `plugins` folder
2. Restart the server
3. Place your images (`.png`, `.jpg`) inside `plugins/ImageMaps/images/`

---

### 📌 Main Commands

| Command                                   | Description                                                           |
| ----------------------------------------- | --------------------------------------------------------------------- |
| `/imagemap place <filename>`              | Starts the image placement process. Right‑click the top‑left block    |
| `/imagemap reloadconfig [0.0–1.0] [ALGO]` | Reloads configuration. Example: `/imagemap reloadconfig 0.8 ATKINSON` |
| `/imagemap algorithms`                    | Displays the list of available algorithms                             |
| `/imagemap compare [Algo1] [Algo2]`       | Compares two algorithms side‑by‑side                                  |
| `/imagemap download <name> <url>`         | Downloads an image directly from the internet                         |
| `/imagemap list`                          | Lists all saved images                                                |

---

## ⚙️ Configuration (`config.yml`)

```yml
# Color processing algorithm
# Options: FLOYD, ATKINSON, BURKES, SIERRA

ditherAlgorithm: ATKINSON

# Effect strength (0.0 to 1.0)
# 0.0 = Disabled (Original image)
# 0.6 = Balanced
# 0.8 - 1.0 = Recommended for Atkinson

ditherStrength: 0.8
```

---

## ⚖️ Credits & License

This project is based on the original code by **SydMontague**.

* Original repository: [https://github.com/SydMontague/ImageMaps](https://github.com/SydMontague/ImageMaps)
* License: **MIT License** (maintained from the original project)

All modifications and rendering improvements were implemented for the benefit of the community.