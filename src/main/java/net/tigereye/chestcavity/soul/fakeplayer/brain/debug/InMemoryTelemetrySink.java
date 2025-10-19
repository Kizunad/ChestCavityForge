package net.tigereye.chestcavity.soul.fakeplayer.brain.debug;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fixed size ring buffer sink useful for diagnostics and tests.
 */
public final class InMemoryTelemetrySink implements BrainTelemetrySink {

    private final Deque<BrainDebugEvent> buffer;
    private final int capacity;

    public InMemoryTelemetrySink(int capacity) {
        this.capacity = Math.max(1, capacity);
        this.buffer = new ArrayDeque<>(this.capacity);
    }

    @Override
    public synchronized void publish(BrainDebugEvent event) {
        if (buffer.size() == capacity) {
            buffer.removeFirst();
        }
        buffer.addLast(event);
    }

    public synchronized List<BrainDebugEvent> snapshot() {
        return buffer.stream().collect(Collectors.toList());
    }

    public synchronized Collection<BrainDebugEvent> view() {
        return List.copyOf(buffer);
    }

    public synchronized void clear() {
        buffer.clear();
    }
}
