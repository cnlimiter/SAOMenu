package com.sao.saomenu.forge;

import com.sao.saomenu.forge.registry.SAOMenuForgeParticles;
import com.sao.saomenu.forge.registry.SAOMenuForgeSounds;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;

/**
 * ExpectPlatform 实现(Forge):音效事件来自 DeferredRegister。
 *
 * <p>Architectury 约定实现类位于 {@code <包>.<平台>.<类名>Impl};
 * 放在 common 的同名包下会与 common jar 触发 JPMS 分包冲突。</p>
 */
public class SAOMenuPlatformImpl {

    public static SoundEvent launcherSound() {
        return SAOMenuForgeSounds.LAUNCHER.get();
    }

    public static SoundEvent clickSound() {
        return SAOMenuForgeSounds.CLICK.get();
    }

    public static SoundEvent panelSound() {
        return SAOMenuForgeSounds.PANEL.get();
    }

    public static SoundEvent alertSound() {
        return SAOMenuForgeSounds.ALERT.get();
    }

    public static SimpleParticleType shardParticle() {
        return SAOMenuForgeParticles.SHARD.get();
    }

    public static SimpleParticleType glowParticle() {
        return SAOMenuForgeParticles.GLOW.get();
    }
}
