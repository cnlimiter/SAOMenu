package com.sao.saomenu.api.forge;

import com.sao.saomenu.api.SaoUiRegistry;
import net.minecraftforge.eventbus.api.Event;

import java.util.Objects;

/**
 * Fired once on {@code MinecraftForge.EVENT_BUS}, on the first client tick after loading,
 * after builtin contributions and before freezing all registries. Subscribe on the Forge bus
 * with {@code @Mod.EventBusSubscriber(value = Dist.CLIENT)}. This is not a MOD-bus event.
 * Do not enqueue registration for later: the registration window ends when dispatch returns.
 */
public final class SaoUiRegisterEvent extends Event {
    private final SaoUiRegistry registry;

    public SaoUiRegisterEvent(SaoUiRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public SaoUiRegistry registry() {
        return registry;
    }
}
