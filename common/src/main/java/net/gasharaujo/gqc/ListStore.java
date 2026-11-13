package net.gasharaujo.gqc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Local client data only. Writes are committed before the in-memory state changes. */
public final class ListStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final int MAX_DELAY = 3_600_000;
    private final Path file;
    private Data data = new Data();
    public ListStore(Path file) { this.file = file; }
    private static final class Data {
        int schema = 1;
        long nextList = 1;
        List<CommandList> lists = new ArrayList<>();
    }
    public void load() throws IOException {
        if (!Files.exists(file)) return;
        try {
            Data loaded = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Data.class);
            if (loaded == null || loaded.schema != 1 || loaded.lists == null || loaded.nextList < 1)
                throw new IllegalArgumentException("Invalid data/schema");
            Set<UUID> ids = new HashSet<>();
            Set<String> names = new HashSet<>();
            for (CommandList list : loaded.lists) {
                validate(list);
                if (!ids.add(list.id()) || !names.add(list.name().toLowerCase(Locale.ROOT)))
                    throw new IllegalArgumentException("Duplicate id/name");
            }
            data = loaded;
        } catch (RuntimeException ex) {
            // Preserve the user's original file and prevent silent data loss.
            Path backup = file.resolveSibling(file.getFileName() + ".invalid-" + System.currentTimeMillis());
            Files.copy(file, backup);
            throw new IOException("Invalid lists file; backup: " + backup, ex);
        }
    }
    public List<CommandList> lists() { return List.copyOf(data.lists); }
    public String nextName() {
        long n = data.nextList;
        while (hasName("list " + n, null)) n++;
        return "list " + n;
    }
    public boolean hasName(String name, UUID except) {
        return data.lists.stream().anyMatch(l -> !l.id().equals(except) && l.name().equalsIgnoreCase(name.trim()));
    }
    public static List<String> parseLines(String text) {
        return text.lines().map(String::strip).filter(s -> !s.isEmpty()).toList();
    }
    private static void validate(CommandList l) {
        if (l.id() == null || l.name() == null || l.name().isBlank() || l.name().length() > 64
                || !l.name().equals(l.name().strip()) || l.key() == null || l.modifier() == null
                || l.delayMs() < -1 || l.delayMs() > MAX_DELAY || l.lines().isEmpty())
            throw new IllegalArgumentException("gqc.error.invalid");
        for (String s : l.lines()) {
            if (s.isBlank() || s.length() > 256 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0
                    || s.chars().anyMatch(c -> c < 32 || c == 127 || c == 167))
                throw new IllegalArgumentException("gqc.error.line");
        }
    }
    public void save(CommandList list) throws IOException {
        validate(list);
        if (hasName(list.name(), list.id())) throw new IllegalArgumentException("gqc.error.name");
        Data updated = copy();
        int index = -1;
        for (int i = 0; i < updated.lists.size(); i++) if (updated.lists.get(i).id().equals(list.id())) index = i;
        if (index < 0) updated.lists.add(list); else updated.lists.set(index, list);
        if (index < 0) {
            long used = data.nextList;
            while (hasName("list " + used, null)) used++;
            updated.nextList = used + 1;
        }
        commit(updated);
    }
    public void delete(UUID id) throws IOException {
        Data updated = copy();
        updated.lists.removeIf(l -> l.id().equals(id));
        commit(updated);
    }
    public void bindings(Map<UUID, CommandList> changes) throws IOException {
        Data updated = copy();
        updated.lists.replaceAll(l -> changes.getOrDefault(l.id(), l));
        commit(updated);
    }
    private Data copy() {
        Data result = new Data();
        result.nextList = data.nextList;
        result.lists = new ArrayList<>(data.lists);
        return result;
    }
    private void commit(Data updated) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "gqc-", ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(updated), StandardCharsets.UTF_8);
            try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
            data = updated;
        } finally { Files.deleteIfExists(temp); }
    }
}
