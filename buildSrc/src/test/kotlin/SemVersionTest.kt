import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SemVersionTest {

    @Test
    fun parsesComponents() {
        val version = SemVersion.parse("2.3.1")
        assertEquals(2, version.major)
        assertEquals(3, version.minor)
        assertEquals(1, version.patch)
        assertEquals("2.3.1", version.toString())
    }

    @Test
    fun rejectsInvalidInputs() {
        listOf("", "2.3", "2.3.1.0", "v2.3.1", "2.x.1", "-1.0.0").forEach { input ->
            assertFailsWith<IllegalArgumentException>("应拒绝 \"$input\"") { SemVersion.parse(input) }
        }
    }

    @Test
    fun rejectsComponentsOutsideEncodingRange() {
        assertFailsWith<IllegalArgumentException> { SemVersion(1, 100, 0) }
        assertFailsWith<IllegalArgumentException> { SemVersion(1, 0, 100) }
    }

    @Test
    fun derivesVersionCodeFromSemver() {
        assertEquals(0, SemVersion(0, 0, 0).versionCode)
        assertEquals(20301, SemVersion(2, 3, 1).versionCode)
        assertEquals(100_000, SemVersion(10, 0, 0).versionCode)
        assertEquals(29_999, SemVersion(2, 99, 99).versionCode)
    }

    @Test
    fun newEncodingExceedsLastHistoricalCode() {
        // 旧方案（git 提交数）线上最后版本 2.3.1 的 code 为 221；新编码必须保证可升级
        assertTrue(SemVersion.parse("2.3.1").versionCode > 221)
    }

    @Test
    fun bumpAppliesPresetRules() {
        assertEquals(SemVersion(2, 3, 2), SemVersion(2, 3, 1).bump(BumpLevel.PATCH))
        assertEquals(SemVersion(2, 4, 0), SemVersion(2, 3, 9).bump(BumpLevel.MINOR))
        assertEquals(SemVersion(3, 0, 0), SemVersion(2, 9, 9).bump(BumpLevel.MAJOR))
    }

    @Test
    fun everyBumpStrictlyIncreasesVersionCode() {
        var version = SemVersion(0, 0, 0)
        repeat(2_000) { i ->
            val level = when {
                i % 17 == 0 -> BumpLevel.MAJOR
                i % 5 == 0 -> BumpLevel.MINOR
                else -> BumpLevel.PATCH
            }
            val next = version.bump(level)
            assertTrue(next.versionCode > version.versionCode, "$next 必须晚于 $version")
            assertEquals(next, SemVersion.parse(next.toString()))
            version = next
        }
    }

    @Test
    fun versionCodeOrderingMatchesSemverPrecedence() {
        val ordered = listOf(
            SemVersion(0, 0, 1),
            SemVersion(0, 0, 99),
            SemVersion(0, 1, 0),
            SemVersion(1, 0, 0),
            SemVersion(2, 3, 1),
            SemVersion(2, 3, 99),
            SemVersion(2, 4, 0),
            SemVersion(3, 0, 0),
        )
        ordered.zipWithNext { current, next ->
            assertTrue(current < next, "$current 应早于 $next")
            assertTrue(current.versionCode < next.versionCode)
        }
    }
}
