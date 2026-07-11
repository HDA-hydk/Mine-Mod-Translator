package com.mmt.fabric;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mmt.core.Constants;
import com.mmt.core.MmtMod;
import com.mmt.core.api.IPlatformAdapter;
import com.mmt.core.extract.model.ModLangFile;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Fabric 适配层主类
 * 实现 ModInitializer 接口和平台适配接口
 */
public class MmtFabric implements ModInitializer, IPlatformAdapter {
    private static MmtFabric INSTANCE;

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

        new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            MmtMod.getInstance().onGameStarted();
        }, "MMT-Fabric-GameStart").start();
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
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.options != null) {
                return mc.options.language;
            }
        } catch (Exception e) {
            // ignore
        }
        return "en_us";
    }

    @Override
    public String getMinecraftVersion() {
        return FabricLoader.getInstance().getModContainer("minecraft")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("1.20.1");
    }

    @Override
    public List<String> getLoadedModIds() {
        return FabricLoader.getInstance().getAllMods().stream()
                .map(mod -> mod.getMetadata().getId())
                .collect(Collectors.toList());
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
                mc.player.sendMessage(Text.literal(message), false);
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
                mc.player.sendMessage(msg, false);
            } else {
                System.out.println("[MMT] " + prefix + filePath + suffix);
            }
        });
    }

    @Override
    public Map<String, List<ModLangFile>> getAllModLangFiles() {
        Map<String, List<ModLangFile>> result = new HashMap<>();

        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null) {
                System.err.println("[MMT] Minecraft instance is null (dedicated server?)");
                return result;
            }

            ResourceManager rm = mc.getResourceManager();
            if (rm == null) {
                System.err.println("[MMT] Resource manager is null");
                return result;
            }

            Map<Identifier, Resource> langResources = rm.findResources("lang",
                    rl -> {
                        String path = rl.getPath();
                        return path.endsWith(".json")
                                || path.endsWith(".properties")
                                || path.endsWith(".lang");
                    });

            Gson gson = new Gson();

            for (Map.Entry<Identifier, Resource> entry : langResources.entrySet()) {
                Identifier rl = entry.getKey();
                String modId = rl.getNamespace();
                String path = rl.getPath();

                String langCode = path;
                if (langCode.startsWith("lang/")) {
                    langCode = langCode.substring(5);
                }

                boolean isJson = langCode.endsWith(".json");
                boolean isProperties = langCode.endsWith(".properties") || langCode.endsWith(".lang");

                if (isJson) {
                    langCode = langCode.substring(0, langCode.length() - 5);
                } else if (isProperties) {
                    if (langCode.endsWith(".properties")) {
                        langCode = langCode.substring(0, langCode.length() - 11);
                    } else {
                        langCode = langCode.substring(0, langCode.length() - 5);
                    }
                }

                langCode = langCode.toLowerCase().replace('-', '_');

                try (InputStream is = entry.getValue().getInputStream();
                     InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                    Map<String, String> entries = new HashMap<>();

                    if (isJson) {
                        Map<String, String> jsonEntries = gson.fromJson(reader,
                                new TypeToken<Map<String, String>>() {}.getType());
                        if (jsonEntries != null) {
                            entries.putAll(jsonEntries);
                        }
                    } else if (isProperties) {
                        Properties props = new Properties();
                        props.load(reader);
                        for (String key : props.stringPropertyNames()) {
                            entries.put(key, props.getProperty(key));
                        }
                    }

                    if (!entries.isEmpty()) {
                        ModLangFile langFile = new ModLangFile(modId, modId, langCode, path);
                        langFile.getEntries().putAll(entries);
                        result.computeIfAbsent(modId, k -> new ArrayList<>()).add(langFile);
                    }
                } catch (Exception e) {
                    System.err.println("[MMT] Failed to parse lang file: " + modId + "/" + langCode + " - " + e.getMessage());
                }
            }

            System.out.println("[MMT] Found " + result.size() + " mods with lang files");
        } catch (Exception e) {
            System.err.println("[MMT] Failed to get mod lang files: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public List<ModLangFile> getModLangFiles(String modId) {
        Map<String, List<ModLangFile>> all = getAllModLangFiles();
        return all.getOrDefault(modId, new ArrayList<>());
    }

    @Override
    public ModLangFile getModLangFile(String modId, String language) {
        List<ModLangFile> files = getModLangFiles(modId);
        for (ModLangFile file : files) {
            if (file.getLanguage().equals(language)) {
                return file;
            }
        }
        return null;
    }

    @Override
    public boolean hasLangFile(String modId, String language) {
        return getModLangFile(modId, language) != null;
    }

    @Override
    public List<String> getModNamespaces(String modId) {
        List<ModLangFile> files = getModLangFiles(modId);
        return files.stream().map(ModLangFile::getNamespace).distinct().collect(Collectors.toList());
    }

    public static MmtFabric getInstance() {
        return INSTANCE;
    }
}