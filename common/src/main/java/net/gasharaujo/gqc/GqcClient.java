package net.gasharaujo.gqc;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.util.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GqcClient {
    public static final String MOD_ID = "gash_quick_commands";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);
    public static final SequenceRunner RUNNER = new SequenceRunner();
    public static final Map<UUID, KeyMapping> BINDINGS = new LinkedHashMap<>();
    public static ListStore STORE;
    public static KeyMapping OPEN;
    private static boolean openRequested;
    private static Object connection;
    private static String loadError;
    private static int ticks;
    private GqcClient() { }
    public static void initialize() {
        STORE = new ListStore(Platform.configDirectory().resolve(MOD_ID + "/lists.json"));
        try { STORE.load(); }
        catch (IOException e) { loadError = e.getMessage(); LOG.error("Could not load command lists", e); }
        OPEN = Platform.createMapping("key.gqc.open", "key.keyboard.unknown", "NONE");
        for (CommandList l : STORE.lists()) BINDINGS.put(l.id(), createMapping(l));
    }
    private static KeyMapping createMapping(CommandList list) {
        try { return Platform.createMapping("GQC: " + list.name(), list.key(), list.modifier()); }
        catch (RuntimeException e) {
            LOG.warn("Invalid key binding for {}. Leaving it unbound.", list.name());
            return Platform.createMapping("GQC: " + list.name(), "key.keyboard.unknown", "NONE");
        }
    }
    public static void requestOpen() { openRequested = true; }
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null || STORE == null) return;
        if (connection != mc.getConnection()) {
            RUNNER.cancel();
            connection = mc.getConnection();
        }
        if (openRequested) { openRequested = false; mc.setScreen(new GqcScreen(null, false)); }
        if (OPEN.consumeClick() && mc.screen == null) requestOpen();
        for (CommandList list : STORE.lists()) {
            KeyMapping key = BINDINGS.get(list.id());
            if (key != null && key.consumeClick() && mc.screen == null && mc.player != null) run(List.of(list));
        }
        if (++ticks % 20 == 0) {
            syncBindings();
            refreshReport();
        }
        try {
            RUNNER.tick(System.nanoTime(), mc.player != null && mc.getConnection() != null, GqcClient::sendLine);
        } catch (RuntimeException e) {
            LOG.error("Command sequence stopped", e);
            message("gqc.error.send");
        }
        if (loadError != null && mc.player != null) {
            message("gqc.error.load", loadError);
            loadError = null;
        }
    }
    public static void syncBindings() {
        Map<UUID, CommandList> changed = new HashMap<>();
        for (CommandList l : STORE.lists()) {
            KeyMapping k = BINDINGS.get(l.id());
            if (k != null && (!l.key().equals(k.getKey().getName()) || !l.modifier().equals(Platform.modifier(k))))
                changed.put(l.id(), l.withBinding(k.getKey().getName(), Platform.modifier(k)));
        }
        if (!changed.isEmpty()) {
            try { STORE.bindings(changed); }
            catch (IOException e) { LOG.error("Could not save changed controls", e); message("gqc.error.save"); }
        }
    }
    public static void rebuildBindings() {
        Minecraft mc = Minecraft.getInstance();
        Set<KeyMapping> old = new HashSet<>(BINDINGS.values());
        List<KeyMapping> keys = new ArrayList<>(Arrays.asList(mc.options.keyMappings));
        keys.removeIf(old::contains);
        for (KeyMapping k : old) k.setKey(InputConstants.UNKNOWN);
        BINDINGS.clear();
        for (CommandList list : STORE.lists()) {
            KeyMapping key = createMapping(list);
            BINDINGS.put(list.id(), key);
            keys.add(key);
        }
        mc.options.keyMappings = keys.toArray(KeyMapping[]::new);
        KeyMapping.resetMapping();
        mc.options.save();
        refreshReport();
    }
    public static List<String> conflicts(String key, String modifier, UUID except, boolean forOpen) {
        if (key.equals("key.keyboard.unknown")) return List.of();
        List<String> result = new ArrayList<>();
        KeyMapping ignored = except == null ? null : BINDINGS.get(except);
        for (KeyMapping other : Minecraft.getInstance().options.keyMappings) {
            if (other == ignored || (forOpen && other == OPEN) || other.isUnbound()) continue;
            if (Platform.conflicts(key, modifier, other)) result.add(I18n.get(other.getName()));
        }
        return result.stream().distinct().toList();
    }
    public static List<String> report() {
        List<String> lines = new ArrayList<>();
        for (var entry : BINDINGS.entrySet()) {
            KeyMapping k = entry.getValue();
            List<String> c = conflicts(k.getKey().getName(), Platform.modifier(k), entry.getKey(), false);
            if (!c.isEmpty()) lines.add(I18n.get(k.getName()) + " -> " + String.join(", ", c));
        }
        List<String> open = conflicts(OPEN.getKey().getName(), Platform.modifier(OPEN), null, true);
        if (!open.isEmpty()) lines.add(I18n.get("key.gqc.open") + " -> " + String.join(", ", open));
        if (lines.isEmpty()) lines.add(I18n.get("gqc.no_conflicts"));
        return lines;
    }
    public static void refreshReport() { Platform.updateReport(report()); }
    public static boolean run(List<CommandList> lists) {
        if (Minecraft.getInstance().player == null) { message("gqc.error.world"); return false; }
        if (lists.isEmpty()) return false;
        if (!RUNNER.start(lists, Platform.defaultDelay())) { message("gqc.error.busy"); return false; }
        return true;
    }
    public static void message(String key, Object... args) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) Platform.localMessage(Component.translatable(key, args));
    }
    private static void sendLine(String line) {
        Minecraft mc = Minecraft.getInstance();
        var network = mc.getConnection();
        if (network == null) { RUNNER.cancel(); return; }
        // These are the same connection entry points used by vanilla chat. Loader hooks
        // dispatch client commands and allow other chat mods to process normal messages.
        ChatDispatch.send(line, network::sendCommand, network::sendChat, mc.gui.getChat()::addRecentChat);
    }
    public static Screen configScreen(Screen parent) { syncBindings(); return new GqcScreen(parent, true); }
}
