package com.sao.saomenu.client.input;

import com.sao.saomenu.client.effect.SAODeathEffect;
import com.sao.saomenu.client.effect.SAOWelcome;
import com.sao.saomenu.client.menu.SAOMenuScreen;
import com.sao.saomenu.client.screen.settings.SAOSettingsScreen;
import com.sao.saomenu.client.skill.SAODualWield;
import com.sao.saomenu.config.SAOConfig;

import com.mojang.blaze3d.platform.InputConstants;
import com.sao.saomenu.SAOMenu;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * 打开菜单的按键(默认 O)与逐帧检测。
 *
 * <p>Forge 侧按键注册必须走原生的 {@code RegisterKeyMappingsEvent}(见
 * {@code com.sao.saomenu.forge.client.SAOMenuForgeClient});Architectury 的 {@link KeyMappingRegistry} 在
 * FMLClientSetupEvent 阶段调用会抛 "registered after event"。</p>
 */
public final class SAOKeybinds {

    public static final String CATEGORY = "key.categories." + SAOMenu.MOD_ID;

    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key." + SAOMenu.MOD_ID + ".open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY);

    private static boolean tickHooked = false;

    /** 上一 tick 是否有客户端玩家:用于在世界切换时一次性清掉纯客户端的显示状态。 */
    private static boolean hadPlayer = false;

    /** 技能快捷键:槽位 i 对应注册表里第 i 个技能(未绑定按键 = 不触发)。 */
    public static final KeyMapping[] SKILL_KEYS =
            new KeyMapping[com.sao.saomenu.client.skill.SaoSkills.HOTKEY_SLOTS];

    static {
        for (int i = 0; i < SKILL_KEYS.length; i++) {
            SKILL_KEYS[i] = new KeyMapping("key." + SAOMenu.MOD_ID + ".skill_slot" + (i + 1),
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
        }
    }

    private SAOKeybinds() {
    }

    /** Fabric:初始化时注册按键 + 逐帧检测(Architectury 在 init 阶段可安全注册)。 */
    public static void register() {
        KeyMappingRegistry.register(OPEN_MENU);
        registerTickHandler();
    }

    /** Forge:按键由 RegisterKeyMappingsEvent 注册，这里挂客户端逐帧检测。 */
    public static void registerTickHandler() {
        if (tickHooked) {
            return;
        }
        tickHooked = true;
        // 加载客户端配置(锚点/缩放/浮动/音效/HUD),供布局与渲染读取
        SAOConfig.load(Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("saomenu.json"));
        // 外部主题:config/saomenu/themes/*.json(坏文件只跳过它自己,内置预设不受影响)
        com.sao.saomenu.ui.theme.SaoThemeLibrary.load(
                Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("saomenu"));
        // 注册内置面板(个人/队伍/好友/设置);顺序即主按钮列顺序
        com.sao.saomenu.client.menu.SaoMenuRegistry.registerBuiltins();
        // 注册内置技能(二刀流 + 六项占位剑技);顺序即技能列顺序
        com.sao.saomenu.client.skill.SaoSkillRegistry.registerBuiltins();
        ClientTickEvent.CLIENT_POST.register(client -> {
            // 进入世界检测:无世界→有世界时播放 SAO 欢迎动画
            SAOWelcome.clientTick(client);
            // 退出世界:清空技能冷却显示(计时基准是本地毫秒,不跨世界保留)。
            // 只在"有→无"那一次执行,不在无世界期间逐 tick 重复清空。
            if (client.player == null) {
                if (hadPlayer) {
                    hadPlayer = false;
                    com.sao.saomenu.client.skill.SaoSkillClientState.reset();
                }
            } else {
                hadPlayer = true;
            }
            // Alt 自由观察逐 tick 轮询(进入/退出/锁定状态机)
            SAOFreeLook.tick(client);
            // 生物死亡检测:死亡当帧爆散蓝色碎片
            SAODeathEffect.clientTick(client);
            // 二刀流:装备包发出后延后几 tick 再切史诗战斗的战斗模式
            SAODualWield.tick();
            // 技能快捷键:槽位 i → 注册表第 i 个技能,与菜单项走同一激活入口
            for (int i = 0; i < SKILL_KEYS.length; i++) {
                if (client.player != null && SKILL_KEYS[i].consumeClick()) {
                    java.util.List<com.sao.saomenu.client.skill.SaoSkill> skills =
                            com.sao.saomenu.client.skill.SaoSkillRegistry.skills();
                    if (i < skills.size()) {
                        com.sao.saomenu.client.skill.SaoSkills.activate(client.player, skills.get(i));
                    }
                }
            }
            while (OPEN_MENU.consumeClick()) {
                if (client.player == null) {
                    continue;
                }
                if (client.screen == null) {
                    client.setScreen(new SAOMenuScreen());
                } else if (client.screen instanceof SAOMenuScreen) {
                    // 再按一次 O 关闭菜单(走关闭动画)
                    client.screen.onClose();
                } else if (client.screen instanceof SAOSettingsScreen) {
                    // 模组设置内按 O:返回上一级菜单。
                    client.screen.onClose();
                }
            }
        });
    }
}
