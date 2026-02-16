<p align="center">
  <img src="assets/logo_banner.png" alt="Aroma Affect Logo" width="400"/>
</p>
<p align="center">
  <strong>A Minecraft mod that brings the sense of smell to your gameplay</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#requirements">Requirements</a> •
  <a href="#installation">Installation</a> •
  <a href="#building-from-source">Building</a> •
  <a href="#license">License</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.10-brightgreen" alt="Minecraft Version"/>
  <img src="https://img.shields.io/badge/NeoForge-Supported-orange" alt="NeoForge"/>
  <img src="https://img.shields.io/badge/Fabric-Supported-blue" alt="Fabric"/>
  <img src="https://img.shields.io/badge/License-Proprietary-red" alt="License"/>
</p>

---

## About

**Aroma Affect** is a Minecraft mod developed by [Mood Studios](https://moodstudios.co) for [OVR Technology](https://ovrtechnology.com) that integrates **real-world scent hardware** into Minecraft. Players can smell their Minecraft world, from the sulfur of nearby lava to the fresh air of a forest biome.

The mod works on two levels:

- **Passive Scenting:** The world around you emits scents automatically based on biomes, blocks, and environmental hazards. No items required.
- **Active Tracking:** Equip a **Nose** to filter, focus, and actively track specific targets like ores, biomes, and structures.

The mod is fully playable both **with** and **without** OVR scent hardware. Without hardware, all gameplay mechanics, visual feedback, and particle effects still work.

## Features

### 🌍 Ambient World Scenting
Biome transitions, environmental hazards, and nearby resources all emit scents automatically.

### 🎭 The Nose System
Craft and upgrade Scent Masks through a tiered progression system. Each tier unlocks new abilities, track blocks/biomes/structures/flowers, and navigate toward targets with visual guidance.

### 🐕 Sniffer Integration
Endgame companion that unlocks new gameplay possibilities.

### 🎡 Radial Menu
Access your scent abilities through an intuitive radial menu system.

## Requirements

- **Minecraft** 1.21.10
- **NeoForge** 21.10.50-beta or **Fabric Loader** 0.18.2
- **Architectury API** 18.0.8 (automatically included)

### OVR Bridge

To experience real-world scents with OVR hardware, you'll need the **OVR Bridge** application:

- [**Download OVR Bridge for Windows**](https://www.ovrtechnology.com/) *(Currently Windows only)*

> The mod works perfectly without the Bridge. All gameplay features, visual feedback, and tracking mechanics are available without hardware.

## Installation

1. Install [NeoForge](https://neoforged.net/) or [Fabric](https://fabricmc.net/) for Minecraft 1.21.10
2. Download the latest Aroma Affect release for your loader
3. Place the `.jar` file in your `mods` folder
4. Launch Minecraft

## Project Structure

Aroma Affect uses [Architectury](https://www.curseforge.com/minecraft/mc-mods/architectury-api) for cross-platform development:

```
aromaaffect/
├── common/     # Shared code for all platforms
├── fabric/     # Fabric-specific implementation
├── neoforge/   # NeoForge-specific implementation
└── scripts/    # Utility scripts (NBT tools, code generation)
```

## Building from Source

```bash
git clone https://github.com/MoodStudios/aroma-affect.git
cd aroma-affect

./gradlew build

# Output JARs in fabric/build/libs and neoforge/build/libs
```

## License

**Copyright © 2025 OVR Technologies. All rights reserved.**

This software is proprietary and confidential. See the [LICENSE](LICENSE) file for full terms.

---

<p align="center">
  Developed with ❤️ by <a href="https://moodstudios.co">Mood Studios</a> for <a href="https://ovrtechnology.com">OVR Technology</a>
</p>
