package net.gasharaujo.gqc;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = GqcClient.MOD_ID, dist = Dist.CLIENT)
public final class GashQuickCommands {
    public GashQuickCommands(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Platform.SPEC);
        GqcClient.initialize();
        bus.addListener(ClientEvents::registerKeys);
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> GqcClient.configScreen(parent));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(ClientEvents.class);
    }
}
