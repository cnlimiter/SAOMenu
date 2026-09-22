# 附属模组快速接入

要求 Java 17、Minecraft 1.20.1、Forge 47.3.0。仓库根项目只有 `common` 和 `forge`，不要假设存在 Fabric 发布包。

## 构建核心与独立示例

在仓库根目录执行：

```text
gradlew :common:test :forge:build --console=plain
gradlew -p examples/framework-addon build --console=plain
```

示例有自己的 `settings.gradle`、Gradle 配置、模组元数据与源码，不是核心项目的子模块。它只依赖生成的 `forge/build/libs/saomenu-forge-1.0.0.jar`，没有 `project(:common)`、源码目录或内部类捷径。

使用另一个位置的发行 JAR：

```text
gradlew -p examples/framework-addon build -PsaomenuJar=D:/mods/saomenu-forge-1.0.0.jar --console=plain
```

产物为 `examples/framework-addon/build/libs/saomenu-showcase-1.0.0.jar`。示例使用自己的 `saomenu_showcase` 命名空间和 `saomenu_showcase-client.toml`，不会把第三方配置塞进核心 `saomenu.json`。

## 开发运行

单独运行示例项目：

```text
gradlew -p examples/framework-addon runClient --console=plain
```

或将独立示例 JAR 加到核心项目的隔离预览：

```text
gradlew :forge:runClient -Psaomenu.addonJar=examples/framework-addon/build/libs/saomenu-showcase-1.0.0.jar -Psaomenu.runDir=D:/saomenu-verify/addon-client -Psaomenu.preview=D:/saomenu-verify/addon-captures -Psaomenu.preview.api=true -Psaomenu.preview.keepOpen=true --console=plain
```

预览会创建/重建自己的 `saves/saomenupreview`，更改隔离目录中的语言、GUI 缩放和演示配置。**不可把预览指向个人游戏目录**。示例运行目录默认位于其自己的 `run/client`，与核心日常开发目录分离。

`saomenu.preview.api=true` 要求加载示例 JAR。它额外注册仅开发环境存在的长列表夹具，验证 13 个菜单面板、40 行动态条目、13 个设置分类及溢出主题选择，并验证原生字段、滚动焦点、弹层和 HUD 拖动。日志须出现 `API native checks passed`；截图位于指定输出目录。它不验证操作系统输入法组合过程，也不替代独立服务端/联机验收。

根项目加载发行附属 JAR 时，会把已经构建的核心发行 JAR 加到 `modCompileOnly`，仅供 Loom 重映射分析继承层次；运行时仍只有开发中的核心模组。缺少该层次时，附属屏幕继承的 `Screen.font` 等成员可能残留 SRG 名而在开发客户端报 `NoSuchFieldError`。因此必须先构建核心，再构建附属模组，最后运行预览。

## 接入约定

1. 把客户端事件订阅器放在独立客户端类中，用 `Dist.CLIENT` 限制发现。
2. 在 Forge 总线的 `SaoUiRegisterEvent` 内同步调用注册器，不再次排队。
3. ID、图标、字体使用自己命名空间的 `ResourceLocation`；标题使用 `Component`。
4. 设置组自己持有值、保存和复位回调。持久化格式由附属模组决定。
5. 绘制/输入回调在客户端线程执行。清理世界引用使用 `SessionListener`，清理界面实例资源使用屏幕卸载钩子。
6. 未注册的原版/其他模组屏幕不得通过全局类名匹配、反射或强制替换来接管。只依赖公共 API 和 Minecraft/Forge 类型。

具体构造器、坐标约定和可用控件见 [API](api.md) 及完整示例源码。注册失败必须修复冲突或时序，不要吞掉异常让附属模组看似接入成功。

本地文件模组依赖使用 Loom 的 `modImplementation` 重映射流程，参见 [Architectury Loom — Using libraries](https://docs.architectury.dev/loom/using_libraries)。这里不发布 Maven 包，也不把附属模组嵌入核心 JAR。
