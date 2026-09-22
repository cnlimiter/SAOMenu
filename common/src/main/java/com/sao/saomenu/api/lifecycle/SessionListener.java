package com.sao.saomenu.api.lifecycle;

import com.sao.saomenu.api.UiContribution;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Client-thread level transition callback, including connect, dimension replacement and disconnect.
 * A null previous/current level means no level. Release entity references and GPU resources owned
 * by the previous level here. This is not a server gameplay event or a per-render callback.
 */
public interface SessionListener extends UiContribution {
    void levelChanged(ClientLevel previous, ClientLevel current);

    static SessionListener of(ResourceLocation id, int order, BiConsumer<ClientLevel, ClientLevel> callback) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(callback, "callback");
        return new SessionListener() {
            @Override
            public ResourceLocation id() {
                return id;
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public void levelChanged(ClientLevel previous, ClientLevel current) {
                callback.accept(previous, current);
            }
        };
    }
}
