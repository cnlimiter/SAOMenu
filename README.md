# SAO Menu

Minecraft **1.20.1 / Forge 47.3.0 / Java 17** 的 SAO 风格菜单与 HUD。

当前构建包含 `common` 与 `forge` 两个模块；没有可发布的 Fabric 模块。Architectury API 随 Forge 产物内嵌。

## 使用

将 `forge/build/libs/saomenu-forge-1.0.0.jar` 放入客户端的 `mods` 目录。

- 默认 **O** 键打开悬浮圆形菜单，再按 O 层级返回。按键可在原版设置中修改。
- 菜单包含玩家、队伍、好友、设置面板；装备和物品通过动态菜单列展示。
- HUD 包括玩家状态板、圆点快捷栏、技能栏、时钟、地图、通知和 Boss 横幅。
- 世界渲染包含目标血条、菜单投影及死亡碎裂效果。
- 模组设置保留独立的 P5 风格动画背景与分类界面。
- 配置保存在 `config/saomenu.json`；用户主题从 `config/saomenu/themes/*.json` 加载。

队伍和物品操作存在服务端协议。客户端菜单可显示不等于服务器已经安装或支持这些协议。未知模组、光影组合和所有原版界面的兼容性不能由一次开发客户端预览推定。

## 构建与验证

```text
gradlew :common:test :forge:build --console=plain
gradlew :forge:runClient --console=plain
```

默认开发客户端目录为 `forge/run/client`。可用 `-Psaomenu.runDir=<目录>` 指定独立目录；相对路径以仓库根目录为基准。

自动预览使用真实客户端创建专用世界、执行交互并截图：

```text
gradlew :forge:runClient -Psaomenu.preview=D:/saomenu-verify/current --console=plain
```

指定 `saomenu.preview` 时默认改用 `forge/run/preview`，不再使用普通开发客户端目录。也可另外指定独立的 `saomenu.runDir`。**不要将预览运行目录指向个人游戏目录**：预览会重建 `saves/saomenupreview`，并修改该目录内的语言、GUI 缩放与模组配置。

预览代码位于 `common/src/dev/java`，Forge 启动钩子位于 `forge/src/dev/java`；仅预览运行选择 `dev` source set。这些类不进入发行 JAR，也不由生产按键逻辑反射加载。

预览的边界：

- 截图文件名不证明目标页面实际打开；须核对当时的 `Screen`、日志和画面。
- `dev.preview.SAOInventoryScreen` 是旧的客户端本地物品栏验证夹具，不是服务端权威的原版容器替代品。
- 集成世界预览不代替独立服务端、联机同步或第三方模组兼容性验收。
- 世界、粒子、时钟和动画不是像素确定的；不能用全图零差异作为 HUD 验收条件。

截图比较工具：

```text
python tools/verification/diff_screens.py <基线目录> <当前目录> --only settings --max-changed-ratio 0.005 --max-channel-delta 48
python -m unittest discover -s tools/verification -p test_*.py -v
```

旧物品栏夹具的整张截图仍包含背景世界与 HUD；不要将其全图差异直接视为面板回归。需要比较时，应按实际面板边界单独测量，而不是放宽阈值直至通过。

## 源码职责

`common/src/main/java/com/sao/saomenu/`：

| 包 | 职责 |
| --- | --- |
| `client/menu` | 菜单布局、面板、条目、会话上下文和菜单屏幕 |
| `client/screen` | 独立页面；`settings` 保存设置页面及选项描述 |
| `client/hud` | 屏幕空间 HUD 与浮动组件 |
| `client/render` | 世界空间渲染；`target` 保存目标血条 |
| `client/input` | 按键、菜单移动、自由视角 |
| `client/effect` | 欢迎、死亡碎裂及粒子实现 |
| `client/skill`、`client/party` | 客户端技能呈现和队伍状态 |
| `config` | 配置数据与持久化 |
| `network` | 协议注册及 `c2s`、`s2c` 消息 |
| `server` | 队伍、物品操作、技能冷却等服务端规则 |
| `ui/render`、`ui/text`、`ui/animation`、`ui/theme` | 共用绘制、文字、动画、主题工具 |

`forge` 保存加载器入口、注册、客户端事件适配与按职责分类的 Mixin。`SAOMenuPlatform` 与 `forge.SAOMenuPlatformImpl` 保持 Architectury 平台桥命名约定。测试目录跟随被测类的包。

`common/src/dev/java/com/sao/saomenu/dev/preview` 保存开发预览与历史交互夹具，不作为开发者 API。

包分类本身不代表客户端/服务端依赖已经完全解耦，也不将内部类自动升级为稳定公共 API。

## 资源工具

需要 Python 与 Pillow；视频转换另需 ffmpeg。

```text
python tools/assets/gen_textures.py
python tools/assets/gen_particles.py
python tools/assets/gen_settings_bg.py <视频路径>
```

这些命令会重写 `common/src/main/resources` 下的对应生成资源，不是构建的必要步骤。

素材来源：`SAO_Utils_2.2` 主题包的音效，以及 `SAO Utils Icon Set` 的欢迎动画素材；菜单视觉参考 SAO Utils 的圆形按钮、白色浮动卡片与橙色选中态。参考项目的代码与资源不会因视觉借鉴自动获得本项目的 MIT 授权。
