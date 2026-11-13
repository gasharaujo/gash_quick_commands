package net.gasharaujo.gqc;

import java.util.List;

public final class CommandExamples {
    public record Example(String name, List<String> lines) {
        public Example { lines = List.copyOf(lines); }
    }
    public static final List<Example> ALL = List.of(
        new Example("hello world", List.of("ola", "mundo")),
        new Example("Safe Gamerules", List.of(
            "/gamerule keepInventory true",
            "/gamerule mobGriefing false",
            "/gamerule lavaSourceConversion true",
            "/gamerule doImmediateRespawn true",
            "/gamerule playersSleepingPercentage 1")),
        new Example("FTBEssentials Unlimited Homes", List.of(
            "/ftbranks node add member ftbessentials.home.max 2147483647",
            "/ftbranks node add member ftbessentials.home.cooldown 0",
            "/ftbranks node add member ftbessentials.home.warmup 0")),
        new Example("FTBEssentials no cd/warmup", List.of(
            "/ftbranks node add member ftbessentials.back.cooldown 0",
            "/ftbranks node add member ftbessentials.back.warmup 0",
            "/ftbranks node add member ftbessentials.tpa.cooldown 0",
            "/ftbranks node add member ftbessentials.tpa.warmup 0",
            "/ftbranks node add member ftbessentials.spawn.cooldown 0",
            "/ftbranks node add member ftbessentials.spawn.warmup 0",
            "/ftbranks node add member ftbessentials.rtp.cooldown 0",
            "/ftbranks node add member ftbessentials.rtp.warmup 0",
            "/ftbranks node add member ftbessentials.warp.cooldown 0",
            "/ftbranks node add member ftbessentials.warp.warmup 0"))
    );
    private CommandExamples() { }
    public static String append(String current, Example example) {
        String added = String.join("\n", example.lines());
        if (current.isEmpty()) return added;
        return current + (current.endsWith("\n") ? "" : "\n") + added;
    }
}
