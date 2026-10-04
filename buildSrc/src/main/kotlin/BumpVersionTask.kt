import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

/**
 * 版本递增任务：读取 [configFile] 中的 VERSION_NAME，按 [level] 规则递增后回写配置文件。
 * versionCode 由语义化版本自动派生，无需也不会被手动修改。
 */
abstract class BumpVersionTask : DefaultTask() {

    @get:Input
    abstract val level: Property<String>

    @get:InputFile
    abstract val configFile: RegularFileProperty

    @TaskAction
    fun bump() {
        val bumpLevel = runCatching { BumpLevel.valueOf(level.get().uppercase()) }.getOrElse {
            throw GradleException("非法递增级别 \"${level.get()}\"，应为 major、minor 或 patch")
        }
        val file = configFile.get().asFile
        if (!file.exists()) throw GradleException("找不到版本配置文件：${file.path}")

        val source = file.readText()
        val match = VERSION_NAME_PATTERN.find(source)
            ?: throw GradleException("在 ${file.name} 中未找到 `const val VERSION_NAME = \"x.y.z\"`")

        val current = SemVersion.parse(match.groupValues[1])
        val next = current.bump(bumpLevel)
        val updated = source.replaceRange(match.groups[1]!!.range, next.toString())
        file.writeText(updated)

        logger.lifecycle("版本已更新：$current -> $next（versionCode: ${current.versionCode} -> ${next.versionCode}）")
    }

    private companion object {
        val VERSION_NAME_PATTERN = Regex("""const val VERSION_NAME = "(\d+\.\d+\.\d+)"""")
    }
}
