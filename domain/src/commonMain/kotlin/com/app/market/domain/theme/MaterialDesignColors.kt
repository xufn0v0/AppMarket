package com.app.market.domain.theme

/**
 * Material Design 3（MD3，2021 规范）与 Material Design 3 Expressive（MD3E，2025 修订）
 * 色彩系统的唯一事实来源：
 *
 * - 官方命名体系：所有枚举均携带 [Md3ColorRole.specName] / [Md3ExtendedColorRole.specName] /
 *   [MaterialTonalPaletteStyle.specName]，与规范文档中的专业术语逐字对应
 *   （如 "primary"、"onPrimaryContainer"、"surfaceContainerHigh"、"TonalSpot"）；
 * - 完整色彩集：[Md3ColorRole] 覆盖 MD3 全部 30 个核心色彩角色，
 *   [Md3ExtendedColorRole] 覆盖 MD3E 全部 19 个扩展角色（fixed 固定色 12 个 + 表面容器/明暗 7 个）；
 * - 精确数值：[coreBaseline] / [extendedBaseline] 收录规范基线方案（seed #6750A4 静态基线）
 *   的官方精确十六进制色值，浅/深双模式齐全；
 * - 色调位次：[SPEC2021_TONAL_SPOT_TONES_LIGHT] / [SPEC2021_TONAL_SPOT_TONES_DARK] 收录
 *   Spec2021 + TonalSpot 动态方案的官方色调（HCT tone）分配，用于校验引擎输出的精确性。
 *
 * 本对象为纯 Kotlin 定义，不依赖 Compose；与 Miuix 运行时的桥接见
 * `com.app.market.ui.theme.MaterialColorSchemeMapping`。
 */
object MaterialDesignColors {

    /** 颜色规范版本：2021 为 MD3 初版，2025 为 MD3E（Material 3 Expressive）修订。 */
    enum class MaterialColorSpec(val specName: String) {
        SPEC_2021("Spec2021"),
        SPEC_2025("Spec2025"),
    }

    /**
     * MD3 官方动态调色板风格（material-color-utilities 定义）：
     * [TONAL_SPOT] 为规范默认风格（即官方术语 "tonal spot"），在识别度与和谐度间取平衡。
     */
    enum class MaterialTonalPaletteStyle(val specName: String) {
        TONAL_SPOT("TonalSpot"),
        NEUTRAL("Neutral"),
        VIBRANT("Vibrant"),
        EXPRESSIVE("Expressive"),
        RAINBOW("Rainbow"),
        FRUIT_SALAD("FruitSalad"),
        MONOCHROME("Monochrome"),
        FIDELITY("Fidelity"),
        CONTENT("Content"),
    }

    /** 生产环境锁定的规范版本与调色板风格（MD3 默认组合）。 */
    val DEFAULT_SPEC: MaterialColorSpec = MaterialColorSpec.SPEC_2021
    val DEFAULT_PALETTE_STYLE: MaterialTonalPaletteStyle = MaterialTonalPaletteStyle.TONAL_SPOT

