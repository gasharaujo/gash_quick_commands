package net.gasharaujo.gqc;

import java.util.*;
import java.util.function.Consumer;

/** Driven by the client tick; never sleeps or blocks the render thread. */
public final class SequenceRunner {
    private record Send(String line, long delayNanos) { }
    private final Deque<Send> queue = new ArrayDeque<>();
    private long nextSend;
    private boolean waiting;
    public boolean start(List<CommandList> lists, int defaultDelay) {
        if (!queue.isEmpty()) return false;
        for (CommandList list : lists) {
            int delay = list.delayMs() < 0 ? defaultDelay : list.delayMs();
            for (String line : list.lines()) queue.add(new Send(line, Math.max(0, delay) * 1_000_000L));
        }
        waiting = false;
        return true;
    }
    public void tick(long nowNanos, boolean connected, Consumer<String> sender) {
        if (!connected) { cancel(); return; }
        if (queue.isEmpty() || (waiting && nowNanos - nextSend < 0)) return;
        Send next = queue.removeFirst();
        // At most one message per tick, including when delay is zero or the game lags.
        try { sender.accept(next.line()); }
        catch (RuntimeException ex) { cancel(); throw ex; }
        nextSend = nowNanos + next.delayNanos();
        waiting = true;
    }
    public int remaining() { return queue.size(); }
    public void cancel() { queue.clear(); waiting = false; }
}
