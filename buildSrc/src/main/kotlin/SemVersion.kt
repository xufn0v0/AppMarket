import org.gradle.api.Project

/**
 * 语义化版本 MAJOR.MINOR.PATCH（https://semver.org ）。
 *
 * Android versionCode 采用确定性编码 [versionCode]：MAJOR*10000 + MINOR*100 + PATCH，
 * 严格保持语义化版本的先后顺序，因此 versionCode 无需人工维护，每次构建自动从
 * [ProjectConfig.VERSION_NAME] 派生；MINOR/PATCH 限定 0..99，超出时须先递增更高位。
 */
data class SemVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemVersion> {

    init {
        require(major >= 0) { "major 必须为非负整数" }
        require(minor in 0 until COMPONENT_LIMIT) {
            "minor 必须在 0..${COMPONENT_LIMIT - 1} 之间；请先递增 major"
        }
        require(patch in 0 until COMPONENT_LIMIT) {
            "patch 必须在 0..${COMPONENT_LIMIT - 1} 之间；请先递增 minor"
        }
    }

    /** 由语义化版本派生的 Android versionCode，随版本号自动递增，保持升级顺序。 */
    val versionCode: Int = major * MAJOR_FACTOR + minor * COMPONENT_LIMIT + patch

    /** 按预设规则递增：MAJOR/MINOR 递增时低位归零。 */
    fun bump(level: BumpLevel): SemVersion = when (level) {
        BumpLevel.MAJOR -> copy(major = major + 1, minor = 0, patch = 0)
        BumpLevel.MINOR -> copy(minor = minor + 1, patch = 0)
        BumpLevel.PATCH -> copy(patch = patch + 1)
    }

    override fun compareTo(other: SemVersion): Int {
        major.compareTo(other.major).let { if (it != 0) return it }
        minor.compareTo(other.minor).let { if (it != 0) return it }
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        const val COMPONENT_LIMIT = 100
        const val MAJOR_FACTOR = 10_000

        private val PATTERN = Regex("""^(\d+)\.(\d+)\.(\d+)$""")

        /** 解析 MAJOR.MINOR.PATCH；格式非法时抛出明确异常。 */
        fun parse(text: String): SemVersion {
            val match = PATTERN.matchEntire(text.trim())
                ?: throw IllegalArgumentException("非法语义化版本（应为 MAJOR.MINOR.PATCH）：\"$text\"")
            val (major, minor, patch) = match.destructured
            return SemVersion(major.toInt(), minor.toInt(), patch.toInt())
        }
    }
}

/** 版本递增级别。 */
enum class BumpLevel { MAJOR, MINOR, PATCH }

/** 应用当前语义化版本；[ProjectConfig.VERSION_NAME] 是版本号的唯一事实来源。 */
val Project.appVersion: SemVersion
    get() = SemVersion.parse(ProjectConfig.VERSION_NAME)
