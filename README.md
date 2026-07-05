# Mine Mod Translator (MMT)

Minecraft 模组翻译工具，支持多加载器（Forge / Fabric / NeoForge），提供提取、翻译、打包一体化的模组本地化解决方案。

## 功能特性

- **多平台支持**：同时支持 Forge、Fabric、NeoForge 三种模组加载器
- **多语言格式**：支持 `.json` 和 `.properties` / `.lang` 两种语言文件格式
- **智能提取**：自动扫描模组语言文件，智能选择源语言，支持差异提取
- **多种翻译方式**：
  - 词典翻译（I18n-dict）：本地词典快速匹配
  - AI 手动翻译：生成 AI 待翻译文件，人工粘贴翻译结果
  - AI 自动翻译：通过 API 自动调用 AI 翻译
  - 机器翻译（MT）：通过 API 调用机器翻译服务
- **优先级管理**：用户翻译 > 词典翻译 > AI 翻译 > 机器翻译
- **自动流水线**：一键执行提取、翻译、打包全流程
- **资源包生成**：自动生成 Minecraft 资源包，可直接在游戏中启用
- **多语言界面**：命令输出支持简体中文、繁体中文、英文

## 项目结构

```
MMT3.0/
├── common/          # 公共核心模块（平台无关逻辑）
│   └── src/main/java/com/mmt/core/
│       ├── extract/      # 提取器
│       ├── translate/    # 翻译器
│       ├── pack/         # 打包器
│       ├── pipeline/     # 自动流水线
│       ├── command/      # 命令系统
│       ├── config/       # 配置管理
│       ├── i18n/         # 国际化
│       └── data/         # 数据模型
├── forge/           # Forge 平台适配
├── fabric/          # Fabric 平台适配
├── neoforge/        # NeoForge 平台适配
└── gradle/          # Gradle 构建配置
```

## 构建

### 环境要求

- JDK 17+
- Gradle（项目已包含 Wrapper）

### 构建命令

```bash
# 构建所有平台
./gradlew build

# 仅构建 Forge 版本
./gradlew forge:build

# 仅构建 Fabric 版本
./gradlew fabric:build

# 仅构建 NeoForge 版本
./gradlew neoforge:build
```

构建产物位于各模块的 `build/libs/` 目录下。

## 使用方法

### 安装

将对应平台的模组 JAR 文件放入游戏的 `mods/` 目录，启动游戏即可。

### 数据目录

MMT 的所有数据文件存储在游戏根目录的 `mmt/` 文件夹下：

```
mmt/
├── config.txt              # 配置文件
├── data/
│   ├── extracted.json      # 已提取的语言数据
│   ├── translated.json     # 已翻译的语言数据
│   └── packed.json         # 已打包的语言数据
├── i18n-dic/               # 词典目录（可放置 JSON 词典文件）
├── logs/                   # 日志目录
├── AItranslation_*.txt     # AI 待翻译文件
└── AItranslationResult_*.txt  # AI 翻译结果文件
```

### 命令列表

所有命令均以 `/mmt` 开头。

| 命令 | 说明 |
|------|------|
| `/mmt status [目标语言]` | 查看当前翻译状态 |
| `/mmt extract [目标语言]` | 提取模组语言文件 |
| `/mmt translate [目标语言]` | 执行翻译 |
| `/mmt pack [目标语言]` | 打包生成资源包 |
| `/mmt auto [目标语言]` | 自动执行流水线（提取→翻译→打包） |
| `/mmt config <get\|set\|reload> [键] [值]` | 管理配置 |
| `/mmt dict status` | 查看词典状态 |
| `/mmt dict install <mini\|full>` | 安装词典 |
| `/mmt dict reload` | 重新加载词典 |
| `/mmt dict dir` | 显示词典目录 |
| `/mmt clear <extracted\|translated\|packed\|ai\|all> [目标语言]` | 清除数据 |
| `/mmt togglejoinmsg [on\|off]` | 开关进入世界提示 |
| `/mmt confirm` | 确认危险操作 |

### 快速开始

1. 进入游戏世界(此时MMT会自动提取需要翻译的内容)
2. 打开游戏目录，找到 `mmt/AItranslation_[语言代码]_[编号].txt`
3. 将 `mmt/AItranslation_[语言代码]_[编号].txt` 发送给AI，并等待翻译结果
4. 复制翻译结果，粘贴并覆盖原文件的内容
5. 执行 `/mmt auto` 自动识别翻译并打包
6. 在游戏中启用生成的资源包

### 进阶操作
1. **词典翻译**：前往 `https://github.com/CFPATools/i18n-dict/releases/` 下载词典文件，将词典文件放置在 `mmt/i18n-dic/` 目录下。此时MMT会优先使用词典翻译。
2. **手动修改**: 你可以手动修改资源包里的语言文件，MMT在下次打包时不会覆盖你的修改。

### 翻译优先级

翻译结果按照以下优先级处理，高优先级不会被低优先级覆盖：

1. **用户手动翻译**（USER_TRANSLATED）
2. **词典翻译**（I18N_DICT）
3. **AI 翻译**（AI_MANUAL / AI_AUTO）
4. **机器翻译**（MT）

## 配置说明

主要配置项（通过 `/mmt config set` 修改）：

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| `targetLanguage` | （空） | 目标语言，默认为游戏当前语言 |
| `extractMode` | `diff` | 提取模式：`diff` 差异提取 / `full` 全量提取 |
| `translateMethods` | `I18n-dict,AI-manual` | 启用的翻译方式，逗号分隔 |
| `aiFileMaxSize` | `-1` | AI 文件最大字符数（-1 为不限制） |
| `aiSplitOversizedMod` | `false` | 是否强制拆分超大模组的 AI 文件 |
| `showJoinSummary` | `true` | 是否在进入世界时显示状态摘要 |
| `aiApiUrl` | （空） | AI 自动翻译 API 地址 |
| `aiApiKey` | （空） | AI 自动翻译 API 密钥 |
| `aiApiModel` | （空） | AI 自动翻译模型名称 |
| `mtApiUrl` | （空） | 机器翻译 API 地址 |
| `mtApiKey` | （空） | 机器翻译 API 密钥 |
| `dictVersion` | `mini` | 词典版本：`mini` / `full` |

## 技术栈

- **语言**：Java 17
- **构建工具**：Gradle
- **支持平台**：Minecraft 1.20.1（Forge / Fabric / NeoForge）

## 补充说明
本项目由AI完成开发，且部分功能未经测试，如有问题和建议欢迎提交issue，感谢。

## 声明
本项目基于 [MIT 许可证](https://opensource.org/licenses/MIT) 开源。
