package com.app.market.ui.theme

import androidx.compose.ui.graphics.Color
import com.app.market.domain.theme.MaterialDesignColors.MaterialColorSpec
import com.app.market.domain.theme.MaterialDesignColors.MaterialTonalPaletteStyle
import com.app.market.domain.theme.MaterialDesignColors.Md3ColorRole
import com.app.market.domain.theme.MaterialDesignColors.Md3ExtendedColorRole
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

/** 官方命名 → 库枚举（颜色规范版本）。 */
fun MaterialColorSpec.toMiuixThemeColorSpec(): ThemeColorSpec = when (this) {
    MaterialColorSpec.SPEC_2021 -> ThemeColorSpec.Spec2021
    MaterialColorSpec.SPEC_2025 -> ThemeColorSpec.Spec2025
}

/** 官方命名 → 库枚举（调色板风格，含规范默认的 TonalSpot）。 */
fun MaterialTonalPaletteStyle.toMiuixPaletteStyle(): ThemePaletteStyle = when (this) {
    MaterialTonalPaletteStyle.TONAL_SPOT -> ThemePaletteStyle.TonalSpot
    MaterialTonalPaletteStyle.NEUTRAL -> ThemePaletteStyle.Neutral
    MaterialTonalPaletteStyle.VIBRANT -> ThemePaletteStyle.Vibrant
    MaterialTonalPaletteStyle.EXPRESSIVE -> ThemePaletteStyle.Expressive
    MaterialTonalPaletteStyle.RAINBOW -> ThemePaletteStyle.Rainbow
    MaterialTonalPaletteStyle.FRUIT_SALAD -> ThemePaletteStyle.FruitSalad
    MaterialTonalPaletteStyle.MONOCHROME -> ThemePaletteStyle.Monochrome
    MaterialTonalPaletteStyle.FIDELITY -> ThemePaletteStyle.Fidelity
    MaterialTonalPaletteStyle.CONTENT -> ThemePaletteStyle.Content
}

/**
 * MD3/MD3E 官方命名体系与 Miuix 运行时实现之间的角色映射。
 *
 * [Colors.md3ColorOrNull] / [Colors.md3ExtendedColorOrNull]：把 Miuix 运行时配色
 * 映射到 MD3 核心角色 / MD3E 扩展角色，返回 null 表示该角色由引擎内部持有、
 * 运行时 [Colors] 未直接暴露（见 [availabilityOf] 的分类说明）。
 */
object MaterialColorSchemeMapping {

    /** MD3/MD3E 角色在 Miuix 运行时配色中的落地方式。 */
    enum class Md3RoleAvailability {
        /** 直接暴露：Miuix Colors 角色与 MD3 角色一一对应。 */
        DIRECT,

        /** MIUI 适配：Miuix 按 MIUI 视觉规范改造的角色（如装饰性 secondary、分割线）。 */
        MIUI_ADAPTED,

        /** 引擎内部：取色引擎已计算（MonetRoles 含 fixed 等），但运行时 Colors 未暴露。 */
        ENGINE_INTERNAL,
    }

