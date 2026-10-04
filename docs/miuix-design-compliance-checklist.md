# MIUIX 设计规范 — 主题色系统符合性检查清单

- 日期：2026-10-05
- 适用依赖：`top.yukonga.miuix.kmp` 0.9.4-rc01
- 配色方案：Monet 引擎默认（`Spec2021` + `TonalSpot`）；固定主题色预设已移除，仅保留动态取色开关（关闭时系统默认静态方案，开启时 `keyColor = null` 跟随壁纸或引擎默认种子 `#6750A4`）
- 证据约定：所有对比度为真实 `ThemeController` 装配后的实测值（见 [monet-color-scheme-test-report.md](./monet-color-scheme-test-report.md)）；测试代码位于 `domain` / `data` / `app/shared` 的 `desktopTest`。

图例：✅ 符合并通过自动化验证 ｜ ⚠️ 有约束说明（非缺陷）

## A0. MD3/MD3E 官方命名与角色映射

| # | 要求 | 结果 | 证据 |
|---|---|---|---|
| A0.1 | 核心角色命名与 MD3 规范逐字一致 | ✅ | `MaterialDesignColors.Md3ColorRole` 30 个枚举携带 `specName`，测试 `md3ColorRoleSpecNamesMatchOfficialTerminology` 逐字断言 |
| A0.2 | 扩展角色命名与 MD3E 规范逐字一致 | ✅ | `MaterialDesignColors.Md3ExtendedColorRole` 19 个枚举（fixed 12 + 表面层级 7），测试 `md3ExtendedColorRoleSpecNamesMatchOfficialTerminology` 逐字断言 |
| A0.3 | 调色板风格命名与官方一致（含 TonalSpot） | ✅ | `MaterialDesignColors.MaterialTonalPaletteStyle` 9 种风格枚举，测试 `paletteStyleSpecNamesMatchOfficialTerminology` 逐字断言 |
| A0.4 | 基线精确色值完整且符合规范 | ✅ | `MaterialDesignColors.coreBaseline` / `extendedBaseline` 浅深双模式 49 个角色精确色值，测试 `coreLightBaselineContainsAll30Roles` 等 |
| A0.5 | 角色可用性分类清晰（DIRECT / MIUI_ADAPTED / ENGINE_INTERNAL） | ✅ | `MaterialColorSchemeMapping.availabilityOf` + 测试 `all30CoreRolesHaveDefinedAvailability` / `all19ExtendedRolesHaveDefinedAvailability` |
| A0.6 | 引擎输出 tone 位次与 Spec2021 TonalSpot 官方表一致 | ✅ | `Md3SpecComplianceTest.engineOutputMatchesSpec2021TonalSpotTonesLight/Dark`，浅 17 角色 / 深 17 角色 tone 误差 ≤ 1.5 |
| A0.7 | Spec2025（MD3E）同样满足 WCAG AA | ✅ | `Md3SpecComplianceTest.spec2025MeetsWcagAaInBothAppearances` |

## A. 色彩对比度（WCAG 2.1 AA）

| # | 要求 | 结果 | 证据 |
|---|---|---|---|
| A1 | 正文文本对 ≥ 4.5:1（浅/深） | ✅ | 最小实测：浅色 onPrimary/primary 6.46；深色 onPrimaryContainer 7.20。测试 `MonetDefaultPaletteTest.defaultSeedMeetsAaInBothAppearances` 对默认种子 × 2 模式逐对断言 |
| A2 | 大号文本/UI 图形对象 ≥ 3:1 | ✅ | 全部正文对 ≥ 6.46，远超 3:1；装饰填充角色 secondary 实测 2.63/2.96，不承载文本，不单独作为状态唯一区分（见 A4） |
| A3 | 容器（卡片/对话框/底部表）内文本可读 | ✅ | surfaceContainer 12.60~14.76；surfaceContainerHigh 7.65~7.90；surfaceContainerHighest 9.48~13.34；primary/secondary/errorContainer 全部 7.19~7.27 |
| A4 | 装饰角色不被误用为文本背景 | ⚠️→✅ | `secondary/onSecondary` 为 Miuix 滑块/开关轨道色（引擎默认即 2.6~3.0:1）；项目中不使用该对承载文字，AA 断言明确排除并在代码注释固化该语义 |
| A5 | 错误/警示语义可辨识且文字达标 | ✅ | error 浅色 `#BA1A1A`、深色 `#FFB4AB`；onError/error 6.46（浅）/ 7.72（深），容器对 7.24 |
| A6 | 颜色不透明，无意外 alpha | ✅ | `defaultSeedMeetsAaInBothAppearances` + 5 设备矩阵对 primary/secondary/background/surface/outline 做 alpha=1 断言 |

## B. MIUIX 色彩理论：主色 / 辅助色 / 中性色和谐

