package net.gasharaujo.gqc;

import java.nio.file.*;
import java.io.IOException;
import java.util.*;

public final class CoreChecks {
    private static int checks;
    private static void check(boolean ok, String why) {
        checks++; if (!ok) throw new AssertionError(why);
    }
    private static CommandList list(String name, int delay, String... lines) {
        return new CommandList(UUID.randomUUID(), name, List.of(lines), delay, "key.keyboard.unknown", "NONE");
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("gqc-checks-");
        try {
            Path file = dir.resolve("lists.json");
            ListStore store = new ListStore(file);
            store.load();
            check(store.nextName().equals("list 1"), "initial automatic name");
            CommandList first = list(store.nextName(), -1, "/say one", "/say two");
            store.save(first);
            check(store.nextName().equals("list 2"), "increment after save");
            store.save(new CommandList(first.id(), first.name(), List.of("/home"), 100, first.key(), first.modifier()));
            check(store.nextName().equals("list 2"), "editing does not increment");
            try { store.save(list("LIST 1", -1, "/spawn")); throw new AssertionError("duplicate accepted"); }
            catch (IllegalArgumentException expected) { checks++; }
            for (CommandList bad : List.of(list("bad", -2, "x"), list("bad", 0), list("bad", 0, "x".repeat(257)), list(" ", 1, "x"))) {
                try { store.save(bad); throw new AssertionError("invalid list accepted"); }
                catch (IllegalArgumentException expected) { checks++; }
            }
            check(store.lists().size() == 1, "invalid saves leave state unchanged");
            ListStore reload = new ListStore(file); reload.load();
            check(reload.lists().equals(store.lists()), "persistence round trip");
            check(ListStore.parseLines(" /a\r\n\n  /b  \n hello ").equals(List.of("/a", "/b", "hello")), "one line one send / blank lines");
            store.delete(first.id());
            check(store.nextName().equals("list 2"), "deleted numbers not reused");
            store.save(list("list 4", 0, "hello"));
            store.save(list(store.nextName(), 0, "x"));
            check(store.nextName().equals("list 5"), "automatic names skip occupied numbers");
            String invalid = "{invalid";
            Files.writeString(file, invalid);
            try { new ListStore(file).load(); throw new AssertionError("invalid JSON accepted"); }
            catch (IOException expected) { checks++; }
            check(Files.readString(file).equals(invalid), "corrupt original preserved");
            try (var files = Files.list(dir)) { check(files.anyMatch(p -> p.getFileName().toString().contains(".invalid-")), "corrupt backup saved"); }
            SequenceRunner runner = new SequenceRunner();
            List<String> sent = new ArrayList<>();
            check(runner.start(List.of(first, list("second", 50, "plain chat", "/last")), 100), "start");
            check(!runner.start(List.of(first), 0), "running sequence cannot be replaced");
            runner.tick(0, true, sent::add);
            check(sent.equals(List.of("/say one")), "first line immediately");
            runner.tick(99_000_000L, true, sent::add);
            check(sent.size() == 1, "default delay respected");
            runner.tick(100_000_000L, true, sent::add);
            check(sent.get(1).equals("/say two"), "second line in order");
            runner.tick(199_000_000L, true, sent::add);
            check(sent.size() == 2, "delay across lists");
            runner.tick(200_000_000L, true, sent::add);
            runner.tick(249_000_000L, true, sent::add);
            check(sent.size() == 3, "per-list override");
            runner.tick(250_000_000L, true, sent::add);
            check(sent.equals(List.of("/say one", "/say two", "plain chat", "/last")), "all list order");
            runner.start(List.of(first), 0); runner.tick(300_000_000L, true, sent::add);
            check(runner.remaining() == 1, "zero delay does not burst");
            runner.tick(400_000_000L, false, sent::add);
            check(runner.remaining() == 0, "disconnect cancels pending sends");
            runner.start(List.of(first), 1); runner.cancel();
            check(runner.remaining() == 0, "manual cancel");
            runner.start(List.of(first), 1);
            try { runner.tick(500_000_000L, true, s -> { throw new IllegalStateException(); }); }
            catch (IllegalStateException expected) { checks++; }
            check(runner.remaining() == 0, "send failure cancels");
            List<String> commands = new ArrayList<>(), chat = new ArrayList<>(), history = new ArrayList<>();
            ChatDispatch.send("/say hello", commands::add, chat::add, history::add);
            ChatDispatch.send("hello", commands::add, chat::add, history::add);
            ChatDispatch.send("/gqc", commands::add, chat::add, history::add);
            ChatDispatch.send("", commands::add, chat::add, history::add);
            check(commands.equals(List.of("say hello", "gqc")), "command slash removed exactly once / client alias routed");
            check(chat.equals(List.of("hello")), "plain chat uses chat entry point");
            check(history.equals(List.of("/say hello", "hello", "/gqc")), "each nonblank line recorded once");
            check(CommandExamples.ALL.size() == 4, "four requested examples");
            check(CommandExamples.append("", CommandExamples.ALL.get(0)).equals("ola\nmundo"), "hello example preserves two sends");
            check(CommandExamples.append("/existing", CommandExamples.ALL.get(0)).equals("/existing\nola\nmundo"), "example appends without replacing draft");
            check(CommandExamples.append("/existing\n", CommandExamples.ALL.get(0)).equals("/existing\nola\nmundo"), "example avoids extra empty line");
            check(CommandExamples.ALL.get(1).lines().size() == 5 && CommandExamples.ALL.get(2).lines().size() == 3
                && CommandExamples.ALL.get(3).lines().size() == 10, "requested command counts");
            System.out.println("PASS: " + checks + " core checks");
        } finally {
            try (var files = Files.walk(dir)) {
                for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
            }
        }
    }
}
