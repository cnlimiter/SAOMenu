package com.sao.saomenu.client.runtime;

import com.sao.saomenu.api.UiContribution;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Client-thread startup builder; render paths read one immutable snapshot. */
final class OrderedUiRegistry<T extends UiContribution> {
    private final String domain;
    private final Map<ResourceLocation, T> pending = new HashMap<>();
    private List<T> snapshot;

    OrderedUiRegistry(String domain) {
        this.domain = domain;
    }

    void add(T contribution) {
        if (snapshot != null) {
            throw new IllegalStateException(domain + " registrations are frozen");
        }
        Objects.requireNonNull(contribution, "contribution");
        ResourceLocation id = Objects.requireNonNull(contribution.id(), "contribution ID");
        if (pending.putIfAbsent(id, contribution) != null) {
            throw new IllegalArgumentException("Duplicate " + domain + " registration: " + id);
        }
    }

    void freeze() {
        if (snapshot != null) {
            throw new IllegalStateException(domain + " registrations are already frozen");
        }
        List<T> ordered = new ArrayList<>(pending.values());
        ordered.sort(Comparator.comparingInt(UiContribution::order)
                .thenComparing(value -> value.id().toString()));
        snapshot = List.copyOf(ordered);
        pending.clear();
    }

    List<T> entries() {
        if (snapshot == null) {
            throw new IllegalStateException(domain + " registrations are not yet frozen");
        }
        return snapshot;
    }
}
