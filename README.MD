# ImageMaps (Enhanced Edition)

> **Note:** This is an enhanced fork of the original **ImageMaps** plugin, focused on performance, modern usability, and advanced rendering features.

ImageMaps allows you to load custom images into your Minecraft server and place them into **Item Frames** or **Glow Item Frames** in a simple and efficient way.

Fully compatible with **Minecraft 1.21+**.

---

## ✨ What’s New? (Enhanced Features)

### ⚡ Performance & Optimization

* **Zero-Lag Reload**
  Configuration reloads now run asynchronously in the background. You can test different algorithms in real time without causing server lag spikes.

* **Smart Rendering Cache**
  Implemented a **Pixel Cache system (Byte Array)**. The plugin no longer recalculates color math every frame, drastically reducing CPU usage.

---

### 🎮 User Experience (GUI)

* **Interactive Menus**
  No more excessive typing. The commands `/imagemap list`, `/imagemap help`, and `/imagemap algorithms` now feature clickable buttons, hover tooltips, and quick chat actions.

* **Swap Command**
  New command to convert normal Item Frames into **Glow Item Frames** (or vice versa) within an area, without breaking and replacing images manually.

---

### 🎨 Advanced Rendering

* **RAW Mode (No Filter)**
  New *unfiltered* algorithm (Nearest Neighbor). Ideal for **Pixel Art**, simple logos, or builds that require clean and solid colors without dithering noise.

* **Auto-Translator**
  The plugin detects missing translation keys and automatically updates language files without overwriting your existing customizations.

---

## 📖 Command Guide

| Command                            | Description                                                                    | Permission               |
| ---------------------------------- | ------------------------------------------------------------------------------ | ------------------------ |
| `/imagemap place <file> [options]` | Places an image in the world. Supports args like `true` (invisible) or `fixed` | `imagemaps.place`        |
| `/imagemap download <name> <url>`  | Downloads an image from the internet into the plugin folder                    | `imagemaps.download`     |
| `/imagemap list [page]`            | Interactive list of all saved images                                           | `imagemaps.list`         |
| `/imagemap swap <type> <radius>`   | **NEW:** Swap Glow/Normal frames in an area around the player                  | `imagemaps.swap`         |
| `/imagemap algorithms`             | Visual list and explanation of available color algorithms                      | `imagemaps.algorithms`   |
| `/imagemap compare [A] [B]`        | Split-screen mode to compare two algorithms side-by-side                       | `imagemaps.compare`      |
| `/imagemap reloadconfig`           | Reloads configuration and algorithms asynchronously                            | `imagemaps.reloadconfig` |
| `/imagemap info <file>`            | Displays technical details and image resolution                                | `imagemaps.info`         |
| `/imagemap delete <file>`          | Deletes the image from disk and removes maps from the world                    | `imagemaps.delete`       |

---

## ⚙️ Configuration (`config.yml`)

You can customize the color algorithm and default placement behavior:

```yml
# Choose the color algorithm (Dithering)
# FLOYD, ATKINSON, BURKES, SIERRA, RAW

ditherAlgorithm: ATKINSON

# Dithering strength (0.0 to 1.0)
ditherStrength: 0.8

# Default behavior when using /imagemap place (without extra arguments)
defaultPlacement:
  glowing: false   # Use Glow Item Frames by default?
  invisible: false # Use invisible frames by default?
  fixed: false     # Prevent breaking by default?
```

---

## 🎨 Understanding the Algorithms

The plugin converts RGB images into Minecraft’s limited color palette. Choose the best option for your use case:

* **FLOYD (Floyd–Steinberg):** Classic standard. Great for realistic photos and smooth gradients, but can generate noticeable noise in solid colors.
* **ATKINSON:** Best choice for **anime, cartoons, and logos**. Preserves contrast and keeps clean flat areas.
* **RAW (No Filter):** **New.** Picks the closest color without pixel mixing. Perfect for pixel art and clean builds.
* **BURKES / SIERRA:** Softer alternatives to Floyd with different noise patterns.

---

## 📦 Installation

1. Stop your server.
2. Place the `.jar` file inside the `plugins/` folder.
3. Start the server.
4. Place your images (`.png`, `.jpg`, `.jpeg`) inside `plugins/ImageMaps/images/`.
5. Use `/imagemap list` in-game to view and place your images.

---

## 📜 Credits & License

This project is a fork and fully respects the original license.

* **Original Plugin:** ImageMaps by **SydMontague** (and previous contributors such as CraftCitizen)
* **Original Source Code:** [https://github.com/SydMontague/ImageMaps](https://github.com/SydMontague/ImageMaps)
* **License:** Distributed under the same license as the original project (see the `LICENSE` file in this repository)

All **Enhanced** modifications — including caching, swap commands, and interactive GUI — were built on top of the solid foundation of the original project.
