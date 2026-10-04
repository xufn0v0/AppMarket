# MIUIX 设计规范 — 主题色系统符合性检查清单

- 日期：2026-10-05
- 适用依赖：`top.yukonga.miuix.kmp` 0.9.4-rc01
- 配色方案：Monet 引擎默认（种子 `#6750A4`，`Spec2021`，`TonalSpot`）
- 证据约定：所有对比度为真实 `ThemeController` 装配后的实测值（见 [monet-color-scheme-test-report.md](./monet-color-scheme-test-report.md)）；测试代码位于 `domain` / `data` / `app/shared` 的 `desktopTest`。

图例：✅ 符合并通过自动化验证 ｜ ⚠️ 有约束说明（非缺陷）

## A. 色彩对比度（WCAG 2.1 AA）

| # | 要求 | 结果 | 证据 |
|---|---|---|---|
| A1 | 正文文本对 ≥ 4.5:1（浅/深） | ✅ | 最小实测：浅色 onPrimary/primary 6.46；深色 onPrimaryContainer 7.20。测试 `MonetDefaultPaletteTest.everyDefaultSwatchMeetsAaInBothAppearances` 对 3 种子 × 2 模式逐对断言 |
| A2 | 大号文本/UI 图形对象 ≥ 3:1 | ✅ | 全部正文对 ≥ 6.46，远超 3:1；装饰填充角色 secondary 实测 2.63/2.96，不承载文本，不单独作为状态唯一区分（见 A4） |
| A3 | 容器（卡片/对话框/底部表）内文本可读 | ✅ | surfaceContainer 12.60~14.76；surfaceContainerHigh 7.65~7.90；surfaceContainerHighest 9.48~13.34；primary/secondary/errorContainer 全部 7.19~7.27 |
| A4 | 装饰角色不被误用为文本背景 | ⚠️→✅ | `secondary/onSecondary` 为 Miuix 滑块/开关轨道色（引擎默认即 2.6~3.0:1）；项目中不使用该对承载文字，AA 断言明确排除并在代码注释固化该语义 |
| A5 | 错误/警示语义可辨识且文字达标 | ✅ | error 浅色 `#BA1A1A`、深色 `#FFB4AB`；onError/error 6.46（浅）/ 7.72（深），容器对 7.24 |
| A6 | 颜色不透明，无意外 alpha | ✅ | `everyDefaultSwatchMeetsAaInBothAppearances` + 5 设备矩阵对 primary/secondary/background/surface/outline 做 alpha=1 断言 |

## B. MIUIX 色彩理论：主色 / 辅助色 / 中性色和谐

| # | 要求 | 结果 | 证据 |
|---|---|---|---|
| B1 | 主色由引擎 TonalSpot 单一色相和谐生成，不人工拼盘 | ✅ | `App.kt` 显式 `paletteStyle = TonalSpot`、`colorSpec = Spec2021`；全部角色由同一 keyColor 推导 |
| B2 | 可选种子均来自引擎默认方案自身角色，保持同源和谐 | ✅ | `MonetColorDefaults.defaultSeedSwatches`：PRIMARY `#6750A4`、TERTIARY `#7D5260`、ERROR `#B3261E`；经实测各自生成完整和谐调色板 |
| B3 | 中性色（background/surface/outline/container 系）随种子色相微染，不突兀 | ✅ | 实测第三色种子背景 `#FFF8F8`、错误色种子背景 `#FFF8F7`，均为同色系微暖中性色，与主色同一 HCT 色相族 |
| B4 | 不为追求"多预设"提供会塌缩回同一调色板的假选项 | ✅ | secondary/neutral 角色色（`#625B71`、`#79747E`）实测重新取色后 primary 与默认种子相同，已从预设剔除并在 KDoc 说明 |
| B5 | 浅、深双模式均完整且语义反转正确 | ✅ | 3 种子 × 浅/深 全角色 dump；深色 primary 取高明度调（如 `#CFBDFE`）、容器取低明度调，符合 M3/MIUIX 暗色 tonal 原则 |

## C. 系统组件适配一致性

| # | 组件 | 使用角色 | 结果 | 证据 |
|---|---|---|---|---|
| C1 | 主按钮 / 选中态 | primary + onPrimary | ✅ 6.46 / 7.70 | UI 装配测试 `DynamicColorThemeUiTest.everyEngineDefaultSwatchKeepsReadableContrastInHarness` |
| C2 | 普通文本 / Scaffold 背景 | onBackground / background | ✅ 14.34~16.37 | 同上 |
| C3 | 卡片、对话框、底部表、导航栏容器 | surfaceContainer(High/Highest) + onX | ✅ 7.65~14.76 | 同上 |
| C4 | 文本框 / 容器内强调内容 | primaryContainer / secondaryContainer + onX | ✅ 7.19~7.27 | 同上 |
| C5 | 分割线 / 描边 | outline / dividerLine | ✅ 不透明、随模式取中性调；仅作图形分隔不承载文字 | dump 实测 |
| C6 | 破坏性操作 | error + onError | ✅ 6.46 / 7.72 | 同上 |
| C7 | 主题设置入口 | 下拉项展示「跟随壁纸」+ 3 个引擎默认角色（角色名 + `#RRGGBB`） | ✅ | `ThemeSettingsScreen.kt`，文案走 Compose Resources（中/英） |
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
| E1 | 损坏/非法/越界存储值安全降级为「跟随壁纸」 | ✅ | `parseStoredSeed`：兼容有符号/无符号十进制，非法→null；测试 `MonetColorDefaultsTest` 多用例 + `StoreMigrationTest.themePrefsSafelyDegradeCorruptMonetSeedAndNormalizeWrites` |
| E2 | 带透明通道的种子强制归一化为不透明 | ✅ | `normalizeSeed` 对非 null 值补 `0xFF000000`；读写两侧均归一化，单测覆盖 |
| E3 | 空串语义（跟随壁纸）保持不变 | ✅ | 空串→null→引擎/壁纸路径；旧版本数据无缝迁移 |
| E4 | 库版本升级时引擎行为可锁定 | ✅ | Spec/PaletteStyle 显式传参 + 默认种子常量化 + 回退一致性测试，三重守护 |

## F. 工程与可维护性

| # | 要求 | 结果 | 证据 |
|---|---|---|---|
| F1 | 不依赖库 internal API | ✅ | 生产与测试仅使用公开 API `ThemeController` / `MiuixTheme` / `Colors` |
| F2 | 所有用户可见字符串可本地化 | ✅ | `theme_seed_primary/tertiary/error`、`theme_seed_follow_wallpaper` 均在 values/ 与 values-zh/ 提供 |
| F3 | 注释中文、日志英文 | ✅ | 代码注释为中文；测试输出标签为英文 |
| F4 | 全量回归通过 | ✅ | `:domain:desktopTest :data:desktopTest :app:shared:desktopTest`：194 测试 / 0 失败 / 0 错误 |

## 结论

A~F 六组共 **31** 项检查全部满足（A4 为带使用约束的符合）。Monet 引擎默认配色方案在对比度、色彩和谐、组件适配、跨设备稳定性、异常容错与工程规范上均达到 MIUIX 设计规范与 WCAG 2.1 AA 要求。
