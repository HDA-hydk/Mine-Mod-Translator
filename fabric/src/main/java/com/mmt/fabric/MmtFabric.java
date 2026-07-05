package com.mmt.fabric;

import com.mmt.core.Constants;
import com.mmt.core.MmtMod;
import com.mmt.core.api.IPlatformAdapter;
import com.mmt.core.extract.model.ModLangFile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ClickEvent;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Fabric 适配层主类
 * 实现 ModInitializer 接口和平台适配接口
 */
public class MmtFabric implements ModInitializer, IPlatformAdapter {
    private static MmtFabric INSTANCE;

    private static final String MC_VERSION = "1.20.1";

    private boolean hasNotifiedJoin = false;

    @Override
    public void onInitialize() {
        INSTANCE = this;

        MmtMod.getInstance().init(this);

        MmtCommands.register();

        System.out.println("[MMT-Fabric] Mod initialized");

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player != null && !hasNotifiedJoin) {
                hasNotifiedJoin = true;
                MmtMod.getInstance().onPlayerJoinedWorld();
            }
            if (mc.player == null && hasNotifiedJoin) {
                hasNotifiedJoin = false;
            }
        });

        MmtMod.getInstance().onGameStarted();
    }

    @Override
    public String getPlatformName() {
        return "fabric";
    }

    @Override
    public Path getGameVersionDir() {
        return getGameRunDir();
    }

    @Override
    public Path getGameRunDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public String getCurrentLanguage() {
        try {
            Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
            Object instance = minecraftClass.getMethod("getInstance").invoke(null);
            Object options = minecraftClass.getField("options").get(instance);
            Class<?> optionsClass = options.getClass();
            String language = (String) optionsClass.getMethod("getLanguage").invoke(options);
            return language != null ? language : "en_us";
        } catch (Exception e) {
            return "en_us";
        }
    }

    @Override
    public String getMinecraftVersion() {
        return MC_VERSION;
    }

    @Override
    public List<String> getLoadedModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> mod.getMetadata().getId())
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void sendChatMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            System.out.println("[MMT] " + message);
            return;
        }
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Text.literal(message));
            } else {
                System.out.println("[MMT] " + message);
            }
        });
    }

    @Override
    public void sendChatMessageWithFilePath(String prefix, String filePath, String suffix) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            System.out.println("[MMT] " + prefix + filePath + suffix);
            return;
        }
        mc.execute(() -> {
            if (mc.player != null) {
                MutableText msg = Text.literal(prefix)
                        .append(Text.literal(filePath)
                                .setStyle(Style.EMPTY
                                        .withUnderline(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, filePath))))
                        .append(Text.literal(suffix));
                mc.player.sendSystemMessage(msg);
            } else {
                System.out.println("[MMT] " + prefix + filePath + suffix);
            }
        });
    }

    @Override
    public Map<String, List<ModLangFile>> getAllModLangFiles() {
        return Collections.emptyMap();
    }

    @Override
    public List<ModLangFile> getModLangFiles(String modId) {
        return Collections.emptyList();
    }

    @Override
    public ModLangFile getModLangFile(String modId, String language) {
        return null;
    }

    @Override
    public boolean hasLangFile(String modId, String language) {
        return false;
    }

    @Override
    public List<String> getModNamespaces(String modId) {
        return Collections.emptyList();
    }

    public static MmtFabric getInstance() {
        return INSTANCE;
    }
}