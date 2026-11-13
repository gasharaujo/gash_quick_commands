package net.gasharaujo.gqc;

import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public final class ClientEvents {
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(Platform.CATEGORY);
        event.register(GqcClient.OPEN);
        GqcClient.BINDINGS.values().forEach(event::register);
    }
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        for (String alias : new String[]{"gqc", "gashQuickCommands"})
            event.getDispatcher().register(Commands.literal(alias).executes(ctx -> { GqcClient.requestOpen(); return 1; }));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) { GqcClient.tick(); }
}
