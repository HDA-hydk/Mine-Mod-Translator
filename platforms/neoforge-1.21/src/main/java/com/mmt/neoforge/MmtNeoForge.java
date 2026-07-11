package com.mmt.neoforge;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mmt.core.Constants;
import com.mmt.core.MmtMod;
import com.mmt.core.api.IPlatformAdapter;
import com.mmt.core.extract.model.ModLangFile;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.ModList;

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

@Mod(Constants.MOD_ID)
public class MmtNeoForge implements IPlatformAdapter {
    private static MmtNeoForge INSTANCE;

    public MmtNeoForge(IEventBus modEventBus) {
        INSTANCE = this;

        modEventBus.addListener(this::onCommonSetup);

        NeoForge.EVENT_BUS.register(this);

        MmtMod.getInstance().init(this);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            System.out.println("[MMT-NeoForge] Common setup completed");
            MmtMod.getInstance().onGameStarted();
        });
    }

    @Override
    public String getPlatformName() {
        return "neoforge";
    }

    @Override
    public Path getGameVersionDir() {
        return getGameRunDir();
    }

    @Override
    public Path getGameRunDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public String getCurrentLanguage() {
        try {
            return Minecraft.getInstance().options.languageCode;
        } catch (Exception e) {
            return "en_us";
        }
    }

    @Override
    public String getMinecraftVersion() {
        return FMLLoader.versionInfo().mcVersion();
    }

    @Override
    public List<String> getLoadedModIds() {
        return ModList.get().getMods().stream()
                .map(mod -> mod.getModId())
                .collect(Collectors.toList());
    }

    @Override
    public void sendChatMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            System.out.println("[MMT] " + message);
            return;
        }
        mc.execute(() -> {
            if (mc.player != null) {
                mc.player.sendSystemMessage(Component.literal(message));
            } else {
                System.out.println("[MMT] " + message);
            }
        });
    }

    @Override
    public void sendChatMessageWithFilePath(String prefix, String filePath, String suffix) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            System.out.println("[MMT] " + prefix + filePath + suffix);
            return;
        }
        mc.execute(() -> {
            if (mc.player != null) {
                MutableComponent msg = Component.literal(prefix)
                        .append(Component.literal(filePath)
                                .withStyle(Style.EMPTY
                                        .withUnderlined(true)
                                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, filePath))))
                        .append(Component.literal(suffix));
                mc.player.sendSystemMessage(msg);
            } else {
                System.out.println("[MMT] " + prefix + filePath + suffix);
            }
        });
    }

    @Override
    public Map<String, List<ModLangFile>> getAllModLangFiles() {
        Map<String, List<ModLangFile>> result = new HashMap<>();

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                System.err.println("[MMT] Minecraft instance is null (dedicated server?)");
                return result;
            }

            ResourceManager rm = mc.getResourceManager();
            if (rm == null) {
                System.err.println("[MMT] Resource manager is null");
                return result;
            }

            Map<ResourceLocation, Resource> langResources = rm.listResources("lang",
                    rl -> {
                        String path = rl.getPath();
                        return path.endsWith(".json")
                                || path.endsWith(".properties")
                                || path.endsWith(".lang");
                    });

            Gson gson = new Gson();

            for (Map.Entry<ResourceLocation, Resource> entry : langResources.entrySet()) {
                ResourceLocation rl = entry.getKey();
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

                try (InputStream is = entry.getValue().open();
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

    public static MmtNeoForge getInstance() {
        return INSTANCE;
    }
}