    /**
     * MD3 核心色彩角色（30 个），命名与官方规范逐字对应（见 [specName]）。
     * 四大强调组（primary/secondary/tertiary/error 各 4 个）+ 中性表面组 + 轮廓 + 反色 + 工具色。
     */
    enum class Md3ColorRole(val specName: String) {
        PRIMARY("primary"),
        ON_PRIMARY("onPrimary"),
        PRIMARY_CONTAINER("primaryContainer"),
        ON_PRIMARY_CONTAINER("onPrimaryContainer"),
        SECONDARY("secondary"),
        ON_SECONDARY("onSecondary"),
        SECONDARY_CONTAINER("secondaryContainer"),
        ON_SECONDARY_CONTAINER("onSecondaryContainer"),
        TERTIARY("tertiary"),
        ON_TERTIARY("onTertiary"),
        TERTIARY_CONTAINER("tertiaryContainer"),
        ON_TERTIARY_CONTAINER("onTertiaryContainer"),
        ERROR("error"),
        ON_ERROR("onError"),
        ERROR_CONTAINER("errorContainer"),
        ON_ERROR_CONTAINER("onErrorContainer"),
        BACKGROUND("background"),
        ON_BACKGROUND("onBackground"),
        SURFACE("surface"),
        ON_SURFACE("onSurface"),
        SURFACE_VARIANT("surfaceVariant"),
        ON_SURFACE_VARIANT("onSurfaceVariant"),
        OUTLINE("outline"),
        OUTLINE_VARIANT("outlineVariant"),
        INVERSE_SURFACE("inverseSurface"),
        INVERSE_ON_SURFACE("inverseOnSurface"),
        INVERSE_PRIMARY("inversePrimary"),
        SURFACE_TINT("surfaceTint"),
        SCRIM("scrim"),
        SHADOW("shadow"),
    }

    /**
     * MD3E（2025 修订）扩展色彩角色（19 个）：
     * 三个强调组的 fixed 固定色系列（fixed / fixedDim / onFixed / onFixedVariant，共 12 个，
     * 固定色在浅深模式下保持一致，用于需要跨模式稳定强调的场景），
     * 以及表面层级系统（surfaceDim / surfaceBright / 5 级 surfaceContainer，共 7 个）。
     */
    enum class Md3ExtendedColorRole(val specName: String) {
        PRIMARY_FIXED("primaryFixed"),
        PRIMARY_FIXED_DIM("primaryFixedDim"),
        ON_PRIMARY_FIXED("onPrimaryFixed"),
        ON_PRIMARY_FIXED_VARIANT("onPrimaryFixedVariant"),
        SECONDARY_FIXED("secondaryFixed"),
        SECONDARY_FIXED_DIM("secondaryFixedDim"),
        ON_SECONDARY_FIXED("onSecondaryFixed"),
        ON_SECONDARY_FIXED_VARIANT("onSecondaryFixedVariant"),
        TERTIARY_FIXED("tertiaryFixed"),
        TERTIARY_FIXED_DIM("tertiaryFixedDim"),
        ON_TERTIARY_FIXED("onTertiaryFixed"),
        ON_TERTIARY_FIXED_VARIANT("onTertiaryFixedVariant"),
        SURFACE_DIM("surfaceDim"),
        SURFACE_BRIGHT("surfaceBright"),
        SURFACE_CONTAINER_LOWEST("surfaceContainerLowest"),
        SURFACE_CONTAINER_LOW("surfaceContainerLow"),
        SURFACE_CONTAINER("surfaceContainer"),
        SURFACE_CONTAINER_HIGH("surfaceContainerHigh"),
        SURFACE_CONTAINER_HIGHEST("surfaceContainerHighest"),
    }

    // ------------------------------------------------------------------
    // MD3 基线方案精确色值（官方规范 baseline schemes，种子 #6750A4 静态基线）
    // ------------------------------------------------------------------

