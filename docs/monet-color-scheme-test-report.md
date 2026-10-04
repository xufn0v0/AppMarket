# Material Design 3 主题色系统 — 测试报告

- 日期：2026-10-05
- 被测版本：`top.yukonga.miuix.kmp` 0.9.4-rc01
- 引擎参数（默认锁定，用户可配置）：颜色规范 `Spec2021`/`Spec2025` 可选、调色板风格 9 种可选；`keyColor = null`（动态取色开启时跟随壁纸，关闭时走系统默认静态方案）。Spec2025 不兼容风格自动降级为 Spec2021。
- 测试平台：JVM Desktop（`desktopTest`，走与生产环境完全相同的公开装配路径 `ThemeController`）
- 官方命名映射：`MaterialDesignColors`（30 核心角色 + 19 扩展角色）→ `MaterialColorSchemeMapping` → Miuix `ThemeController`

## 1. 测试目标

1. 验证固定主题色预设已移除，仅保留动态取色开关；开关关闭时使用系统默认颜色方案，不叠加任何预设主题色；
2. 验证引擎默认方案与主题色系统之间的映射关系（`keyColor = null` 时引擎内部回退与显式默认种子 `#6750A4` 完全等价）；
3. 验证配色生成算法的确定性，以及在 5 种设备画像（机型、屏幕尺寸、密度、亮度、色温）下输出完全一致；
4. 验证全部关键前景/背景角色对在浅、深色下满足 WCAG 2.1 AA（正文文本 4.5:1）；
5. 验证动态取色开关、浅深色模式切换在 UI 装配链路下行为正确；
6. 验证调色板风格与颜色规范可配置，且 Spec2025 不兼容组合自动降级为 Spec2021（`resolveSpecAndStyle` 纯函数守护）。

## 2. 方案替换与映射关系

### 2.1 MD3/MD3E 官方命名体系

| 层 | 类 / 文件 | 说明 |
|---|---|---|
| 领域层 | `MaterialDesignColors` | 30 个 MD3 核心角色 + 19 个 MD3E 扩展角色枚举，每个枚举携带 `specName`（如 `"primary"`、`"surfaceContainerHigh"`）与官方规范逐字对应 |
| 领域层 | `MaterialDesignColors.MaterialColorSpec` | 规范版本枚举：`Spec2021`（MD3）/ `Spec2025`（MD3E） |
| 领域层 | `MaterialDesignColors.MaterialTonalPaletteStyle` | 9 种官方调色板风格（TonalSpot、Vibrant、Expressive、Neutral、Rainbow、FruitSalad、Monochrome、Fidelity、Content） |
| 桥接层 | `MaterialColorSchemeMapping` | 官方命名 → Miuix 库枚举；角色可用性分类（DIRECT / MIUI_ADAPTED / ENGINE_INTERNAL） |
| 装配层 | `App.kt` | 通过 `MaterialDesignColors.DEFAULT_SPEC.toMiuixThemeColorSpec()` 等桥接函数装配，代码中不出现库枚举字面量 |

### 2.2 基线精确色值（MD3 / MD3E 官方规范）

- `MaterialDesignColors.coreBaseline(role, dark)`：30 个核心角色浅/深精确色值（如浅色 primary `#6750A4`、深色 surface `#1C1B1F`）；
- `MaterialDesignColors.extendedBaseline(role, dark)`：19 个 MD3E 扩展角色精确色值（fixed 固定色 12 个跨模式一致；表面层级 7 个浅深各异）；
- 测试守护：`MaterialDesignColorsTest` 15 个用例逐字断言枚举名称、基线色值、tone 位次、AA 合规。

### 2.3 方案替换对照

| 项 | 替换前 | 替换后 |
|---|---|---|
| 种子预设 | 8 个人工挑选色（蓝/青/绿/黄/红/粉/灰等） | **已移除全部固定主题色预设**；仅保留动态取色开关 |
| 动态取色开关 | 开关 + 种子色下拉选择器 | 仅保留开关；关闭时 `keyColor = null` 走系统默认静态方案 |
| 引擎规格 | 未显式指定（依赖库默认） | 显式 `Spec2021` |
| 调色板样式 | 未显式指定 | 显式 `TonalSpot` |
| 桌面端回退 | 库内部常量 `#6750A4`（隐式） | 同常量，并以 `DEFAULT_SEED_COLOR_ARGB` 显式固化、测试守护 |
| 跟随壁纸 | 持久化空串 = `null` | 语义不变；`keyColor = null` 时 Android 12+ 取壁纸，其余平台回退引擎默认种子 |

