# 客户端 UI API

目标版本：Minecraft 1.20.1、Forge 47.3.0、Java 17。公共契约位于 `com.sao.saomenu.api`；Forge 事件位于 `com.sao.saomenu.api.forge`。`client.*`、`ui.*`、Mixin 和开发预览不是附属模组依赖面。

## 注册窗口

订阅 **Forge 总线**上的 `SaoUiRegisterEvent`，不是 MOD 总线。事件在加载完成、Forge 总线启用后的首个客户端 tick 同步触发；内置贡献先注册，附属模组随后注册，事件返回后统一冻结。不能在 `FMLClientSetupEvent` 中向尚未启用的 Forge 总线投递它。

- 每个领域用 `ResourceLocation` 标识贡献；使用自己的模组命名空间。
- 同领域重复 ID 抛出异常，不覆盖、不静默忽略。
- 顺序按 `order()` 升序，再按完整 ID 的字典序；不依赖模组发现顺序或事件监听器顺序。
- 必须在事件回调中同步注册。不要再次 `enqueueWork`、异步注册或保留注册器以供稍后修改。
- 冻结后返回不可变、有序快照；渲染期间不重新排序和复制注册表。
- UI 文案使用 `Component`；动态名字使用 `Component.literal`，翻译键使用 `Component.translatable`。不再凭文本中是否有点号猜测它是翻译键。

```java
package example.client;

import com.sao.saomenu.api.forge.SaoUiRegisterEvent;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.theme.ThemeTokens;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "example", value = Dist.CLIENT)
public final class ClientUi {
    @SubscribeEvent
    public static void register(SaoUiRegisterEvent event) {
        event.registry().theme(new ThemeDefinition(
                new ResourceLocation("example", "ice"), 900,
                Component.literal("Ice"), 202f, ThemeTokens.sao()));
    }
}
```

这段客户端订阅器属于已经存在的 `example` Forge 模组；完整独立项目见 [`examples/framework-addon`](../examples/framework-addon)。不要在专用服务端入口加载 `SaoUi`、屏幕、`Minecraft` 或其他客户端类型。

## 领域

| 注册方法 | 公共贡献 | 使用边界 |
| --- | --- | --- |
| `menu` | `api.menu.SaoPanel` | 主按钮、动态条目、子菜单、动作与侧卡 |
| `hud` | `api.hud.HudElement` | 有序 HUD 绘制、可见性、可选布局编辑绑定 |
| `settings` | `api.settings.SettingsGroup` | 类型化设置项及贡献方自己的保存、复位操作 |
| `theme` | `api.theme.ThemeDefinition` | 颜色、字体资源和运动时长 |
| `world` | `api.world.WorldOverlay` | `AFTER_ENTITIES` 世界绘制 |
| `session` | `api.lifecycle.SessionListener` | 连接、维度更换和断开时释放旧世界资源 |

内置实现也走这些注册入口。公共接口不承诺服务端能力：装备、丢弃、队伍和技能仍须经过相应协议与服务端校验；UI 扩展不会授权本地修改真实容器。

## 运行时访问

`SaoUi.panels()`、`hudElements()`、`settingsGroups()`、`worldOverlays()` 返回冻结快照。`themes()` 返回代码主题与用户文件覆盖合并后的不可变快照，`theme()` 返回当前缓存的颜色、字体和运动令牌。

`SaoUi.openMenu()` 要求框架启用且当前玩家和世界有效。`openSettings(parent)` 保留返回目标。`notify(title, message)` 与带 `ItemStack` 图标的重载进入同一通知队列，保留 `Component` 样式；框架关闭时不排队、不播放新通知音。以上变更操作必须在客户端线程执行。

`selectTheme(id)` 修改当前主题和默认色相，不自行保存整个配置；保存由拥有这次设置会话的界面负责。未知主题是错误，不悄悄换成另一个主题。

`enabled()` 是配置和启动安全模式共同决定的有效开关。`setEnabled(false)` 保存用户开关，关闭自有屏幕、取消未保存 HUD 拖动、清除自有临时视觉状态；不替换原生屏幕、容器控制器或光标物品，也不清空服务器队伍/技能状态。HUD 和世界绘制贡献一并暂停，但会话清理回调继续运行。重新启用不会补播关闭期间的通知。

`safeMode()` 对应 JVM 参数 `-Dsaomenu.safeMode=true`。此模式不改写用户偏好，`setEnabled(true)` 会抛出异常；必须去掉启动参数并重启才能启用。F8 是可重绑定的全局恢复键，标题页和暂停页也有显式入口；按键设置正在捕获新绑定时不会触发恢复动作。

`bodyFont()`、`displayFont()` 用于自有界面，不修改 Minecraft 的全局默认字体。字体资源与 `Component` 字族边界见 [主题文档](themes.md)。

## 坐标和生命周期

- 屏幕、控件与 HUD 编辑器使用 **GUI 像素**，不是窗口物理像素。窗口坐标应先按当前 GUI 比例转换。
- 菜单侧卡和菜单条目使用菜单组的本地坐标；菜单宿主负责组变换及逆变换。不要对侧卡再手工乘 GUI scale。
- 原生 `Screen` 管理焦点、键盘、叙述和输入事件；文字字段保持原生控件，不手工实现字符缓存或吞掉输入法事件。
- `SaoScreen.buildContent(content)` 每次挂载重建控件；`layoutWidgets(content)` 在尺寸变化时移动现有控件。两者都由子类实现。卸载时调用 `disposeMount()`；再次打开同一对象仍会正确挂载。
- `SessionListener.levelChanged(previous, current)` 在实际客户端世界对象身份变化时调用；任一端可以为 `null`。清理实体引用、世界缓存和 GPU 资源，不在回调里推断玩家一定已建立。
- 世界绘制的姿态栈已有相机旋转、没有世界位置平移。绘制位置用 `worldPosition - camera.getPosition()`；不要保留帧对象，必须平衡额外 push/pop 并恢复自己更改的渲染状态。
- HUD 编辑只在菜单编辑路由中开始。拖动松手时保存，取消或世界结束回滚未保存锚点；不要在逐帧绘制中写配置。

`HudPass.WORLD` 在 SAO 菜单关闭时运行，`MENU_UNDERLAY`、`MENU_OVERLAY` 分别位于菜单内容下方和上方。`GAME_OVERLAY` 位于平台 HUD 尾部、原生 Screen 绘制之前，菜单打开时仍运行；欢迎和 Boss 横幅使用这一阶段。不要同时注册到多个阶段却假定每帧只调用一次。

`SaoScrollPane(bounds, narration)` 接收原生控件，并使用内容局部坐标保存其位置。滚动不篡改控件自身的 `visible`，Tab 可进入子控件并使焦点行可见。`SaoConfirmDialog.open(parent, title, message, action)` 使用 Forge 原生 GUI 层，取消不执行动作，父屏幕草稿不因弹层打开而卸载。

## API 文档构建

```text
gradlew :forge:apiJavadoc :forge:apiJavadocJar --console=plain
```

HTML 输出到 `forge/build/docs/api`，文档 JAR 与其他 Forge 产物一同位于 `forge/build/libs`。只对公共 API 源码生成文档，不把开发预览或内部实现当作 SDK。
