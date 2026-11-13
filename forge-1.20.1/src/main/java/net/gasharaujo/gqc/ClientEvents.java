package net.gasharaujo.gqc;

import net.minecraft.commands.Commands;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;

public final class ClientEvents {
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(GqcClient.OPEN);
        GqcClient.BINDINGS.values().forEach(event::register);
    }
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent event) {
        for (String alias : new String[]{"gqc", "gashQuickCommands"})
            event.getDispatcher().register(Commands.literal(alias).executes(ctx -> { GqcClient.requestOpen(); return 1; }));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) GqcClient.tick();
    }
}