    /**
     * MD3 核心角色 → Miuix Colors 的映射；null 表示引擎内部角色。
     *
     * 映射关系说明：
     * - secondary/onSecondary：Miuix 按 MIUI 规范用作滑块、开关轨道等装饰填充，
     *   非 MD3 文本强调色（MIUI_ADAPTED）；
     * - outlineVariant → Miuix dividerLine（MIUI 命名的同一语义角色）；
     * - onSurfaceVariant → Miuix onSurfaceVariantSummary（次级说明文字）；
     * - scrim → Miuix windowDimming（弹窗遮罩）；
     * - tertiary/onTertiary、inverse 系列、shadow：引擎内部计算，运行时未暴露。
     */
    fun Colors.md3ColorOrNull(role: Md3ColorRole): Color? = when (role) {
        Md3ColorRole.PRIMARY -> primary
        Md3ColorRole.ON_PRIMARY -> onPrimary
        Md3ColorRole.PRIMARY_CONTAINER -> primaryContainer
        Md3ColorRole.ON_PRIMARY_CONTAINER -> onPrimaryContainer
        Md3ColorRole.SECONDARY -> secondary
        Md3ColorRole.ON_SECONDARY -> onSecondary
        Md3ColorRole.SECONDARY_CONTAINER -> secondaryContainer
        Md3ColorRole.ON_SECONDARY_CONTAINER -> onSecondaryContainer
        Md3ColorRole.TERTIARY -> null
        Md3ColorRole.ON_TERTIARY -> null
        Md3ColorRole.TERTIARY_CONTAINER -> tertiaryContainer
        Md3ColorRole.ON_TERTIARY_CONTAINER -> onTertiaryContainer
        Md3ColorRole.ERROR -> error
        Md3ColorRole.ON_ERROR -> onError
        Md3ColorRole.ERROR_CONTAINER -> errorContainer
        Md3ColorRole.ON_ERROR_CONTAINER -> onErrorContainer
        Md3ColorRole.BACKGROUND -> background
        Md3ColorRole.ON_BACKGROUND -> onBackground
        Md3ColorRole.SURFACE -> surface
        Md3ColorRole.ON_SURFACE -> onSurface
        Md3ColorRole.SURFACE_VARIANT -> surfaceVariant
        Md3ColorRole.ON_SURFACE_VARIANT -> onSurfaceVariantSummary
        Md3ColorRole.OUTLINE -> outline
        Md3ColorRole.OUTLINE_VARIANT -> dividerLine
        Md3ColorRole.INVERSE_SURFACE -> null
        Md3ColorRole.INVERSE_ON_SURFACE -> null
        Md3ColorRole.INVERSE_PRIMARY -> null
        Md3ColorRole.SURFACE_TINT -> primary
        Md3ColorRole.SCRIM -> windowDimming
        Md3ColorRole.SHADOW -> null
    }

    /**
     * MD3E 扩展角色 → Miuix Colors 的映射；null 表示引擎内部角色。
     * 三级表面容器直接暴露；fixed 固定色与 dim/bright/lowest/low 由引擎内部持有。
     */
    fun Colors.md3ExtendedColorOrNull(role: Md3ExtendedColorRole): Color? = when (role) {
        Md3ExtendedColorRole.SURFACE_CONTAINER -> surfaceContainer
        Md3ExtendedColorRole.SURFACE_CONTAINER_HIGH -> surfaceContainerHigh
        Md3ExtendedColorRole.SURFACE_CONTAINER_HIGHEST -> surfaceContainerHighest
        else -> null
    }

    /** 查询 MD3 核心角色在 Miuix 运行时配色中的落地方式。 */
    fun availabilityOf(role: Md3ColorRole): Md3RoleAvailability = when (role) {
        Md3ColorRole.SECONDARY,
        Md3ColorRole.ON_SECONDARY,
        Md3ColorRole.ON_SURFACE_VARIANT,
        Md3ColorRole.OUTLINE_VARIANT,
        Md3ColorRole.SCRIM,
        -> Md3RoleAvailability.MIUI_ADAPTED

        Md3ColorRole.TERTIARY,
        Md3ColorRole.ON_TERTIARY,
        Md3ColorRole.INVERSE_SURFACE,
        Md3ColorRole.INVERSE_ON_SURFACE,
        Md3ColorRole.INVERSE_PRIMARY,
        Md3ColorRole.SHADOW,
        -> Md3RoleAvailability.ENGINE_INTERNAL

        else -> Md3RoleAvailability.DIRECT
    }

    /** 查询 MD3E 扩展角色在 Miuix 运行时配色中的落地方式。 */
    fun availabilityOf(role: Md3ExtendedColorRole): Md3RoleAvailability = when (role) {
        Md3ExtendedColorRole.SURFACE_CONTAINER,
        Md3ExtendedColorRole.SURFACE_CONTAINER_HIGH,
        Md3ExtendedColorRole.SURFACE_CONTAINER_HIGHEST,
        -> Md3RoleAvailability.DIRECT

        else -> Md3RoleAvailability.ENGINE_INTERNAL
    }
}
