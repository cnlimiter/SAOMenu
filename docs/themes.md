# 主题与资源

## 注册主题

在客户端 `SaoUiRegisterEvent` 中注册 `ThemeDefinition`。身份必须是贡献方自己的 `ResourceLocation`，例如 `example:winter`。代码主题与菜单、HUD、设置使用相同的排序、重复 ID 检查和冻结时机。

`ThemeTokens` 包含：

| 令牌 | 作用 |
| --- | --- |
| `colors` | 按语义区分正文、强调、分隔、底面与投影的 ARGB 调色板 |
| `bodyFont` | 自有菜单、HUD、按钮、字段、对话框正文使用的默认字体资源 |
| `displayFont` | 自有卡片屏幕标题使用的默认字体资源 |
| `enterMillis`、`exitMillis` | 菜单开合时长；公共卡片的入场也读取 `enterMillis` |

`SaoUi.bodyFont()`、`displayFont()` 提供稳定的 `Font` 句柄，按当前主题及资源重载后的字体集合解析字形。原生文本字段使用这个句柄时仍保留 Minecraft 的输入、选择和光标实现；自行传入其他 `Font` 的字段保持调用方选择。P5 设置页是独立艺术风格，不把其视频节拍、蓝粉配色和运动时序强行换成 SAO 卡片参数。

`SaoUi.theme()` 返回当前缓存令牌。切换主题会改变调色板身份、字体和时长；之后单独调整色相只改变强调色，不丢失当前调色板身份。`SaoUi.selectTheme(id)` 是客户端线程上的内存选择，不负责保存整个核心配置。

## 用户 JSON

目录是游戏实例的 `config/saomenu/themes/`。启动时按文件路径排序读取 `.json`，后读取的同 ID 文件覆盖先前定义；坏文件记录原因并单独跳过。修改文件后重新启动客户端。

```json
{
  "id": "example:winter",
  "name": "Winter",
  "order": 500,
  "defaultHue": 202,
  "bodyFont": "minecraft:default",
  "displayFont": "minecraft:default",
  "enterMillis": 260,
  "exitMillis": 170,
  "colors": {
    "textOnSurface": "#24313D",
    "textMuted": "#7A8B98",
    "divider": "#93AABD",
    "dialogSurface": "#E6F5FAFF",
    "dialogShadow": "#6E18232D"
  }
}
```

- 只有 `id` 必填。缺省字段继承同 ID 已注册主题；没有同 ID 时继承 SAO 基础令牌。
- `nameKey` 可提供翻译键，优先于字面 `name`；语言内容由模组或资源包提供。
- `order` 是 32 位整数。省略时继承同 ID 的次序，新 ID 使用当前最大次序加 100（上限 `2147483647`）。合并结果始终按 `order`、完整 ID 排序；文件读取顺序只决定同 ID 覆盖的先后。
- `defaultHue` 是有限数，按 360 度循环。强调色 `accent` 始终由当前色相生成；JSON 中的 `colors.accent` 被忽略并记录警告。
- 颜色只接受 `#RRGGBB`、`#AARRGGBB` 或 `-2147483648..4294967295` 范围内的整数；八位字符串的前两位是透明度。短十六进制、无 `#` 字符串、小数和溢出数字会使该文件被跳过，不部分覆盖既有主题。
- 可覆盖的其他颜色角色为 `textOnAccent`、`highlight`、`shadow`、`surfaceSlot`。
- 运动时长是非负 32 位整数，不截断小数或溢出数。字体 ID 只验证资源名语法；真实字体必须随资源包或附属模组提供，缺失资源仍由 Minecraft 的资源诊断报告。
- 旧文件里的短 ID，例如 `qinglan`，只在输入加载时迁移为 `saomenu:qinglan`。新文件和代码使用完整 ID，运行时查找不保留短名别名。

## 字体、图标与所有权

资源必须留在自己的命名空间。例子：

```text
assets/example/font/body.json
assets/example/textures/gui/notebook.png
assets/example/lang/zh_cn.json
```

字体资源 `example:body` 对应 `font/body.json`，不是文件系统路径。独立示例的字体是一个明确引用 `minecraft:default` 的自有资源入口，不声称它提供了另一套字形。

内置主题默认字体为 `saomenu:body`，不再覆盖 `assets/minecraft/font/default.json`。关闭框架后，这些句柄回退到原版默认字体；其他模组的默认字体不被修改。显式的非默认 `Component` 字体仍保留。Minecraft 的解析器不能区分“未指定字体”和显式指定 `minecraft:default`，二者在主题句柄内都会使用当前默认角色；需要保持独立字族时使用自己的非默认字体 ID。

这些令牌作用于框架自有绘制与明确列入适配范围的原版界面，不是任意第三方屏幕的全局换肤协议。附属界面应使用 `SaoScreen`、公共控件或明确的绘制回调；未知屏幕和已知屏幕的第三方子类默认保持原样，见 [适配边界](api.md#第三方界面边界)。

`MenuIcon` 与 `MenuEntry.icon` 使用完整纹理资源，如 `example:textures/gui/notebook.png`。菜单宿主缩放图标；附属模组不应覆盖核心同名资源来实现自己的入口。

世界绘制回调借用姿态栈和相机，不取得它们的所有权。自己创建的纹理、缓存和实体引用应在相应生命周期中释放；不能仅依赖 `Screen.removed()` 清理跨世界对象。
