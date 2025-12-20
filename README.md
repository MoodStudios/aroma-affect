<p align="center">
  <img src="assets/logo.png" alt="Aroma Affect Logo" width="400"/>
</p>

<h1 align="center">Aroma Affect</h1>

<p align="center">
  <strong>A Minecraft mod that brings the sense of smell to your gameplay</strong>
</p>

<p align="center">
  <a href="https://github.com/MoodStudios/aroma-affect/wiki/Getting-Started">Getting Started</a> •
  <a href="#features">Features</a> •
  <a href="#requirements">Requirements</a> •
  <a href="#installation">Installation</a> •
  <a href="#license">License</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.10-brightgreen" alt="Minecraft Version"/>
  <img src="https://img.shields.io/badge/NeoForge-Supported-orange" alt="NeoForge"/>
  <img src="https://img.shields.io/badge/Fabric-Supported-blue" alt="Fabric"/>
  <img src="https://img.shields.io/badge/License-Proprietary-red" alt="License"/>
</p>

---

## 🎮 About

**Aroma Affect** is a gameplay-first Minecraft mod developed by [Mood Studios](https://moodstudios.co) for [OVR Technology](https://ovrtechnology.com) that integrates **real-world scent hardware** into Minecraft. With Aroma Affect, players can literally smell their Minecraft world - from the sulfur of nearby lava to the fresh air of a forest biome.

The mod works on two levels:

- **Passive Scenting** - The world around you emits scents automatically. Walk through a jungle and smell the vegetation, approach lava and sense the danger, enter a village and catch ambient scents. This works out of the box with no items required.

- **Active Tracking** - Equip a **Scent Mask** (known as "The Nose") to filter, focus, and actively track specific targets. The Nose amplifies your scent abilities and unlocks precision tracking for ores, biomes, structures, and more.

The mod is designed to be fully playable for:
- 🎯 **Players with OVR scent hardware** - Experience immersive real-world scents triggered by in-game events
- 🎮 **Players without hardware** - Enjoy full gameplay with visual feedback, particle effects, and tracking mechanics

## ✨ Features

### 🌍 Ambient World Scenting
Experience your Minecraft world through smell. Biome transitions, environmental hazards, nearby resources, and ambient surroundings all emit scents - no special items required.

### 🎭 The Nose System
Craft and upgrade Scent Masks through a tiered progression system. Each tier unlocks new abilities:
- **Filter** specific scents from the ambient noise
- **Track** blocks, biomes, and structures with precision
- **Navigate** towards your targets with visual guidance

### 🐕 Sniffer Integration
Unlock endgame content with the Sniffer - a companion that enhances your scent abilities and opens up new gameplay possibilities.

### 🗺️ Scent Tracking Categories
- **Blocks** - Locate ores, resources, and specific block types
- **Biomes** - Sense and navigate to biome transitions
- **Structures** - Find villages, dungeons, strongholds, and more

### 🎡 Intuitive Radial Menu
Access your scent abilities through a beautifully designed radial menu system with easy-to-use selection interfaces.

### 🔧 Modpack Friendly
Built with compatibility in mind, Aroma Affect is designed to work seamlessly within modded Minecraft environments.

## 📋 Requirements

- **Minecraft** 1.21.10
- **NeoForge** 21.10.50-beta or **Fabric Loader** 0.18.2
- **Architectury API** 18.0.8 (automatically included)

### 🌉 OVR Bridge

To experience real-world scents with OVR hardware, you'll need the **OVR Bridge** application. The Bridge handles communication between Aroma Affect and your OVR scent device.

- [**Download OVR Bridge for Windows**](https://www.ovrtechnology.com/) *(Currently Windows only)*

> **Note:** The mod works perfectly without the Bridge - you'll still enjoy all gameplay features, visual feedback, and tracking mechanics. The Bridge is only required for the physical scent hardware integration.

## 📦 Installation

1. Download and install [NeoForge](https://neoforged.net/) or [Fabric](https://fabricmc.net/) for Minecraft 1.21.10
2. Download the latest Aroma Affect release for your loader
3. Place the mod `.jar` file in your `mods` folder
4. Launch Minecraft and enjoy!

For detailed setup instructions including OVR hardware configuration, see the [**Getting Started Guide**](https://github.com/MoodStudios/aroma-affect/wiki/Getting-Started).

## 🏗️ Project Structure

Aroma Affect uses the [Architectury](https://architectury.dev/) framework for cross-platform development:

```
aroma-affect/
├── common/     # Shared code for all platforms
├── fabric/     # Fabric-specific implementation
└── neoforge/   # NeoForge-specific implementation
```

## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/MoodStudios/aroma-affect.git
cd aroma-affect

# Build the mod
./gradlew build

# Output JARs will be in fabric/build/libs and neoforge/build/libs
```

## 📖 Documentation

Visit the [**Wiki**](https://github.com/MoodStudios/aroma-affect/wiki) for comprehensive documentation:

- [Getting Started](https://github.com/MoodStudios/aroma-affect/wiki/Getting-Started)
- [The Nose System](https://github.com/MoodStudios/aroma-affect/wiki/The-Nose-System)
- [Configuration Guide](https://github.com/MoodStudios/aroma-affect/wiki/Configuration)
- [OVR Hardware Setup](https://github.com/MoodStudios/aroma-affect/wiki/OVR-Hardware-Setup)

## 🤝 Contributing

This is a proprietary project developed for OVR Technology. For contribution inquiries, please contact the development team.

## 📄 License

**Copyright © 2025 OVR Technologies. All rights reserved.**

This software is proprietary and confidential. See the [LICENSE](LICENSE) file for full terms and conditions.

---

<p align="center">
  Developed with ❤️ by <a href="https://moodstudios.co">Mood Studios</a> for <a href="https://ovrtechnology.com">OVR Technology</a>
</p>
