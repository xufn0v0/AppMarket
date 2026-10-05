package com.app.market.data.remote.fdroid

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FdroidIndexParsingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun mapsLocalizedMetadataAndPicksHighestVersionCode() {
        val catalog = json.decodeFromString<FdroidIndexV2Json>(SAMPLE_INDEX).toCatalog("https://f-droid.org/repo")

        assertEquals("F-Droid", catalog.repoName)
        assertEquals(1_700_000_000_000L, catalog.repoTimestamp)
        // 无可用版本的包被丢弃
        assertEquals(1, catalog.apps.size)

        val app = catalog.apps.single()
        assertEquals("org.fdroid.fdroid", app.packageName)
        // en-US 名称优先
        assertEquals("F-Droid", app.displayName)
        assertEquals("The official app store", app.summary)
        assertEquals("F-Droid Limited", app.author)
        assertEquals("Internet", app.category)
        assertEquals(
            "https://f-droid.org/repo/org.fdroid.fdroid/en-US/icon.png",
            app.iconUrl,
        )
        // 最高 versionCode 的版本胜出，更新日志取版本级 whatsNew
        assertEquals(200L, app.latest.versionCode)
        assertEquals("2.0", app.latest.versionName)
        assertEquals("org.fdroid.fdroid_200.apk", app.latest.fileName)
        assertEquals("bbbb", app.latest.sha256)
        assertEquals(2048L, app.latest.size)
        assertEquals("New in 2.0", app.latest.changeLog)
    }

    @Test
    fun sanitizesDescriptionAndAbsolutizesScreenshots() {
        val catalog = json.decodeFromString<FdroidIndexV2Json>(SAMPLE_INDEX).toCatalog("https://f-droid.org/repo/")
        val app = catalog.apps.single()

        // Markdown 链接保留文字、HTML 标签剥离
        assertTrue(app.description.contains("home page"), app.description)
        assertTrue(!app.description.contains("<b>"), app.description)
        assertTrue(!app.description.contains("](http"), app.description)
        assertEquals(
            listOf("https://f-droid.org/repo/org.fdroid.fdroid/en-US/phoneScreenshots/1.png"),
            app.screenshots,
        )
    }

    @Test
    fun mapsCurrentOfficialV30000SchemaWithFileRefsAndManifest() {
        val catalog = json.decodeFromString<FdroidIndexV2Json>(SAMPLE_INDEX_V30000)
            .toCatalog("https://f-droid.org/repo")

        val app = catalog.apps.single { it.packageName == "org.fdroid.fdroid" }
        // icon 为文件对象，路径以 "/" 开头（仓库根相对）
        assertEquals(
            "https://f-droid.org/repo/org.fdroid.fdroid/en-US/icon_x.png",
            app.iconUrl,
        )
        // screenshots.<orientation>.<locale> 文件对象列表
        assertEquals(
            listOf("https://f-droid.org/repo/org.fdroid.fdroid/en-US/phoneScreenshots/1.png"),
            app.screenshots,
        )
        // 版本信息取自 manifest；APK 路径去掉前导斜杠
        assertEquals(2000051L, app.latest.versionCode)
        assertEquals("2.0.1", app.latest.versionName)
        assertEquals(24, app.latest.sdkVersion)
        assertEquals(37, app.latest.targetSdkVersion)
        assertEquals("org.fdroid.fdroid_2000051.apk", app.latest.fileName)
        assertEquals("cccc", app.latest.sha256)
        // 旧版顶层字段在 manifest 缺失时回退（空版本包仍被丢弃）
        assertEquals(1, catalog.apps.size)
    }

    private companion object {
        const val SAMPLE_INDEX = """
{
  "repo": {
    "name": {"en": "F-Droid"},
    "address": "https://f-droid.org/repo",
    "timestamp": 1700000000000,
    "version": 20001,
    "maxage": 1209600
  },
  "packages": {
    "org.fdroid.fdroid": {
      "metadata": {
        "name": {"en-US": "F-Droid", "zh-CN": "F-Droid 应用商店"},
        "summary": {"en": "The official app store"},
        "description": {"en": "Visit the [home page](https://f-droid.org) or <b>read on</b>.\nSecond line."},
        "icon": {"en-US": "org.fdroid.fdroid/en-US/icon.png"},
        "authorName": "F-Droid Limited",
        "webSite": "https://f-droid.org",
        "categories": ["Internet", "Security"],
        "added": 1600000000000,
        "lastUpdated": 1700000000000,
        "whatsNew": {"en": "Repo level changelog"},
        "phoneScreenshots": {"en-US": ["org.fdroid.fdroid/en-US/phoneScreenshots/1.png"]}
      },
      "versions": {
        "v-old": {
          "file": {"name": "org.fdroid.fdroid_100.apk", "sha256": "aaaa", "size": 1024},
          "versionName": "1.0",
          "versionCode": 100,
          "sdkVersion": 23
        },
        "v-new": {
          "file": {"name": "org.fdroid.fdroid_200.apk", "sha256": "bbbb", "size": 2048},
          "versionName": "2.0",
          "versionCode": 200,
          "sdkVersion": 23,
          "targetSdkVersion": 34,
          "whatsNew": {"en": "New in 2.0"}
        }
      }
    },
    "org.empty.legacy": {
      "metadata": {"name": {"en": "Legacy"}},
      "versions": {}
    }
  }
}
"""
        const val SAMPLE_INDEX_V30000 = """
{
  "repo": {
    "name": {"en-US": "F-Droid"},
    "address": "https://f-droid.org/repo",
    "timestamp": 1790000000000,
    "version": 30000
  },
  "packages": {
    "org.fdroid.fdroid": {
      "metadata": {
        "name": {"en-US": "F-Droid"},
        "summary": {"en-US": "The official app store"},
        "description": {"en-US": "Catalog of FOSS apps."},
        "icon": {"en-US": {"name": "/org.fdroid.fdroid/en-US/icon_x.png", "sha256": "iconhash", "size": 9649}},
        "authorName": "F-Droid Limited",
        "categories": ["System"],
        "added": 1295222400000,
        "lastUpdated": 1790715170525,
        "screenshots": {
          "phone": {
            "en-US": [
              {"name": "/org.fdroid.fdroid/en-US/phoneScreenshots/1.png", "sha256": "shot1", "size": 109644}
            ]
          }
        }
      },
      "versions": {
        "old": {
          "file": {"name": "/org.fdroid.fdroid_1000000.apk", "sha256": "aaaa", "size": 10000000},
          "added": 1700000000000,
          "whatsNew": {"en-US": "older"},
          "manifest": {
            "versionName": "1.0.0",
            "versionCode": 1000000,
            "usesSdk": {"minSdkVersion": 24, "targetSdkVersion": 35},
            "signer": {"sha256": ["43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab"]}
          }
        },
        "new": {
          "file": {"name": "/org.fdroid.fdroid_2000051.apk", "sha256": "cccc", "size": 12537183},
          "added": 1790715170525,
          "whatsNew": {"en-US": "Latest release"},
          "manifest": {
            "versionName": "2.0.1",
            "versionCode": 2000051,
            "usesSdk": {"minSdkVersion": 24, "targetSdkVersion": 37},
            "signer": {"sha256": ["43238d512c1e5eb2d6569f4a3afbf5523418b82e0a3ed1552770abb9a9c9ccab"]}
          }
        }
      }
    }
  }
}
"""
    }
}