    private val CORE_LIGHT: Map<Md3ColorRole, Int> = mapOf(
        Md3ColorRole.PRIMARY to 0xFF6750A4.toInt(),
        Md3ColorRole.ON_PRIMARY to 0xFFFFFFFF.toInt(),
        Md3ColorRole.PRIMARY_CONTAINER to 0xFFEADDFF.toInt(),
        Md3ColorRole.ON_PRIMARY_CONTAINER to 0xFF21005D.toInt(),
        Md3ColorRole.SECONDARY to 0xFF625B71.toInt(),
        Md3ColorRole.ON_SECONDARY to 0xFFFFFFFF.toInt(),
        Md3ColorRole.SECONDARY_CONTAINER to 0xFFE8DEF8.toInt(),
        Md3ColorRole.ON_SECONDARY_CONTAINER to 0xFF1D192B.toInt(),
        Md3ColorRole.TERTIARY to 0xFF7D5260.toInt(),
        Md3ColorRole.ON_TERTIARY to 0xFFFFFFFF.toInt(),
        Md3ColorRole.TERTIARY_CONTAINER to 0xFFFFD8E4.toInt(),
        Md3ColorRole.ON_TERTIARY_CONTAINER to 0xFF31111D.toInt(),
        Md3ColorRole.ERROR to 0xFFB3261E.toInt(),
        Md3ColorRole.ON_ERROR to 0xFFFFFFFF.toInt(),
        Md3ColorRole.ERROR_CONTAINER to 0xFFF9DEDC.toInt(),
        Md3ColorRole.ON_ERROR_CONTAINER to 0xFF410E0B.toInt(),
        Md3ColorRole.BACKGROUND to 0xFFFFFBFE.toInt(),
        Md3ColorRole.ON_BACKGROUND to 0xFF1C1B1F.toInt(),
        Md3ColorRole.SURFACE to 0xFFFFFBFE.toInt(),
        Md3ColorRole.ON_SURFACE to 0xFF1C1B1F.toInt(),
        Md3ColorRole.SURFACE_VARIANT to 0xFFE7E0EC.toInt(),
        Md3ColorRole.ON_SURFACE_VARIANT to 0xFF49454F.toInt(),
        Md3ColorRole.OUTLINE to 0xFF79747E.toInt(),
        Md3ColorRole.OUTLINE_VARIANT to 0xFFCAC4D0.toInt(),
        Md3ColorRole.INVERSE_SURFACE to 0xFF313033.toInt(),
        Md3ColorRole.INVERSE_ON_SURFACE to 0xFFF4EFF4.toInt(),
        Md3ColorRole.INVERSE_PRIMARY to 0xFFD0BCFF.toInt(),
        Md3ColorRole.SURFACE_TINT to 0xFF6750A4.toInt(),
        Md3ColorRole.SCRIM to 0xFF000000.toInt(),
        Md3ColorRole.SHADOW to 0xFF000000.toInt(),
    )

    private val CORE_DARK: Map<Md3ColorRole, Int> = mapOf(
        Md3ColorRole.PRIMARY to 0xFFD0BCFF.toInt(),
        Md3ColorRole.ON_PRIMARY to 0xFF381E72.toInt(),
        Md3ColorRole.PRIMARY_CONTAINER to 0xFF4F378B.toInt(),
        Md3ColorRole.ON_PRIMARY_CONTAINER to 0xFFEADDFF.toInt(),
        Md3ColorRole.SECONDARY to 0xFFCCC2DC.toInt(),
        Md3ColorRole.ON_SECONDARY to 0xFF332D41.toInt(),
        Md3ColorRole.SECONDARY_CONTAINER to 0xFF4A4458.toInt(),
        Md3ColorRole.ON_SECONDARY_CONTAINER to 0xFFE8DEF8.toInt(),
        Md3ColorRole.TERTIARY to 0xFFEFB8C8.toInt(),
        Md3ColorRole.ON_TERTIARY to 0xFF492532.toInt(),
        Md3ColorRole.TERTIARY_CONTAINER to 0xFF633B48.toInt(),
        Md3ColorRole.ON_TERTIARY_CONTAINER to 0xFFFFD8E4.toInt(),
        Md3ColorRole.ERROR to 0xFFF2B8B5.toInt(),
        Md3ColorRole.ON_ERROR to 0xFF601410.toInt(),
        Md3ColorRole.ERROR_CONTAINER to 0xFF8C1D18.toInt(),
        Md3ColorRole.ON_ERROR_CONTAINER to 0xFFF9DEDC.toInt(),
        Md3ColorRole.BACKGROUND to 0xFF1C1B1F.toInt(),
        Md3ColorRole.ON_BACKGROUND to 0xFFE6E1E5.toInt(),
        Md3ColorRole.SURFACE to 0xFF1C1B1F.toInt(),
        Md3ColorRole.ON_SURFACE to 0xFFE6E1E5.toInt(),
        Md3ColorRole.SURFACE_VARIANT to 0xFF49454F.toInt(),
        Md3ColorRole.ON_SURFACE_VARIANT to 0xFFCAC4D0.toInt(),
        Md3ColorRole.OUTLINE to 0xFF938F99.toInt(),
        Md3ColorRole.OUTLINE_VARIANT to 0xFF49454F.toInt(),
        Md3ColorRole.INVERSE_SURFACE to 0xFFE6E1E5.toInt(),
        Md3ColorRole.INVERSE_ON_SURFACE to 0xFF313033.toInt(),
        Md3ColorRole.INVERSE_PRIMARY to 0xFF6750A4.toInt(),
        Md3ColorRole.SURFACE_TINT to 0xFFD0BCFF.toInt(),
        Md3ColorRole.SCRIM to 0xFF000000.toInt(),
        Md3ColorRole.SHADOW to 0xFF000000.toInt(),
    )

