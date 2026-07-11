package com.mmt.core;

import com.mmt.core.api.IPlatformAdapter;
import com.mmt.core.command.CommandContext;
import com.mmt.core.config.ConfigManager;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.extract.Extractor;
import com.mmt.core.i18n.I18nManager;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.pack.Packer;
import com.mmt.core.pipeline.AutoPipeline;
import com.mmt.core.translate.Translator;
import com.mmt.core.translate.dict.I18nDict;

/**
 * MMT 模组主类（公共层）
 * 各加载器适配层通过此类访问公共功能
 */
public final class MmtMod {
    private static MmtMod INSTANCE;

    private IPlatformAdapter platformAdapter;
    private ConfigManager configManager;
    private MmtLogger logger;
    private Extractor extractor;
    private Translator translator;
    private Packer packer;
    private I18nManager i18nManager;
    private CommandContext commandContext;
    private AutoPipeline autoPipeline;
    private boolean initialized = false;

    private MmtMod() {
    }

    public static MmtMod getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MmtMod();
        }
        return INSTANCE;
    }

    /**
     * 初始化模组
     * 由各加载器适配层在模组加载时调用
     * @param adapter 平台适配器
     */
    public void init(IPlatformAdapter adapter) {
        if (initialized) {
            return;
        }
        this.platformAdapter = adapter;
        this.initialized = true;

        configManager = ConfigManager.getInstance(adapter);
        configManager.init();

        logger = MmtLogger.getInstance(adapter);
        int debugMode = configManager.getDebug();
        logger.init(debugMode);

        i18nManager = I18nManager.getInstance(logger);

        PathHelper pathHelper = new PathHelper(adapter);

        I18nDict i18nDict = I18nDict.getInstance(logger);
        i18nDict.setPathHelper(pathHelper);

        extractor = new Extractor(adapter, configManager, pathHelper, logger);
        translator = new Translator(configManager, pathHelper, logger);
        packer = new Packer(configManager, pathHelper, logger);

        autoPipeline = new AutoPipeline(configManager, extractor, translator, packer,
                logger, adapter.getMinecraftVersion(),
                adapter, i18nManager, pathHelper);

        commandContext = new CommandContext(configManager, pathHelper, logger, extractor, translator, packer,
                adapter.getMinecraftVersion(), i18nManager, autoPipeline);

        logger.info(Constants.MOD_NAME + " v" + Constants.MOD_VERSION + " initialized on " + adapter.getPlatformName());
    }

    /**
     * 更新 i18n 语言
     * MMT 自身消息语言跟随游戏语言，与翻译目标语言分离
     */
    private void updateI18nLanguage() {
        String gameLang = platformAdapter.getCurrentLanguage();
        logger.info("I18n: game language=" + gameLang);
        i18nManager.setLanguage(gameLang);
        logger.info("I18n: current language set to " + i18nManager.getCurrentLanguage()
                + ", loaded languages: " + String.join(", ", i18nManager.getLoadedLanguages()));
    }

    /**
     * 游戏启动完成回调
     * 由各加载器适配层在游戏启动完成后调用
     */
    public void onGameStarted() {
        if (!initialized) {
            System.err.println("[MMT] WARNING: onGameStarted called before init");
            return;
        }

        updateI18nLanguage();

        logger.info("Game started, auto pipeline will start in 3 seconds...");

        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            autoPipeline.startAsync();
        }, "MMT-Pipeline-Starter").start();
    }

    /**
     * 玩家进入世界回调
     * 由各加载器适配层在玩家进入世界时调用
     */
    public void onPlayerJoinedWorld() {
        if (!initialized) {
            return;
        }
        autoPipeline.onPlayerJoinedWorld();
    }

    /**
     * 获取平台适配器
     * @return 平台适配器
     */
    public IPlatformAdapter getPlatformAdapter() {
        return platformAdapter;
    }

    /**
     * 获取命令上下文
     * @return 命令上下文
     */
    public CommandContext getCommandContext() {
        return commandContext;
    }

    /**
     * 检查是否已初始化
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 关闭模组（游戏退出时调用）
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        if (logger != null) {
            logger.shutdown();
        }
    }
}