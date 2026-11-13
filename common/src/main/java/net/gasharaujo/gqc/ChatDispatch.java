package net.gasharaujo.gqc;

import java.util.function.Consumer;

public final class ChatDispatch {
    private ChatDispatch() { }
    public static void send(String line, Consumer<String> commands, Consumer<String> chat, Consumer<String> history) {
        if (line.isBlank()) return;
        history.accept(line);
        if (line.startsWith("/")) commands.accept(line.substring(1));
        else chat.accept(line);
    }
}