    // MD3E 固定色（规范规定浅深模式一致）
    private val FIXED_ROLES: Map<Md3ExtendedColorRole, Int> = mapOf(
        Md3ExtendedColorRole.PRIMARY_FIXED to 0xFFEADDFF.toInt(),
        Md3ExtendedColorRole.PRIMARY_FIXED_DIM to 0xFFD0BCFF.toInt(),
        Md3ExtendedColorRole.ON_PRIMARY_FIXED to 0xFF21005D.toInt(),
        Md3ExtendedColorRole.ON_PRIMARY_FIXED_VARIANT to 0xFF4F378B.toInt(),
        Md3ExtendedColorRole.SECONDARY_FIXED to 0xFFE8DEF8.toInt(),
        Md3ExtendedColorRole.SECONDARY_FIXED_DIM to 0xFFCCC2DC.toInt(),
        Md3ExtendedColorRole.ON_SECONDARY_FIXED to 0xFF1D192B.toInt(),
        Md3ExtendedColorRole.ON_SECONDARY_FIXED_VARIANT to 0xFF4A4458.toInt(),
        Md3ExtendedColorRole.TERTIARY_FIXED to 0xFFFFD8E4.toInt(),
        Md3ExtendedColorRole.TERTIARY_FIXED_DIM to 0xFFEFB8C8.toInt(),
        Md3ExtendedColorRole.ON_TERTIARY_FIXED to 0xFF31111D.toInt(),
        Md3ExtendedColorRole.ON_TERTIARY_FIXED_VARIANT to 0xFF633B48.toInt(),
    )

    private val EXTENDED_LIGHT: Map<Md3ExtendedColorRole, Int> = FIXED_ROLES + mapOf(
        Md3ExtendedColorRole.SURFACE_DIM to 0xFFDED8E1.toInt(),
        Md3ExtendedColorRole.SURFACE_BRIGHT to 0xFFFFFBFE.toInt(),
        Md3ExtendedColorRole.SURFACE_CONTAINER_LOWEST to 0xFFFFFFFF.toInt(),
        Md3ExtendedColorRole.SURFACE_CONTAINER_LOW to 0xFFF7F2FA.toInt(),
        Md3ExtendedColorRole.SURFACE_CONTAINER to 0xFFF3EDF7.toInt(),
        Md3ExtendedColorRole.SURFACE_CONTAINER_HIGH to 0xFFECE6F0.toInt(),
        Md3ExtendedColorRole.SURFACE_CONTAINER_HIGHEST to 0xFFE6E0E9.toInt(),
    )

