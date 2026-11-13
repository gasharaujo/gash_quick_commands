package net.gasharaujo.gqc;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.client.ConfigScreenHandler;

@Mod(GqcClient.MOD_ID)
public final class GashQuickCommands {
    public GashQuickCommands() {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Platform.SPEC);
        GqcClient.initialize();
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(ClientEvents::registerKeys);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> GqcClient.configScreen(parent)));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ClientEvents.class);
    }
}
