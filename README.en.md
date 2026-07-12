<div align="right">

**English** | [简体中文](README.md)

</div>

# Mine Mod Translator (MMT)

> 📣 **Join our QQ Group for testing & feedback! Group ID: `1058830118`** 🎮
>
> The project is still in development. We need your feedback and suggestions to improve it.

A Minecraft mod translation tool supporting multiple loaders (Forge / Fabric / NeoForge), providing an integrated extract-translate-pack pipeline for mod localization.

## Features

- **Multi-platform support**: Supports Forge, Fabric, and NeoForge
- **Multiple language file formats**: Supports both `.json` and `.properties` / `.lang` formats with auto-detection and format-preserving packing
- **Smart extraction**:
  - Auto-scans mod language files with intelligent source language selection (variant priority > English priority > alphabetical)
  - Skips Minecraft vanilla, only extracts extra mods
  - Skips mods where the target language is the native language
  - Supports diff extraction / full extraction
  - Checks dictionary during extraction; matched entries are excluded from AI files to reduce translation volume
- **Multiple translation methods**:
  - Dictionary translation (I18n-dict): External directory loading, supports multi-file merging (**only effective when the target language is Chinese**)
  - AI manual translation: Generates AI translation files for manual result pasting
  - AI auto translation: Automatically calls AI API for translation
  - Machine translation (MT): Calls MT API service
- **Efficient translation**: Only processes untranslated keys to avoid redundant translation
- **Priority management**: User translation > Dictionary > AI > MT; lower priority does not override higher priority; all translation results are preserved
- **Auto pipeline**: One-click extract-translate-pack with detailed step-by-step statistics
- **Resource pack generation**: Auto-generates Minecraft resource packs in the mod's original format, ready to enable in-game
- **Multilingual UI**: Command output follows the game language; supports English, Simplified Chinese, and Traditional Chinese

## Project Structure

```
MMT3.0/
├── common/          # Common core module (platform-agnostic logic)
│   └── src/main/java/com/mmt/core/
│       ├── extract/      # Extractor
│       ├── translate/    # Translator
│       ├── pack/         # Packer
│       ├── pipeline/     # Auto pipeline
│       ├── command/      # Command system
│       ├── config/       # Config management
│       ├── i18n/         # Internationalization
│       └── data/         # Data models
├── forge/           # Forge platform adapter
├── fabric/          # Fabric platform adapter
├── neoforge/        # NeoForge platform adapter
└── gradle/          # Gradle build config
```

## Build

### Requirements

- JDK 17+
- Gradle (wrapper included)

### Build Commands

```bash
# Build all platforms
./gradlew build

# Build Forge only
./gradlew forge:build

# Build Fabric only
./gradlew fabric:build

# Build NeoForge only
./gradlew neoforge:build
```

Build artifacts are located in each module's `build/libs/` directory.

## Usage

### Installation

Place the platform-specific mod JAR file into the game's `mods/` directory and launch the game.

### Data Directory

All MMT data files are stored in the `mmt/` folder under the game root directory:

```
mmt/
├── config.txt              # Config file
├── data/
│   ├── extracted.json      # Extracted language data (shared, language code as top-level key)
│   ├── translated.json     # Translated language data (shared)
│   └── packed.json         # Packed language data (shared)
├── AItranslation_*.txt     # AI translation input/output files (per target language)
├── AItranslationResult_*.txt  # AI translation result file
├── archive/ai/             # Archived AI files
├── failed_pastes/          # Failed parse fragments
├── i18n-dic/               # Dictionary directory (multiple JSON files, auto-merged)
└── logs/                   # Log directory
```

### Commands

All commands start with `/mmt`.

| Command | Description |
|---------|-------------|
| `/mmt status [targetLanguage]` | View current translation status |
| `/mmt extract [targetLanguage]` | Extract mod language files |
| `/mmt translate [targetLanguage]` | Execute translation |
| `/mmt pack [targetLanguage]` | Pack and generate resource pack |
| `/mmt auto [targetLanguage]` | Run full pipeline (extract → translate → pack) |
| `/mmt config <get\|set\|reload> [key] [value]` | Manage configuration |
| `/mmt dict status` | View dictionary status |
| `/mmt dict install <mini\|full>` | Install dictionary |
| `/mmt dict reload` | Reload dictionary files |
| `/mmt dict dir` | Show dictionary directory |
| `/mmt clear <extracted\|translated\|packed\|ai\|all> [targetLanguage]` | Clear data |
| `/mmt togglejoinmsg [on\|off]` | Toggle join-world summary |
| `/mmt confirm` | Confirm dangerous operations |

### Quick Start

1. Enter a world (MMT will automatically extract content that needs translation)
2. Open the game directory and find `mmt/AItranslation_[lang]_[number].txt`
3. Send the file content to an AI and wait for the translation result
4. Copy the AI translation result. You can either:
   - Paste and overwrite the original `AItranslation_*.txt` file
   - Or paste it into the `AItranslationResult_*.txt` file
5. Run `/mmt auto` to process translations and pack
6. Enable the generated resource pack in-game (located at `resourcepacks/MMT_[lang]/`)

### Advanced

1. **Dictionary translation**: Download dictionary files from `https://github.com/CFPATools/i18n-dict/releases/` and place them in the `mmt/i18n-dic/` directory. MMT will prioritize dictionary translations. **Note: The dictionary only works when the target language is Chinese (zh_cn / zh_tw) and the source language is English (en_us).**
2. **Manual edits**: You can manually edit language files in the resource pack. MMT will not overwrite your changes on the next pack.

### Translation Priority

Translation results are processed by priority. Higher priority is not overridden by lower priority:

1. **User manual translation** (USER_TRANSLATED)
2. **Dictionary translation** (I18N_DICT) — Only available for Chinese translation
3. **AI translation** (AI_MANUAL / AI_AUTO)
4. **Machine translation** (MT)

## Configuration

Main config options (via `/mmt config set`):

| Key | Default | Description |
|-----|---------|-------------|
| `targetLanguage` | (empty) | Target language, defaults to game language |
| `extractMode` | `diff` | Extract mode: `diff` / `full` |
| `translateMethods` | `I18n-dict,AI-manual` | Enabled translation methods, comma-separated |
| `aiFileMaxSize` | `-1` | Max AI file size in chars (-1 = unlimited) |
| `aiSplitOversizedMod` | `false` | Force-split oversized mods' AI files |
| `showJoinSummary` | `true` | Show status summary on world join |
| `aiApiUrl` | (empty) | AI auto-translation API URL |
| `aiApiKey` | (empty) | AI auto-translation API key |
| `aiApiModel` | (empty) | AI auto-translation model name |
| `mtApiUrl` | (empty) | Machine translation API URL |
| `mtApiKey` | (empty) | Machine translation API key |
| `dictVersion` | `mini` | Dictionary version: `mini` / `full` |

## Tech Stack

- **Language**: Java 17
- **Build tool**: Gradle
- **Supported platforms**: Minecraft 1.20.1 (Forge / Fabric / NeoForge)

## Notes
This project was developed with AI assistance, and some features may not be fully tested. Issues and suggestions are welcome.

## License
This project is open-sourced under the [MIT License](https://opensource.org/licenses/MIT).
