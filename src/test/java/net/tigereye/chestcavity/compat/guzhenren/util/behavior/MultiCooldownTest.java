package net.tigereye.chestcavity.compat.guzhenren.util.behavior;

import net.tigereye.chestcavity.compat.guzhenren.item.common.OrganState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.function.LongUnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

class MultiCooldownTest {

    private static OrganState newState() {
        Map<String, Integer> ints = new HashMap<>();
        Map<String, Long> longs = new HashMap<>();
        OrganState state = mock(OrganState.class);

        lenient().when(state.getInt(anyString(), anyInt()))
                .thenAnswer(invocation -> ints.getOrDefault(invocation.getArgument(0), invocation.getArgument(1)));
        lenient().when(state.setInt(anyString(), anyInt(), any(IntUnaryOperator.class), anyInt()))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    int value = invocation.<IntUnaryOperator>getArgument(2).applyAsInt(invocation.getArgument(1));
                    int defaultValue = invocation.getArgument(3);
                    int previous = ints.getOrDefault(key, defaultValue);
                    ints.put(key, value);
                    return new OrganState.Change<>(previous, value);
                });

        lenient().when(state.getLong(anyString(), anyLong()))
                .thenAnswer(invocation -> longs.getOrDefault(invocation.getArgument(0), invocation.getArgument(1)));
        lenient().when(state.setLong(anyString(), anyLong(), any(LongUnaryOperator.class), anyLong()))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    long value = invocation.<LongUnaryOperator>getArgument(2).applyAsLong(invocation.getArgument(1));
                    long defaultValue = invocation.getArgument(3);
                    long previous = longs.getOrDefault(key, defaultValue);
                    longs.put(key, value);
                    return new OrganState.Change<>(previous, value);
                });

        return state;
    }

    @Test
    void hasLongReflectsStateMutation() {
        MultiCooldown cooldown = MultiCooldown.builder(newState())
                .withLongClamp(value -> Math.max(0L, value), 0L)
                .withIntClamp(value -> Math.max(0, value), 0)
                .build();

        MultiCooldown.Entry entry = cooldown.entry("LongKey")
                .withOnChange((previous, current) -> {
                    assertEquals(0L, previous.longValue());
                    assertEquals(25L, current.longValue());
                });

        assertFalse(cooldown.hasLong("LongKey"));
        entry.setReadyAt(25L);
        assertTrue(cooldown.hasLong("LongKey"));
        assertFalse(entry.isReady(10L));
        assertTrue(entry.isReady(30L));
    }

    @Test
    void hasIntTracksCountdownAndCallbacks() {
        MultiCooldown cooldown = MultiCooldown.builder(newState())
                .withLongClamp(value -> Math.max(0L, value), 0L)
                .withIntClamp(value -> Math.max(0, value), 0)
                .build();

        final int[] observed = new int[2];
        MultiCooldown.EntryInt entry = cooldown.entryInt("IntKey")
                .withOnChange((previous, current) -> {
                    observed[0] = previous;
                    observed[1] = current;
                });

        assertFalse(cooldown.hasInt("IntKey"));
        entry.setTicks(5);
        assertTrue(cooldown.hasInt("IntKey"));
        assertEquals(0, observed[0]);
        assertEquals(5, observed[1]);

        assertFalse(entry.isReady());
        assertTrue(entry.tickDown());
        assertEquals(4, entry.getTicks());
        assertTrue(entry.tickDown());
        assertEquals(3, entry.getTicks());
        entry.clear();
        assertTrue(entry.isReady());
    }
}
