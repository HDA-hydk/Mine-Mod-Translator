package com.mmt.forge;

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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Properties;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Forge 适配层主类
 * 通过 @Mod 注解注册模组，实现平台适配接口
 */
@Mod(Constants.MOD_ID)
public class MmtForge implements IPlatformAdapter {
    private static MmtForge INSTANCE;

    public MmtForge() {
        INSTANCE = this;

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        MmtMod.getInstance().init(this);
    }

    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            System.out.println("[MMT-Forge] Common setup completed");
            MmtMod.getInstance().onGameStarted();
        });
    }

    private boolean hasNotifiedJoin = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null && !hasNotifiedJoin) {
            hasNotifiedJoin = true;
            MmtMod.getInstance().onPlayerJoinedWorld();
        }
        if (mc != null && mc.player == null && hasNotifiedJoin) {
            hasNotifiedJoin = false;
        }
    }

    @Override
    public String getPlatformName() {
        return "forge";
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
            return net.minecraft.client.Minecraft.getInstance().options.languageCode;
        } catch (Exception e) {
            return "en_us";
        }
    }

    @Override
    public String getMinecraftVersion() {
        return net.minecraftforge.fml.loading.FMLLoader.versionInfo().mcVersion();
    }

    @Override
    public List<String> getLoadedModIds() {
        return net.minecraftforge.fml.ModList.get().getMods().stream()
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
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null) {
                System.err.println("[MMT] Minecraft instance is null (dedicated server?)");
                return result;
            }

            ResourceManager rm = mc.getResourceManager();
            if (rm == null) {
                System.err.println("[MMT] Resource manager is null");
                return result;
            }

            // 列出所有 lang 目录下的语言文件（.json 和 .properties/.lang）
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
                String path = rl.getPath(); // 如 "lang/en_us.json" 或 "lang/en_US.lang"

                // 从路径提取语言代码和格式
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

                // 统一语言代码格式（小写+下划线）
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

            System.out.println("[MMT] Found " + result.size() + " mods with lang files via ResourceManager");

            // Fallback: scan mod JAR files directly (for dev environment where mod resources
            // from mods/ directory are not registered in the ResourceManager)
            scanModJarFiles(result);

        } catch (Exception e) {
            System.err.println("[MMT] Failed to get mod lang files: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Fallback: scan mod JAR files in the mods/ directory for lang files.
     * This is needed in dev environment where mod resources from mods/ directory
     * are not registered in the ResourceManager.
     */
    private void scanModJarFiles(Map<String, List<ModLangFile>> result) {
        try {
            Path modsPath = FMLPaths.MODSDIR.get();
            File modsDir = modsPath.toFile();
            if (!modsDir.exists() || !modsDir.isDirectory()) {
                return;
            }

            File[] jarFiles = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                return;
            }

            Gson gson = new Gson();
            int added = 0;

            for (File jarFile : jarFiles) {
                try (ZipFile zip = new ZipFile(jarFile)) {
                    // Find all lang files: assets/<namespace>/lang/<langcode>.json
                    java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (!name.startsWith("assets/") || !name.contains("/lang/")) {
                            continue;
                        }
                        if (!name.endsWith(".json") && !name.endsWith(".properties") && !name.endsWith(".lang")) {
                            continue;
                        }

                        // Extract namespace and lang code
                        // assets/testdict/lang/en_us.json -> namespace=testdict, langCode=en_us
                        String[] parts = name.split("/");
                        if (parts.length < 4 || !parts[0].equals("assets") || !parts[2].equals("lang")) {
                            continue;
                        }
                        String modId = parts[1];
                        String fileName = parts[3];

                        // Skip if already found via ResourceManager
                        if (result.containsKey(modId)) {
                            boolean alreadyHas = result.get(modId).stream()
                                    .anyMatch(f -> f.getFilePath().endsWith(fileName));
                            if (alreadyHas) {
                                continue;
                            }
                        }

                        String langCode = fileName;
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

                        try (InputStream is = zip.getInputStream(entry);
                             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                            Map<String, String> entries2 = new HashMap<>();
                            if (isJson) {
                                Map<String, String> jsonEntries = gson.fromJson(reader,
                                        new TypeToken<Map<String, String>>() {}.getType());
                                if (jsonEntries != null) {
                                    entries2.putAll(jsonEntries);
                                }
                            } else if (isProperties) {
                                Properties props = new Properties();
                                props.load(reader);
                                for (String key : props.stringPropertyNames()) {
                                    entries2.put(key, props.getProperty(key));
                                }
                            }

                            if (!entries2.isEmpty()) {
                                ModLangFile langFile = new ModLangFile(modId, modId, langCode, name);
                                langFile.getEntries().putAll(entries2);
                                result.computeIfAbsent(modId, k -> new ArrayList<>()).add(langFile);
                                added++;
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[MMT] Failed to scan JAR: " + jarFile.getName() + " - " + e.getMessage());
                }
            }

            if (added > 0) {
                System.out.println("[MMT] Fallback JAR scan added " + added + " lang files from mods/ directory");
            }
        } catch (Exception e) {
            System.err.println("[MMT] Fallback JAR scan failed: " + e.getMessage());
        }
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

    public static MmtForge getInstance() {
        return INSTANCE;
    }
}
