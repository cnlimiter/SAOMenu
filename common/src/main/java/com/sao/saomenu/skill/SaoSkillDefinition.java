package com.sao.saomenu.skill;

import java.util.Objects;

/** Gameplay identity and timing, independent of client callbacks and UI registration. */
public record SaoSkillDefinition(String id, int cooldownTicks) {
    public SaoSkillDefinition {
        Objects.requireNonNull(id, "id");
        if (id.isBlank() || cooldownTicks < 0) {
            throw new IllegalArgumentException("A skill needs an id and a non-negative cooldown");
        }
    }
}
