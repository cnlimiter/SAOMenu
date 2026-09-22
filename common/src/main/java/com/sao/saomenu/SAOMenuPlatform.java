package com.sao.saomenu;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;

/**
 * Architectury 平台桥接；当前发行目标为 Forge，具体注册与 accessor 实现在 forge 模块。
 */
public class SAOMenuPlatform {

    @ExpectPlatform
    public static SoundEvent launcherSound() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static SoundEvent clickSound() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static SoundEvent panelSound() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static SoundEvent alertSound() {
        throw new AssertionError();
    }

    /** SAO 死亡碎裂的碎片粒子类型。 */
    @ExpectPlatform
    public static SimpleParticleType shardParticle() {
        throw new AssertionError();
    }

    /** SAO 死亡碎裂的中心闪光粒子类型。 */
    @ExpectPlatform
    public static SimpleParticleType glowParticle() {
        throw new AssertionError();
    }
}
