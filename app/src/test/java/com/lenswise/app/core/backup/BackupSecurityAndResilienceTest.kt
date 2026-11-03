package com.lenswise.app.core.backup

import com.lenswise.app.core.backup.contract.BackupContributionModule
import com.lenswise.app.core.backup.engine.BackupArchiveEngine
import com.lenswise.app.core.backup.model.ModuleMetadata
import com.lenswise.app.core.backup.model.RestoreStrategy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupSecurityAndResilienceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var sandboxDir: File
    private lateinit var engineTempDir: File
    private lateinit var engine: BackupArchiveEngine

    private val dummyModule = object : BackupContributionModule {
        override val moduleId: String = "dummy"
        override val displayName: String = "Dummy"
        override suspend fun exportToStaging(stagingRoot: File): ModuleMetadata = ModuleMetadata()
        override suspend fun importFromStaging(stagingRoot: File, strategy: RestoreStrategy): ModuleMetadata = ModuleMetadata()
        override fun reloadMemoryState() {}
    }

    @Before
    fun setUp() {
        sandboxDir = tempFolder.newFolder("sandbox")
        engineTempDir = tempFolder.newFolder("engine_temp")
        engine = BackupArchiveEngine(listOf(dummyModule), engineTempDir)
    }

    @Test
    fun testZipSlipVulnerabilityAttack_isBlockedAndThrowsSecurityException() = runBlocking {
        // 手工構造包含路徑穿越攻擊的惡意 ZIP
        val maliciousZipBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                val evilEntry = ZipEntry("../../evil_script.sh")
                zos.putNextEntry(evilEntry)
                zos.write("echo 'hacked'".toByteArray())
                zos.closeEntry()
            }
            baos.toByteArray()
        }

        val result = engine.restoreFromZip(ByteArrayInputStream(maliciousZipBytes))

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is SecurityException)
        assertTrue(exception?.message?.contains("Zip Slip") == true)

        // 驗證惡意檔案絕對沒有穿透寫入 sandbox 外層目錄
        val escapedFile = File(engineTempDir.parentFile, "evil_script.sh")
        assertFalse(escapedFile.exists())
    }

    @Test
    fun testEmptyZip_failsGracefullyWithMeaningfulError() = runBlocking {
        // 空 ZIP 壓縮包
        val emptyZipBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                // zero entries
            }
            baos.toByteArray()
        }

        val result = engine.restoreFromZip(ByteArrayInputStream(emptyZipBytes))

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("為空或非有效壓縮檔") == true)
    }

    @Test
    fun testZipMissingManifest_failsGracefullyWithManifestError() = runBlocking {
        // 有檔案但缺少 manifest.json 的 ZIP
        val noManifestZipBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                val entry = ZipEntry("history/receipts/rec_1.json")
                zos.putNextEntry(entry)
                zos.write("{}".toByteArray())
                zos.closeEntry()
            }
            baos.toByteArray()
        }

        val result = engine.restoreFromZip(ByteArrayInputStream(noManifestZipBytes))

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalArgumentException)
        assertTrue(exception?.message?.contains("缺少根目錄 manifest.json") == true)
    }

    @Test
    fun testHigherSchemaVersion_failsGracefullyToPreventDataCorruption() = runBlocking {
        // schemaVersion = 999 的未來版本 ZIP
        val futureManifestZipBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                val entry = ZipEntry("manifest.json")
                zos.putNextEntry(entry)
                zos.write("""{"schemaVersion": 999, "totalRecords": 0}""".toByteArray())
                zos.closeEntry()
            }
            baos.toByteArray()
        }

        val result = engine.restoreFromZip(ByteArrayInputStream(futureManifestZipBytes))

        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertTrue(exception?.message?.contains("高於當前 App 支援之版本") == true)
    }
}
