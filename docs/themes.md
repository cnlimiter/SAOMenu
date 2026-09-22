# 主题与资源

## 注册主题

在客户端 `SaoUiRegisterEvent` 中注册 `ThemeDefinition`。身份必须是贡献方自己的 `ResourceLocation`，例如 `example:winter`。代码主题与菜单、HUD、设置使用相同的排序、重复 ID 检查和冻结时机。

`ThemeTokens` 包含：

| 令牌 | 作用 |
| --- | --- |
| `colors` | 按语义区分正文、强调、分隔、底面与投影的 ARGB 调色板 |
| `bodyFont` | 公共按钮、对话框正文使用的字体资源 |
| `displayFont` | 公共卡片屏幕标题使用的字体资源 |
| `enterMillis`、`exitMillis` | 菜单开合时长；公共卡片的入场也读取 `enterMillis` |

原生文本字段继续使用传入的 Minecraft `Font`，不替换其输入、选择或光标实现。P5 设置页是独立艺术风格，不把其视频节拍、蓝粉配色和运动时序强行换成 SAO 卡片参数。

`SaoUi.theme()` 返回当前缓存令牌。切换主题会改变调色板身份、字体和时长；之后单独调整色相只改变强调色，不丢失当前调色板身份。`SaoUi.selectTheme(id)` 是客户端线程上的内存选择，不负责保存整个核心配置。

## 用户 JSON

目录是游戏实例的 `config/saomenu/themes/`。启动时按文件路径排序读取 `.json`，后读取的同 ID 文件覆盖先前定义；坏文件记录原因并单独跳过。修改文件后重新启动客户端。

```json
{
  "id": "example:winter",
  "name": "Winter",
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
- `defaultHue` 是有限数，按 360 度循环。强调色 `accent` 始终由当前色相生成；JSON 中的 `colors.accent` 被忽略并记录警告。
- 颜色接受 `#RRGGBB`、`#AARRGGBB` 或整数；八位字符串的前两位是透明度。
- 可覆盖的其他颜色角色为 `textOnAccent`、`highlight`、`shadow`、`surfaceSlot`。
- 运动时长不得为负。字体 ID 只验证资源名语法；真实字体必须随资源包或附属模组提供，缺失资源仍由 Minecraft 的资源诊断报告。
- 旧文件里的短 ID，例如 `qinglan`，只在输入加载时迁移为 `saomenu:qinglan`。新文件和代码使用完整 ID，运行时查找不保留短名别名。

## 字体、图标与所有权

资源必须留在自己的命名空间。例子：

```text
assets/example/font/body.json
assets/example/textures/gui/notebook.png
assets/example/lang/zh_cn.json
```

字体资源 `example:body` 对应 `font/body.json`，不是文件系统路径。独立示例的字体是一个明确引用 `minecraft:default` 的自有资源入口，不声称它提供了另一套字形。

`MenuIcon` 与 `MenuEntry.icon` 使用完整纹理资源，如 `example:textures/gui/notebook.png`。菜单宿主缩放图标；附属模组不应覆盖核心同名资源来实现自己的入口。

世界绘制回调借用姿态栈和相机，不取得它们的所有权。自己创建的纹理、缓存和实体引用应在相应生命周期中释放；不能仅依赖 `Screen.removed()` 清理跨世界对象。
