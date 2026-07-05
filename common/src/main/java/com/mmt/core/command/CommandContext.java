package com.mmt.core.command;

import com.mmt.core.config.ConfigManager;
import com.mmt.core.data.path.PathHelper;
import com.mmt.core.extract.Extractor;
import com.mmt.core.i18n.I18nManager;
import com.mmt.core.log.MmtLogger;
import com.mmt.core.pack.Packer;
import com.mmt.core.pipeline.AutoPipeline;
import com.mmt.core.translate.Translator;

public class CommandContext {
    private final ConfigManager configManager;
    private final PathHelper pathHelper;
    private final MmtLogger logger;
    private final Extractor extractor;
    private final Translator translator;
    private final Packer packer;
    private final String mcVersion;
    private final I18nManager i18n;
    private final AutoPipeline autoPipeline;

    public CommandContext(ConfigManager configManager, PathHelper pathHelper, MmtLogger logger,
                          Extractor extractor, Translator translator, Packer packer, String mcVersion,
                          I18nManager i18n, AutoPipeline autoPipeline) {
        this.configManager = configManager;
        this.pathHelper = pathHelper;
        this.logger = logger;
        this.extractor = extractor;
        this.translator = translator;
        this.packer = packer;
        this.mcVersion = mcVersion;
        this.i18n = i18n;
        this.autoPipeline = autoPipeline;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PathHelper getPathHelper() {
        return pathHelper;
    }

    public MmtLogger getLogger() {
        return logger;
    }

    public Extractor getExtractor() {
        return extractor;
    }

    public Translator getTranslator() {
        return translator;
    }

    public Packer getPacker() {
        return packer;
    }

    public String getMcVersion() {
        return mcVersion;
    }

    public AutoPipeline getAutoPipeline() {
        return autoPipeline;
    }

    public I18nManager getI18n() {
        return i18n;
    }

    public String t(String key) {
        return i18n.translate(key);
    }

    public String t(String key, Object... args) {
        return i18n.translate(key, args);
    }
}