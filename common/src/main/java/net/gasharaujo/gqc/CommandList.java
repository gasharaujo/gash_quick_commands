package net.gasharaujo.gqc;

import java.util.List;
import java.util.UUID;

public record CommandList(UUID id, String name, List<String> lines, int delayMs, String key, String modifier) {
    public CommandList {
        lines = List.copyOf(lines);
    }
    public CommandList withBinding(String key, String modifier) {
        return new CommandList(id, name, lines, delayMs, key, modifier);
    }
}
