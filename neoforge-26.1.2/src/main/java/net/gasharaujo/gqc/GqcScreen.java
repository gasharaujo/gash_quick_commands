package net.gasharaujo.gqc;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Editor drafts survive resizes; only Save commits edits to disk. */
public final class GqcScreen extends Screen {
    private final Screen parent;
    private boolean settings, examples;
    private UUID selected;
    private String draftName, draftText = "", draftDelay = "-1";
    private String draftKey = "key.keyboard.unknown", draftModifier = "NONE";
    private String status = "";
    private boolean dirty, capturing, captureOpen;
    private int page, reportPage;
    private int left, panelWidth, side, contentX, contentWidth;
    private EditBox nameBox, delayBox, globalDelay;
    private MultiLineEditBox editor;
    private Button bindButton;

    public GqcScreen(Screen parent, boolean settings) {
        super(Component.translatable("gqc.title"));
        this.parent = parent;
        this.settings = settings;
        GqcClient.syncBindings();
        List<CommandList> lists = GqcClient.STORE.lists();
        if (!lists.isEmpty()) select(lists.get(0));
        else newDraft();
    }
    private Component t(String key, Object... args) { return Component.translatable(key, args); }
    private Button button(Component text, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(text, b -> action.run()).bounds(x, y, w, 20).build());
    }
    private int rows() { return Math.max(1, (height - 153) / 24); }
    private void select(CommandList l) {
        selected = l.id(); draftName = l.name(); draftText = String.join("\n", l.lines());
        draftDelay = Integer.toString(l.delayMs()); draftKey = l.key(); draftModifier = l.modifier(); dirty = false;
    }
    private void newDraft() {
        selected = null; draftName = GqcClient.STORE.nextName(); draftText = ""; draftDelay = "-1";
        draftKey = "key.keyboard.unknown"; draftModifier = "NONE"; dirty = false;
    }
    private void discardThen(Runnable action) {
        if (!dirty) { action.run(); return; }
        minecraft.setScreen(new ConfirmScreen(ok -> {
            minecraft.setScreen(this);
            if (ok) action.run();
        }, t("gqc.discard"), t("gqc.discard.detail")));
    }
    @Override protected void init() {
        clearWidgets();
        nameBox = null; delayBox = null; editor = null; globalDelay = null;
        panelWidth = Math.min(720, width - 16); left = (width - panelWidth) / 2;
        side = Math.min(174, panelWidth / 3); contentX = left + side + 12;
        contentWidth = panelWidth - side - 20;
        button(t("gqc.examples"), left + panelWidth - 86, 3, 86,
                () -> { examples = true; capturing = false; init(); });
        button(t("gqc.lists"), left, 27, panelWidth / 2 - 2, () -> { settings = false; examples = false; capturing = false; init(); });
        button(t("gqc.config"), left + panelWidth / 2 + 2, 27, panelWidth / 2 - 2,
                () -> { settings = true; examples = false; capturing = false; GqcClient.refreshReport(); init(); });
        if (examples) buildExamples(); else if (settings) buildSettings(); else buildEditor();
    }
    private void buildExamples() {
        int row = 0;
        for (CommandExamples.Example example : CommandExamples.ALL) {
            Button pick = button(Component.literal(example.name()), left + 8, 84 + row++ * 28, panelWidth - 16, () -> {
                draftText = CommandExamples.commands(example);
                dirty = true; settings = false; examples = false; status = "gqc.example_loaded"; init();
            });
            pick.setTooltip(Tooltip.create(Component.literal(String.join("\n", example.lines()))));
        }
        button(t("gui.back"), left + panelWidth - 98, height - 24, 90, () -> { examples = false; init(); });
    }
    private void buildEditor() {
        button(t("gqc.new"), left, 53, side, () -> discardThen(() -> { newDraft(); status = ""; init(); }));
        List<CommandList> lists = GqcClient.STORE.lists();
        page = Math.min(page, Math.max(0, (lists.size() - 1) / rows()));
        for (int i = page * rows(); i < Math.min(lists.size(), (page + 1) * rows()); i++) {
            CommandList l = lists.get(i);
            Button row = button(Component.literal((l.id().equals(selected) ? "> " : "") + l.name()),
                    left, 79 + (i % rows()) * 24, side - 24,
                    () -> discardThen(() -> { select(l); status = ""; capturing = false; init(); }));
            row.setTooltip(Tooltip.create(Component.literal(l.name() + " (" + l.lines().size() + ")")));
            addRenderableWidget(Button.builder(Component.literal("\u25B6"), b -> {
                if (GqcClient.run(List.of(l))) status = "gqc.started";
            }).bounds(left + side - 20, 79 + (i % rows()) * 24, 20, 20)
                    .tooltip(Tooltip.create(t("gqc.quick_cast.tip", l.name())))
                    .createNarration(ignored -> Component.translatable("gqc.quick_cast", l.name())).build());
        }
        button(Component.literal("<"), left, height - 72, 26, () -> { page = Math.max(0, page - 1); init(); });
        button(Component.literal(">"), left + side - 26, height - 72, 26,
                () -> { if ((page + 1) * rows() < lists.size()) page++; init(); });
        button(t("gqc.run_all"), left, height - 48, side, () -> {
            if (GqcClient.run(GqcClient.STORE.lists())) { status = "gqc.started"; }
        });
        button(t("gqc.stop"), left, height - 24, side, () -> { GqcClient.RUNNER.cancel(); status = "gqc.stopped"; });
        int delayWidth = Math.min(88, contentWidth / 3);
        nameBox = addRenderableWidget(new EditBox(font, contentX, 68, contentWidth - delayWidth - 8, 20, t("gqc.name")));
        nameBox.setMaxLength(64); nameBox.setValue(draftName);
        nameBox.setResponder(s -> { draftName = s; dirty = true; });
        delayBox = addRenderableWidget(new EditBox(font, contentX + contentWidth - delayWidth, 68, delayWidth, 20, t("gqc.delay")));
        delayBox.setMaxLength(8); delayBox.setValue(draftDelay);
        delayBox.setResponder(s -> { draftDelay = s; dirty = true; });
        delayBox.setTooltip(Tooltip.create(t("gqc.delay.tip")));
        editor = addRenderableWidget(MultiLineEditBox.builder().setX(contentX).setY(104).setPlaceholder(t("gqc.commands.hint")).build(font, contentWidth, Math.max(30, height - 201), t("gqc.commands")));
        editor.setCharacterLimit(262144); editor.setValue(draftText);
        editor.setValueListener(s -> { draftText = s; dirty = true; });
        bindButton = button(bindingLabel(false), contentX, height - 89, contentWidth,
                () -> { capturing = true; captureOpen = false; bindButton.setMessage(t("gqc.capture")); });
        bindButton.setTooltip(Tooltip.create(t("gqc.capture.tip")));
        int gap = 4, bw = (contentWidth - gap * 3) / 4;
        button(t("gqc.save"), contentX, height - 24, bw, this::save);
        Button run = button(t("gqc.run"), contentX + bw + gap, height - 24, bw, () -> {
            if (dirty) { status = "gqc.save_first"; return; }
            GqcClient.STORE.lists().stream().filter(l -> l.id().equals(selected)).findFirst().ifPresent(l -> {
                if (GqcClient.run(List.of(l))) status = "gqc.started";
            });
        });
        run.active = selected != null;
        Button delete = button(t("gqc.delete"), contentX + (bw + gap) * 2, height - 24, bw, () -> {
            minecraft.setScreen(new ConfirmScreen(ok -> {
                minecraft.setScreen(this);
                if (ok) {
                    try { GqcClient.STORE.delete(selected); GqcClient.rebuildBindings(); newDraft(); status = "gqc.deleted"; init(); }
                    catch (Exception e) { failure(e); }
                }
            }, t("gqc.delete.confirm"), Component.literal(draftName)));
        });
        delete.active = selected != null;
        button(t("gui.done"), contentX + (bw + gap) * 3, height - 24, bw, this::onClose);
    }
    private Component bindingLabel(boolean open) {
        String key = open ? GqcClient.OPEN.getKey().getName() : draftKey;
        String mod = open ? Platform.modifier(GqcClient.OPEN) : draftModifier;
        String display;
        try { display = InputConstants.getKey(key).getDisplayName().getString(); }
        catch (RuntimeException e) { display = "?"; }
        return t("gqc.binding", (mod.equals("NONE") ? "" : mod + " + ") + display);
    }
    private void save() {
        try {
            int delay = Integer.parseInt(draftDelay.strip());
            CommandList list = new CommandList(selected == null ? UUID.randomUUID() : selected,
                    draftName.strip(), ListStore.parseLines(draftText), delay, draftKey, draftModifier);
            GqcClient.STORE.save(list); GqcClient.rebuildBindings(); select(list);
            status = "gqc.saved"; init();
        } catch (NumberFormatException e) { status = "gqc.error.delay"; }
        catch (IllegalArgumentException e) { status = e.getMessage(); }
        catch (Exception e) { failure(e); }
    }
    private void failure(Exception e) { GqcClient.LOG.error("Could not save lists", e); status = "gqc.error.save"; }
    private void buildSettings() {
        globalDelay = addRenderableWidget(new EditBox(font, left + 8, 73, 100, 20, t("gqc.default_delay")));
        globalDelay.setMaxLength(8); globalDelay.setValue(Integer.toString(Platform.defaultDelay()));
        button(t("gqc.apply"), left + 114, 73, 70, () -> {
            try {
                int value = Integer.parseInt(globalDelay.getValue());
                if (value < 0 || value > ListStore.MAX_DELAY) throw new NumberFormatException();
                Platform.defaultDelay(value); status = "gqc.saved";
            } catch (NumberFormatException e) { status = "gqc.error.delay"; }
        });
        bindButton = button(bindingLabel(true), left + 8, 111, panelWidth - 16,
                () -> { capturing = true; captureOpen = true; bindButton.setMessage(t("gqc.capture")); });
        bindButton.setTooltip(Tooltip.create(t("gqc.capture.tip")));
        button(Component.literal("<"), left + 8, height - 24, 30, () -> { reportPage = Math.max(0, reportPage - 1); });
        button(Component.literal(">"), left + 42, height - 24, 30, () -> { reportPage++; });
        button(t("gui.done"), left + panelWidth - 98, height - 24, 90, this::onClose);
    }
    private boolean capture(int keyCode, int scanCode, int mods) {
        if (!capturing) return false;
        if (Platform.isModifierKey(keyCode)) return true;
        String key = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE
                ? "key.keyboard.unknown" : InputConstants.getKey(new KeyEvent(keyCode, scanCode, mods)).getName();
        acceptBinding(key, key.equals("key.keyboard.unknown") ? "NONE" : Platform.modifierFromFlags(mods));
        return true;
    }
    private void acceptBinding(String key, String mod) {
        if (captureOpen) {
            Platform.setBinding(GqcClient.OPEN, key, mod);
            net.minecraft.client.KeyMapping.resetMapping(); minecraft.options.save(); GqcClient.refreshReport();
        } else { draftKey = key; draftModifier = mod; dirty = true; }
        capturing = false; bindButton.setMessage(bindingLabel(captureOpen));
    }
    @Override public boolean keyPressed(KeyEvent event) {
        return capture(event.key(), event.scancode(), event.modifiers()) || super.keyPressed(event);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        if (capturing) { acceptBinding(InputConstants.Type.MOUSE.getOrCreate(button).getName(), "NONE"); return true; }
        return super.mouseClicked(event, doubleClick);
    }
    private void text(GuiGraphicsExtractor g, Component text, int x, int y, int maxWidth, int color) {
        g.text(font, font.plainSubstrByWidth(text.getString(), Math.max(10, maxWidth)), x, y, color, false);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, width, height, 0xEE101823);
        g.fill(left - 4, 2, left + panelWidth + 4, 22, 0xFF203A4B);
        text(g, title, left + 8, 8, panelWidth - 102, 0xFF7FE6D1);
        if (examples) {
            text(g, t("gqc.examples.hint"), left + 8, 58, panelWidth - 16, 0xFFFFFFFF);
            text(g, t("gqc.examples.save_hint"), left + 8, 71, panelWidth - 16, 0xFF91A6BA);
        } else if (settings) {
            text(g, t("gqc.default_delay"), left + 8, 58, panelWidth - 16, 0xFFFFFFFF);
            text(g, t("key.gqc.open"), left + 8, 99, panelWidth - 16, 0xFFFFFFFF);
            text(g, t(Platform.configured() ? "gqc.configured.present" : "gqc.configured.optional"), left + 8, 137, panelWidth - 16, 0xFF91A6BA);
            // Render wrapped components directly to preserve the game's font layout.
            var wrapped = GqcClient.report().stream().flatMap(s -> font.split(Component.literal(s), panelWidth - 24).stream()).toList();
            int visible = Math.max(1, (height - 202) / 11);
            reportPage = Math.min(reportPage, Math.max(0, (wrapped.size() - 1) / visible));
            for (int i = reportPage * visible; i < Math.min(wrapped.size(), (reportPage + 1) * visible); i++)
                g.text(font, wrapped.get(i), left + 8, 157 + (i % visible) * 11, 0xFFFFC477, false);
        } else {
            text(g, t("gqc.name"), contentX, 56, contentWidth - 96, 0xFFFFFFFF);
            text(g, t("gqc.delay"), contentX + contentWidth - Math.min(88, contentWidth / 3), 56, 88, 0xFFFFFFFF);
            text(g, t("gqc.commands"), contentX, 93, contentWidth, 0xFF91A6BA);
            List<String> conflicts = GqcClient.conflicts(draftKey, draftModifier, selected, false);
            Component conflictText = conflicts.isEmpty() ? t("gqc.no_conflicts") : t("gqc.conflicts", String.join(", ", conflicts));
            text(g, conflictText, contentX, height - 64, contentWidth, conflicts.isEmpty() ? 0xFF7FE6D1 : 0xFFFFC477);
            if (!capturing && mouseX >= contentX && mouseX <= contentX + contentWidth && mouseY >= height - 65 && mouseY <= height - 53)
                g.setTooltipForNextFrame(font, font.split(conflictText, Math.min(340, width - 32)), mouseX, mouseY);
            text(g, t("gqc.remaining", GqcClient.RUNNER.remaining()), contentX, height - 50, contentWidth, 0xFF91A6BA);
        }
        if (!examples && !status.isEmpty()) text(g, t(status), settings ? left + 82 : contentX, height - 37, settings ? panelWidth - 90 : contentWidth, 0xFFFFFFFF);
        super.extractRenderState(g, mouseX, mouseY, partialTick);
    }
    @Override public void onClose() { discardThen(() -> minecraft.setScreen(parent)); }
    @Override public boolean isPauseScreen() { return false; }
}