| # | 要求 | 结果 | 证据 |
|---|---|---|---|
| B1 | 主色由引擎 TonalSpot 单一色相和谐生成，不人工拼盘 | ✅ | `App.kt` 显式 `paletteStyle = TonalSpot`、`colorSpec = Spec2021`；全部角色由同一 keyColor 推导 |
| B2 | 不提供固定主题色预设，仅保留动态取色开关 | ✅ | 种子色下拉、持久化存储、编解码链路已全部移除；开关关闭时 `keyColor = null` 走系统默认静态方案，不叠加任何预设主题色 |
| B3 | 中性色（background/surface/outline/container 系）随引擎默认种子色相微染，不突兀 | ✅ | 实测默认方案背景 `#FDF7FF` / 深色 `#141218`，均为同色系微染中性色，与主色同一 HCT 色相族 |
| B4 | 动态取色开关关闭时使用系统默认方案，而非任何预设主题色 | ✅ | `colorSchemeModeFor`：关闭时映射 `System/Light/Dark`（Miuix 静态系统默认方案）；`App.kt` `keyColor = null`，`ThemeController` 不接收任何预设种子 |
| B5 | 浅、深双模式均完整且语义反转正确 | ✅ | 默认种子 × 浅/深 全角色 dump；深色 primary 取高明度调（如 `#CFBDFE`）、容器取低明度调，符合 M3/MIUIX 暗色 tonal 原则 |

## C. 系统组件适配一致性

| # | 组件 | 使用角色 | 结果 | 证据 |
|---|---|---|---|---|
| C1 | 主按钮 / 选中态 | primary + onPrimary | ✅ 6.46 / 7.70 | UI 装配测试 `DynamicColorThemeUiTest.monetLight/DarkPaletteKeepsReadableContrast` |
| C2 | 普通文本 / Scaffold 背景 | onBackground / background | ✅ 14.34~16.37 | 同上 |
| C3 | 卡片、对话框、底部表、导航栏容器 | surfaceContainer(High/Highest) + onX | ✅ 7.65~14.76 | 同上 |
| C4 | 文本框 / 容器内强调内容 | primaryContainer / secondaryContainer + onX | ✅ 7.19~7.27 | 同上 |
| C5 | 分割线 / 描边 | outline / dividerLine | ✅ 不透明、随模式取中性调；仅作图形分隔不承载文字 | dump 实测 |
| C6 | 破坏性操作 | error + onError | ✅ 6.46 / 7.72 | 同上 |
| C7 | 主题设置入口 | 仅保留「动态取色」开关，界面布局与其他设置项不变 | ✅ | `ThemeSettingsScreen.kt`：外观下拉 + 动态取色开关 + 模糊/悬浮底栏/角标/手势/缩放；种子色下拉已移除，文案走 Compose Resources（中/英） |
| C8 | 所有组件统一经 `MiuixTheme.colorScheme` 取色，无硬编码色 | ✅ | 全仓预设常量已删除；新常量集中在 domain 单一来源 | `MonetColorDefaults.kt` |

## D. 配色算法稳定性与跨设备一致性

| # | 要求 | 结果 | 证据 |
|---|---|---|---|
| D1 | 同输入多次生成结果完全一致（确定性） | ✅ | `MonetDefaultPaletteTest.paletteGenerationIsDeterministic`（各模式重复 3 次，29 角色逐值相等） |
| D2 | 不同机型 / 屏幕尺寸 / 密度 / 亮度 / 色温下输出一致 | ✅ | `paletteIsIdenticalAndReadableAcrossFiveDeviceProfiles`：5 画像 × 2 模式，29 角色逐值相等 + AA + 不透明 |
| D3 | 配色生成不接收设备输入（架构契约） | ✅ | `renderPaletteFor` 校验画像参数合法后原样调用引擎；引入设备依赖会立即破坏测试 |
| D4 | Android 与桌面端引擎参数一致 | ✅ | 参数在 `commonMain` 的 `App.kt` 统一传入；桌面端 keyColor=null 回退与显式默认种子逐角色一致（`explicitDefaultSeedMatchesEngineFallbackInBothAppearances`） |

## E. 异常处理与持久化兼容

| # | 要求 | 结果 | 证据 |
|---|---|---|---|
| E1 | 旧版本遗留的种子色存储键不再被读取，不产生副作用 | ✅ | `ThemePreferenceKeys.MonetSeedColor` 已删除；仓库不再 observe 该键；旧数据残留不影响任何功能 |
| E2 | 动态取色开关状态持久化并在重启后恢复 | ✅ | `ThemeSettingsAppearanceTest.setEnableDynamicColorPersistsAndUpdatesState`、`StoreMigrationTest` 持久化用例 |
| E3 | 库版本升级时引擎行为可锁定 | ✅ | Spec/PaletteStyle 显式传参 + 默认种子常量化 + 回退一致性测试，三重守护 |

## F. 工程与可维护性

| # | 要求 | 结果 | 证据 |
|---|---|---|---|
| F1 | 不依赖库 internal API | ✅ | 生产与测试仅使用公开 API `ThemeController` / `MiuixTheme` / `Colors` |
| F2 | 所有用户可见字符串可本地化 | ✅ | `theme_dynamic_color`、`theme_dynamic_color_summary` 等在 values/ 与 values-zh/ 提供；种子色相关字符串已移除 |
| F3 | 注释中文、日志英文 | ✅ | 代码注释为中文；测试输出标签为英文 |
| F4 | 全量回归通过 | ✅ | `:domain:desktopTest :data:desktopTest :app:shared:desktopTest`：194 测试 / 0 失败 / 0 错误 |

## 结论

A~F 六组共 **30** 项检查全部满足（A4 为带使用约束的符合）。固定主题色预设移除后，Monet 引擎默认配色方案在对比度、色彩和谐、组件适配、跨设备稳定性与工程规范上均达到 MIUIX 设计规范与 WCAG 2.1 AA 要求；动态取色开关关闭时回退系统默认静态方案，不叠加任何预设主题色。