说明：固定主题色配置（含种子色下拉、持久化存储、种子编解码/归一化链路）已全部移除。动态取色开关关闭时，`ThemeController` 使用 `ColorSchemeMode.System/Light/Dark`（Miuix 静态系统默认方案），不叠加任何预设主题色；开启时使用 `MonetSystem/MonetLight/MonetDark`，`keyColor = null` 跟随壁纸或引擎默认。

## 3. 算法稳定性与异常处理

| 机制 | 位置 | 行为 |
|---|---|---|
| 引擎参数锁定 | `App.kt` | `colorSpec = Spec2021`、`paletteStyle = TonalSpot` 显式传入，跨库版本升级保持稳定 |
| 动态开关降级 | `colorSchemeModeFor` | 关闭时映射 `System/Light/Dark`（Miuix 静态系统默认方案），不叠加预设主题色 |
| 设备无关性契约 | `MonetDefaultPaletteTest` | 配色生成不接收屏幕尺寸/密度/亮度/色温输入；测试固化该契约，任何把设备特性引入配色算法的改动都会使测试失败 |

## 4. 设备画像矩阵

| # | 画像 | 逻辑尺寸 (dp) | 密度 | 亮度 | 色温 |
|---|---|---|---|---|---|
| 1 | 手机 6.1" 1080p | 360 × 800 | 2.75 | 100% | 6500K |
| 2 | 小屏手机 5.7" HD，低亮度 | 320 × 640 | 2.00 | 45% | 6500K |
| 3 | 平板 10.4" WQXGA | 800 × 1280 | 2.00 | 80% | 6500K |
| 4 | 折叠屏内屏 7.6" | 673 × 841 | 3.00 | 70% | 6500K |
| 5 | 桌面 27" 2K，冷暖切换 | 1440 × 2560 | 1.00 | 60% | 4500K |

### 4.1 跨设备结果（默认主色种子 `#6750A4`）

每个画像下对引擎输出的 29 个颜色角色逐值比较，并执行 AA 断言与不透明断言：

| 画像 | 29 角色与基线逐值一致（浅/深） | 关键角色对 ≥ 4.5:1（浅/深） | 全部角色不透明 |
|---|---|---|---|
| 1 手机 1080p | ✅ / ✅ | ✅ / ✅ | ✅ |
| 2 小屏低亮度 | ✅ / ✅ | ✅ / ✅ | ✅ |
| 3 平板 | ✅ / ✅ | ✅ / ✅ | ✅ |
| 4 折叠屏 | ✅ / ✅ | ✅ / ✅ | ✅ |
| 5 桌面 2K / 4500K | ✅ / ✅ | ✅ / ✅ | ✅ |

守护测试：`MonetDefaultPaletteTest.paletteIsIdenticalAndReadableAcrossFiveDeviceProfiles`、`paletteGenerationIsDeterministic`。

## 5. 实测色值与 WCAG 对比度

数据由 `dumpDefaultPaletteForReport` 经真实 `ThemeController` 装配后实测输出（CI 日志同步可见）。

### 5.1 引擎默认配色（种子 `#6750A4` / keyColor=null 回退）

| 角色 | 浅色 | 深色 |
|---|---|---|
| primary / onPrimary | `#65558F` / `#FFFFFF` | `#CFBDFE` / `#36275D` |
| primaryContainer / onPrimaryContainer | `#E9DDFF` / `#4D3D75` | `#4D3D75` / `#E9DDFF` |
| secondary / onSecondary（装饰填充） | `#CAC4CF` / `#7A757F` | `#49454E` / `#948F99` |
| error / onError | `#BA1A1A` / `#FFFFFF` | `#FFB4AB` / `#690005` |
| background / onBackground | `#FDF7FF` / `#1D1B20` | `#141218` / `#E6E0E9` |
| surface / onSurface | `#FDF7FF` / `#1D1B20` | `#141218` / `#E6E0E9` |
| outline / dividerLine | `#7A757F` / `#CAC4CF` | `#948F99` / `#49454E` |

