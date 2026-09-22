# SAO Menu

Minecraft **1.20.1 / Forge 47.3.0 / Java 17** 的 SAO 风格客户端界面框架，内置菜单与 HUD。

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

默认开发客户端目录为 `forge/run/client`，专用服务端目录为 `forge/run/server`。可用 `-Psaomenu.runDir=<目录>` 指定独立目录；相对路径以仓库根目录为基准。联机验收必须使用独立目录，配置与运行步骤见 [快速开始](docs/quickstart.md#独立服务端与双客户端)。

自动预览使用真实客户端创建专用世界、执行交互并截图：

```text
gradlew :forge:runClient -Psaomenu.preview=D:/saomenu-verify/current --console=plain
```

指定 `saomenu.preview` 时默认改用 `forge/run/preview`，不再使用普通开发客户端目录。也可另外指定独立的 `saomenu.runDir`。**不要将预览运行目录指向个人游戏目录**：预览会重建 `saves/saomenupreview`，并修改该目录内的语言、GUI 缩放与模组配置。

预览代码位于 `common/src/dev/java`，Forge 启动钩子位于 `forge/src/dev/java`；仅预览运行选择 `dev` source set。这些类不进入发行 JAR，也不由生产按键逻辑反射加载。

追加 `-Psaomenu.preview.keepOpen=true` 可在脚本完成后保留客户端供原生窗口检查。默认仍自动退出。

预览的边界：

- 截图文件名不证明目标页面实际打开；须核对当时的 `Screen`、日志和画面。
- `dev.preview.SAOInventoryScreen` 是旧的客户端本地物品栏验证夹具，不是服务端权威的原版容器替代品。
- 生产菜单分别提供角色属性、原生背包、原生统计和完整原生进度图入口。旧预览直接挂载页面的结果不等同于入口验证；恢复场景另走实际菜单指针路由与原生容器协议。
- 二刀流回归检查运行在隔离世界的真实服务端玩家上，覆盖无效来源的原子性、物品守恒、冷却及保留主手；它不代替独立客户端的发包验证。
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
| `api` | 附属模组使用的客户端 UI 契约；加载器事件在 Forge 模块的 `api.forge` |
| `client/menu` | 菜单布局、面板、条目、会话上下文和菜单屏幕 |
| `client/runtime` | 客户端初始化、世界切换与断开连接时的会话清理 |
| `client/screen` | 独立页面；`settings` 保存设置页面及选项描述 |
| `client/hud` | 屏幕空间 HUD 与浮动组件 |
| `client/render` | 世界空间渲染；`target` 保存目标血条 |
| `client/input` | 按键、菜单移动、自由视角 |
| `client/effect` | 欢迎、死亡碎裂及粒子实现 |
| `client/skill`、`client/party` | 客户端技能呈现和队伍状态 |
| `config` | 配置数据与持久化 |
| `network` | 协议注册及 `c2s`、`s2c` 消息 |
| `server` | 队伍、物品操作、技能冷却等服务端规则 |
| `skill` | 客户端与服务端共用的技能标识、冷却和物品判定；不依赖客户端类 |
| `ui/render`、`ui/text`、`ui/animation`、`ui/theme` | 共用绘制、文字、动画、主题工具 |

`forge` 保存加载器入口、注册、客户端事件适配与按职责分类的 Mixin。公共平台桥只提供声音和粒子；UI 注册事件、主题字体句柄与原生弹层使用独立的 `client.runtime.SAOClientPlatform` 桥。原生进度图直接使用客户端连接中的进度控制器，不再复制进度列表。两组桥实现保持 Architectury 命名约定。测试目录跟随被测类的包。

`common/src/dev/java/com/sao/saomenu/dev/preview` 保存开发预览与历史交互夹具，不作为开发者 API。

`SAOMenuScreen` 仅作屏幕适配，菜单状态、输入、列、卡片和对话框分别持有职责；设置页的数据表与动画皮肤分离，HUD 组合与拖动会话分离，目标血条的追踪、几何和绘制分离。配置门面、数据及磁盘存储分开。世界切换清理投影、地图和实体视觉缓存；断线再清理队伍、技能、欢迎动画、自由视角和按键状态。

这些内部类尚未自动成为稳定公共 API。服务端包不再通过客户端技能注册表判定冷却，S2C 消息通过客户端启动时安装的接收器派发；本机独立专服与两个原生客户端已完成真实发包、队伍同步、跨维度和同进程重连验收。

## 附属模组开发

公共接入面是 `com.sao.saomenu.api`，不是上面的内部实现包。订阅客户端 Forge 总线的 `SaoUiRegisterEvent`，同步提交菜单、HUD、设置、主题、世界绘制与会话贡献；返回后注册表冻结。ID 使用自有命名空间的 `ResourceLocation`，文案使用 `Component`。

- [快速开始与独立示例构建](docs/quickstart.md)
- [注册、坐标、生命周期与 API 文档](docs/api.md)
- [主题文件、字体与资源](docs/themes.md)
- [旧接入方式迁移](docs/migration.md)
- [支持与验收边界](docs/support-matrix.md)

`examples/framework-addon` 是独立 Gradle 项目，只依赖产出的 Forge JAR，不加入主工程 source set；示例代码不进入 SAOMenu 发行包。已验收的原版域、独立联机路径，以及未覆盖的账户、输入法和第三方组合见支持矩阵。

当前 231 项普通测试通过。独立附属 JAR 已完成隔离客户端交互验收：13 面板/40 行重排保持选择、13 设置分类、原生输入/滚动/焦点、弹层、缩放重开与 HUD 保存；另有真实资源重载、极小视口像素检查和双客户端专服验收。具体证据及未覆盖边界见支持矩阵。API Javadoc 可用 `gradlew :forge:apiJavadocJar` 生成。

## 本轮框架路线

已分阶段完成：源码与开发夹具分类 → 菜单/HUD/设置及会话职责解耦 → 公共 API 与独立附属示例 → 可恢复的原版分域换肤 → 主题/布局边界与独立联机验收。各阶段分别保留本地提交；没有将所有原版、账户或第三方组合笼统标为兼容。

## 原版恢复

- **F8** 全局切换（可在原版按键设置中改绑）；标题页、暂停页有显式恢复按钮。
- 关闭时暂停所有框架 HUD/世界绘制贡献，清除自有临时视觉状态，并关闭自有菜单；原生屏幕、容器、光标物品、服务器队伍和技能状态保留。
- `config/saomenu.json` 的 `frameworkEnabled` 保存用户开关；外观复位不重新开启框架。
- 紧急启动参数 **`-Dsaomenu.safeMode=true`** 强制保留原版 UI，不覆盖用户配置，也不能从界面/API 重新开启。Gradle 开发运行使用 `-Psaomenu.safeMode=true`。
- 字体位于自有 `saomenu:body`，不覆盖 Minecraft 的全局默认字体。未知原版子类/模组屏幕默认不接管。
- 背包、储物容器、合成台和熔炉族通过精确原生类换肤；创造背包与复杂工作站保留内部功能绘制并装饰边框。槽位、菜单、输入与服务器协议仍由原版控制；熔炼箭头只更换透明材质，进度宽度不变。
- 聊天、书籍、讲台和告示牌保留原生输入及服务端保存；标题、选项、世界/服务器列表、加载、死亡等使用精确类适配。安全提示、未知屏幕和 Forge 启动期覆层不被强行接管。

恢复验证独立于各域换肤验收；运行方式和当前证据见 [快速开始](docs/quickstart.md) 与 [支持矩阵](docs/support-matrix.md)。

## 资源工具

需要 Python 与 Pillow；视频转换另需 ffmpeg。

```text
python tools/assets/gen_textures.py
python tools/assets/gen_particles.py
python tools/assets/gen_settings_bg.py <视频路径>
```

这些命令会重写 `common/src/main/resources` 下的对应生成资源，不是构建的必要步骤。

素材来源：`SAO_Utils_2.2` 主题包的音效，以及 `SAO Utils Icon Set` 的欢迎动画素材；菜单视觉参考 SAO Utils 的圆形按钮、白色浮动卡片与橙色选中态。参考项目的代码与资源不会因视觉借鉴自动获得本项目的 MIT 授权。
