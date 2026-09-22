package com.sao.saomenu.client.runtime;

import com.sao.saomenu.api.UiContribution;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderedUiRegistryTest {
    private record Entry(ResourceLocation id, int order) implements UiContribution {
    }

    @Test
    void rejectsDuplicateWithoutReplacingOriginalAndOrdersTiesByNamespace() {
        OrderedUiRegistry<Entry> registry = new OrderedUiRegistry<>("sample");
        Entry last = new Entry(new ResourceLocation("zeta", "panel"), 20);
        Entry first = new Entry(new ResourceLocation("alpha", "panel"), 10);
        Entry tied = new Entry(new ResourceLocation("alpha", "other"), 20);
        registry.add(last);
        registry.add(first);
        registry.add(tied);
        assertThrows(IllegalArgumentException.class,
                () -> registry.add(new Entry(last.id(), -10)));
        registry.freeze();
        assertEquals(List.of(first, tied, last), registry.entries());
    }

    @Test
    void snapshotsCannotBeReadEarlyOrMutatedAfterFreeze() {
        OrderedUiRegistry<Entry> registry = new OrderedUiRegistry<>("sample");
        Entry original = new Entry(new ResourceLocation("example", "panel"), 0);
        assertThrows(IllegalStateException.class, registry::entries);
        registry.add(original);
        registry.freeze();
        List<Entry> visible = registry.entries();
        assertThrows(UnsupportedOperationException.class, visible::clear);
        assertThrows(IllegalStateException.class,
                () -> registry.add(new Entry(new ResourceLocation("example", "late"), 1)));
        assertEquals(List.of(original), visible);
    }
}