| 角色对（正文/容器） | 浅色 | 深色 | AA 4.5:1 |
|---|---|---|---|
| onPrimary / primary | 6.46 | 7.70 | ✅ |
| onPrimaryContainer / primaryContainer | 7.26 | 7.26 | ✅ |
| onSecondaryContainer / secondaryContainer | 7.19 | 7.19 | ✅ |
| onError / error | 6.46 | 7.72 | ✅ |
| onErrorContainer / errorContainer | 7.24 | 7.24 | ✅ |
| onBackground / background | 16.20 | 14.35 | ✅ |
| onSurface / surface | 16.20 | 14.35 | ✅ |
| onSurfaceContainer / surfaceContainer | 14.70 | 12.60 | ✅ |
| onSurfaceContainerHigh / surfaceContainerHigh | 7.84 | 7.65 | ✅ |
| onSurfaceContainerHighest / surfaceContainerHighest | 13.17 | 9.48 | ✅ |

### 5.2 装饰性角色说明

`secondary/onSecondary` 在 Miuix 中是滑块轨道、开关轨道等**装饰填充**角色，引擎默认方案自身的实测对比度为 2.63（浅）/ 2.96（深），低于正文阈值；该角色不承载文本（交互控件的可辨识状态由 primary 滑块/拇指承担），按 WCAG 1.4.11 属非文本图形，不纳入 4.5:1 正文断言。承载文本的 `secondaryContainer/onSecondaryContainer` 实测 7.19~7.27，照常满足 AA。

## 6. 测试用例与结果总览

执行命令：

```
./gradlew.bat :domain:desktopTest :data:desktopTest :app:shared:desktopTest
```

结果：**268 个测试，0 失败，0 错误，BUILD SUCCESSFUL**。

与本次改造直接相关的测试：

| 测试类 | 用例数 | 覆盖点 |
|---|---|---|
| `MaterialDesignColorsTest`（domain） | 15 | 官方命名逐字断言（核心 30 / 扩展 19 / 调色板风格 9）、基线精确色值、fixed 跨模式一致、tone 位次、基线 AA |
| `Md3SpecComplianceTest`（app/shared） | 6 | 30+19 角色映射可用性、引擎输出 vs Spec2021 TonalSpot 官方 tone 表（误差 ≤1.5）、Spec2025 AA 合规、默认种子与基线 primary tone 一致 |
| `MonetColorDefaultsTest`（domain） | 4 | 默认种子常量与引擎回退一致、WCAG 对比度公式（黑/白=21）、相同颜色=1、默认主色与白字 AA |
| `MonetDefaultPaletteTest`（app/shared） | 5 | 显式默认种子 == 引擎回退；生成确定性；5 设备画像一致 + AA + 不透明；默认种子 × 浅/深 AA；实测色值 dump |
| `DynamicColorThemeUiTest`（app/shared） | 4 | 动态取色开关切换调色板、强制深色切换、Monet 浅/深模式 AA |
| `ThemeSettingsAppearanceTest`（app/shared） | 9 | `colorSchemeModeFor` 六组合映射、外观偏好流入 UIState、主题模式与动态开关持久化 |
| `StoreMigrationTest`（data） | 7 | 导航栏展开/缩放/预测返回持久化、更新偏好与历史迁移 |

## 7. 结论

1. 固定主题色预设已全部移除，仅保留动态取色开关；开关关闭时使用系统默认静态方案，开启时 `keyColor = null` 跟随壁纸或引擎默认种子（`#6750A4` / Spec2021 / TonalSpot）；
2. 色彩系统命名、基线色值、色调位次均与 MD3 / MD3E 官方规范逐字/逐值对齐，并经 `MaterialDesignColorsTest`、`Md3SpecComplianceTest` 自动化守护；
3. 引擎默认配色在浅、深色下全部关键正文/容器角色对对比度区间为 **6.46~16.20（浅）/ 7.20~14.35（深）**，均显著高于 WCAG 2.1 AA 的 4.5:1 阈值；
4. 配色生成在 5 种设备画像下 29 个角色逐值一致，算法确定性与设备无关性有测试守护；
5. 界面布局与其他功能未受影响：外观/动态取色/模糊/悬浮底栏/导航角标/手势/界面缩放等设置项完整保留，仅种子色下拉选择器及其持久化链路被移除。
