package com.mmt.neoforge;

import com.mmt.core.Constants;
import com.mmt.core.MmtMod;
import com.mmt.core.api.IPlatformAdapter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.server.ServerStartedEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Path;
import java.util.List;

/**
 * NeoForge 适配层主类
 * 通过 @Mod 注解注册模组，实现平台适配接口
 */
@Mod(Constants.MOD_ID)
public class MmtNeoForge implements IPlatformAdapter {
    // NeoForge 模组实例
    private static MmtNeoForge INSTANCE;

    public MmtNeoForge(IEventBus modEventBus) {
        INSTANCE = this;

        // 注册模组事件
        modEventBus.addListener(this::onCommonSetup);

        // 注册 NeoForge 事件总线
        NeoForge.EVENT_BUS.register(this);

        // 初始化公共层
        MmtMod.getInstance().init(this);
    }

    /**
     * 公共设置事件
     */
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 在游戏主线程执行初始化
            System.out.println("[MMT-NeoForge] Common setup completed");
        });
    }

    /**
     * 服务端启动完成事件
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // 游戏启动完成，触发公共层回调
        MmtMod.getInstance().onGameStarted();
    }

    // ========== IPlatformAdapter 实现 ==========

    @Override
    public String getPlatformName() {
        return "neoforge";
    }

    @Override
    public Path getGameVersionDir() {
        // NeoForge 不直接提供版本目录，需要手动构造
        return getGameRunDir().resolve("versions").resolve(getMinecraftVersion());
    }

    @Override
    public Path getGameRunDir() {
        return Path.of(".");
    }

    @Override
    public String getCurrentLanguage() {
        // NeoForge 获取当前语言
        try {
            return net.minecraft.client.Minecraft.getInstance().options.languageCode;
        } catch (Exception e) {
            // 服务端环境返回默认值
            return "en_us";
        }
    }

    @Override
    public String getMinecraftVersion() {
        // NeoForge 获取 MC 版本
        return net.neoforged.fml.loading.FMLLoader.versionInfo().mcVersion();
    }

    @Override
    public List<String> getLoadedModIds() {
        // NeoForge 获取已加载模组列表
        return net.neoforged.fml.ModList.get().getMods().stream()
                .map(mod -> mod.getModId())
                .collect(java.util.stream.Collectors.toList());
    }

    // ========== 静态方法 ==========

    public static MmtNeoForge getInstance() {
        return INSTANCE;
    }
}