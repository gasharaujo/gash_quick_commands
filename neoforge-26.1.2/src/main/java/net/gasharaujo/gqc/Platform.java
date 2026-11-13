package net.gasharaujo.gqc;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.lwjgl.glfw.GLFW;

public final class Platform {
    public static void localMessage(net.minecraft.network.chat.Component text) {
        net.minecraft.client.Minecraft.getInstance().getChatListener().handleSystemMessage(text, false);
    }
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(net.minecraft.resources.Identifier.fromNamespaceAndPath(GqcClient.MOD_ID, "main"));
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.IntValue DELAY;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> REPORT;
    static {
        var b = new ModConfigSpec.Builder();
        DELAY = b.comment("Default delay between chat sends in milliseconds. Per-list -1 inherits this value.").defineInRange("defaultDelayMs", 1000, 0, ListStore.MAX_DELAY);
        REPORT = b.comment("Generated diagnostic: conflicting key binding names. Refreshed by the client; edits are replaced.").defineListAllowEmpty("keybindConflicts", List.of("No conflicts"), o -> o instanceof String);
        SPEC = b.build();
    }
    public static Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
    public static boolean configured() { return ModList.get().isLoaded("configured"); }
    public static int defaultDelay() { return DELAY.get(); }
    public static void defaultDelay(int value) { DELAY.set(value); SPEC.save(); }
    public static void updateReport(List<String> report) {
        if (!REPORT.get().equals(report)) { REPORT.set(report); SPEC.save(); }
    }
    public static KeyMapping createMapping(String name, String key, String modifier) {
        KeyMapping mapping = new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.UNKNOWN, CATEGORY);
        setBinding(mapping, key, modifier);
        return mapping;
    }
    public static void setBinding(KeyMapping mapping, String key, String modifier) {
        mapping.setKeyModifierAndCode(KeyModifier.valueOf(modifier), InputConstants.getKey(key));
    }
    public static String modifier(KeyMapping mapping) { return mapping.getKeyModifier().name(); }
    public static boolean conflicts(String key, String modifier, KeyMapping other) {
        var context = other.getKeyConflictContext();
        if (!KeyConflictContext.IN_GAME.conflicts(context) && !context.conflicts(KeyConflictContext.IN_GAME)) return false;
        var input = InputConstants.getKey(key);
        var mod = KeyModifier.valueOf(modifier);
        var otherMod = other.getKeyModifier();
        return (input.equals(other.getKey()) && (mod == otherMod || mod == KeyModifier.NONE || otherMod == KeyModifier.NONE))
            || (mod != KeyModifier.NONE && mod.matches(other.getKey()))
            || (otherMod != KeyModifier.NONE && otherMod.matches(input));
    }
    public static boolean isModifierKey(int code) { return code >= GLFW.GLFW_KEY_LEFT_SHIFT && code <= GLFW.GLFW_KEY_RIGHT_SUPER; }
    public static String modifierFromFlags(int flags) {
        if ((flags & GLFW.GLFW_MOD_CONTROL) != 0) return "CONTROL";
        if ((flags & GLFW.GLFW_MOD_ALT) != 0) return "ALT";
        if ((flags & GLFW.GLFW_MOD_SHIFT) != 0) return "SHIFT";
        return "NONE";
    }
}