    private val EXTENDED_DARK: Map<Md3ExtendedColorRole, Int> = FIXED_ROLES + mapOf(
        Md3ExtendedColorRole.SURFACE_DIM to 0xFF141218.toInt(),
        Md3ExtendedColorRole.SURFACE_BRIGHT to 0xFF3B383E.toInt(),
        Md3ExtendedColorRole.SURFACE_CONTAINER_LOWEST to 0xFF0F0D13.toInt(),
        Md3ExtendedColorRole.SURFACE_CONTAINER_LOW to 0xFF1D1B20.toInt(),
        Md3ExtendedColorRole.SURFACE_CONTAINER to 0xFF211F26.toInt(),
        Md3ExtendedColorRole.SURFACE_CONTAINER_HIGH to 0xFF2B2930.toInt(),
        Md3ExtendedColorRole.SURFACE_CONTAINER_HIGHEST to 0xFF36343B.toInt(),
    )

    /** MD3 核心角色基线精确色值（浅/深）。 */
    fun coreBaseline(role: Md3ColorRole, dark: Boolean): Int =
        (if (dark) CORE_DARK else CORE_LIGHT).getValue(role)

    /** MD3E 扩展角色基线精确色值（浅/深；fixed 固定色两模式一致）。 */
    fun extendedBaseline(role: Md3ExtendedColorRole, dark: Boolean): Int =
        (if (dark) EXTENDED_DARK else EXTENDED_LIGHT).getValue(role)

    // ------------------------------------------------------------------
    // Spec2021 + TonalSpot 动态方案官方色调位次（material-color-utilities 定义）
    // ------------------------------------------------------------------

    /** 浅色模式官方 HCT 色调位次（仅收录规范对动态方案明确定义的角色）。 */
    val SPEC2021_TONAL_SPOT_TONES_LIGHT: Map<Md3ColorRole, Int> = mapOf(
        Md3ColorRole.PRIMARY to 40,
        Md3ColorRole.ON_PRIMARY to 100,
        Md3ColorRole.PRIMARY_CONTAINER to 90,
        Md3ColorRole.ON_PRIMARY_CONTAINER to 30,
        Md3ColorRole.SECONDARY_CONTAINER to 90,
        Md3ColorRole.ON_SECONDARY_CONTAINER to 30,
        Md3ColorRole.TERTIARY_CONTAINER to 90,
        Md3ColorRole.ON_TERTIARY_CONTAINER to 30,
        Md3ColorRole.ERROR to 40,
        Md3ColorRole.ON_ERROR to 100,
        Md3ColorRole.ERROR_CONTAINER to 90,
        Md3ColorRole.ON_ERROR_CONTAINER to 30,
        Md3ColorRole.BACKGROUND to 98,
        Md3ColorRole.ON_BACKGROUND to 10,
        Md3ColorRole.SURFACE to 98,
        Md3ColorRole.ON_SURFACE to 10,
        Md3ColorRole.OUTLINE to 50,
    )

    /** 深色模式官方 HCT 色调位次。 */
    val SPEC2021_TONAL_SPOT_TONES_DARK: Map<Md3ColorRole, Int> = mapOf(
        Md3ColorRole.PRIMARY to 80,
        Md3ColorRole.ON_PRIMARY to 20,
        Md3ColorRole.PRIMARY_CONTAINER to 30,
        Md3ColorRole.ON_PRIMARY_CONTAINER to 90,
        Md3ColorRole.SECONDARY_CONTAINER to 30,
        Md3ColorRole.ON_SECONDARY_CONTAINER to 90,
        Md3ColorRole.TERTIARY_CONTAINER to 30,
        Md3ColorRole.ON_TERTIARY_CONTAINER to 90,
        Md3ColorRole.ERROR to 80,
        Md3ColorRole.ON_ERROR to 20,
        Md3ColorRole.ERROR_CONTAINER to 30,
        Md3ColorRole.ON_ERROR_CONTAINER to 90,
        Md3ColorRole.BACKGROUND to 6,
        Md3ColorRole.ON_BACKGROUND to 90,
        Md3ColorRole.SURFACE to 6,
        Md3ColorRole.ON_SURFACE to 90,
        Md3ColorRole.OUTLINE to 60,
    )
}
