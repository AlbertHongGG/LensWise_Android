package com.lenswise.app.core.backup

import com.lenswise.app.core.backup.contract.BackupContributionModule
import com.lenswise.app.core.backup.engine.BackupArchiveEngine
import com.lenswise.app.core.backup.model.RestoreStrategy
import com.lenswise.app.core.backup.module.DirectoryLogBackupContributionModule
import com.lenswise.app.core.backup.module.HistoryBackupContributionModule
import com.lenswise.app.core.image.AppImageEntity
import com.lenswise.app.core.image.ImageSource
import com.lenswise.app.domain.model.comparison.ComparisonProduct
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord
import com.lenswise.app.domain.model.menu.MenuScannerItem
import com.lenswise.app.domain.model.menu.MenuScannerRecord
import com.lenswise.app.domain.model.product.ProductLensRecord
import com.lenswise.app.domain.model.receipt.ReceiptItem
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord
import com.lenswise.app.features.comparison.data.ComparisonHistoryRepository
import com.lenswise.app.features.menu.data.MenuHistoryRepository
import com.lenswise.app.features.product.data.ProductHistoryRepository
import com.lenswise.app.features.receipt.data.ReceiptHistoryRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class BackupArchiveEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var deviceADir: File
    private lateinit var deviceBDir: File
    private lateinit var engineTempDir: File

    // Repositories for Device A
    private lateinit var receiptRepoA: ReceiptHistoryRepository
    private lateinit var productRepoA: ProductHistoryRepository
    private lateinit var comparisonRepoA: ComparisonHistoryRepository
    private lateinit var menuRepoA: MenuHistoryRepository

    // Repositories for Device B
    private lateinit var receiptRepoB: ReceiptHistoryRepository
    private lateinit var productRepoB: ProductHistoryRepository
    private lateinit var comparisonRepoB: ComparisonHistoryRepository
    private lateinit var menuRepoB: MenuHistoryRepository

    private var reloadNotificationCountB = 0
    private var reloadApiLogCountB = 0

    @Before
    fun setUp() {
        deviceADir = tempFolder.newFolder("device_A_files")
        deviceBDir = tempFolder.newFolder("device_B_files")
        engineTempDir = tempFolder.newFolder("engine_temp")

        receiptRepoA = ReceiptHistoryRepository(deviceADir)
        productRepoA = ProductHistoryRepository(deviceADir)
        comparisonRepoA = ComparisonHistoryRepository(deviceADir)
        menuRepoA = MenuHistoryRepository(deviceADir)

        receiptRepoB = ReceiptHistoryRepository(deviceBDir)
        productRepoB = ProductHistoryRepository(deviceBDir)
        comparisonRepoB = ComparisonHistoryRepository(deviceBDir)
        menuRepoB = MenuHistoryRepository(deviceBDir)
    }

    private fun createDummyImageEntity(name: String, content: String): AppImageEntity {
        val file = File(deviceADir, "$name.jpg").apply { writeText(content) }
        return AppImageEntity(
            file = file,
            width = 800,
            height = 800,
            source = ImageSource.CAMERA
        )
    }

    @Test
    fun testFullBackupAndCrossDeviceRestore_roundTrip() = runBlocking {
        // 1. 在裝置 A 寫入各項歷史資料與照片
        val receiptPhoto = createDummyImageEntity("photo_rec", "receipt_photo_data")
        val recPaths = receiptRepoA.saveImages("rec_1", listOf(receiptPhoto))
        receiptRepoA.save(
            ReceiptScannerRecord(
                id = "rec_1",
                storeName = "7-Eleven 台北",
                totalAmount = 100.0,
                items = listOf(ReceiptItem(originalName = "綠茶", translatedName = "Green Tea", price = 30.0)),
                imagePaths = recPaths
            )
        )

        val prodPhoto = createDummyImageEntity("photo_prod", "product_photo_data")
        val prodPaths = productRepoA.saveImages("prod_1", listOf(prodPhoto))
        productRepoA.save(
            ProductLensRecord(
                id = "prod_1",
                productName = "日清兵衛豆皮烏龍麵",
                brand = "日清",
                imagePaths = prodPaths
            )
        )

        val compPhoto1 = createDummyImageEntity("photo_comp1", "comp_photo_data_1")
        val compPhoto2 = createDummyImageEntity("photo_comp2", "comp_photo_data_2")
        val compPaths = comparisonRepoA.saveImages("comp_1", listOf(compPhoto1, compPhoto2))
        comparisonRepoA.save(
            ProductComparisonRecord(
                id = "comp_1",
                title = "烏龍麵 vs 拉麵",
                products = listOf(
                    ComparisonProduct(originalName = "A", translatedName = "A-Trans"),
                    ComparisonProduct(originalName = "B", translatedName = "B-Trans")
                ),
                imagePaths = compPaths
            )
        )

        val menuPhoto = createDummyImageEntity("photo_menu", "menu_photo_data")
        val menuPaths = menuRepoA.saveImages("menu_1", listOf(menuPhoto))
        menuRepoA.save(
            MenuScannerRecord(
                id = "menu_1",
                storeName = "一蘭拉麵",
                items = listOf(MenuScannerItem(originalName = "ラーメン", translatedName = "拉麵", price = 980.0)),
                imagePaths = menuPaths
            )
        )

        // 寫入日誌檔至裝置 A
        val notifLogDirA = File(deviceADir, "logs/notifications").apply { mkdirs() }
        File(notifLogDirA, "20261103_notif_1.json").writeText("""{"timestamp":"2026-11-03T12:00:00","type":"success","message":"Analysis done"}""")

        val apiLogDirA = File(deviceADir, "logs/api").apply { mkdirs() }
        File(apiLogDirA, "20261103_api_1.json").writeText("""{"agentName":"ReceiptScanner","timestamp":"2026-11-03T12:00:00"}""")

        // 2. 建立裝置 A 的 BackupArchiveEngine 並執行打包
        val modulesA = listOf<BackupContributionModule>(
            HistoryBackupContributionModule(receiptRepoA, "收據歷史紀錄"),
            HistoryBackupContributionModule(productRepoA, "商品歷史紀錄"),
            HistoryBackupContributionModule(comparisonRepoA, "商品比對歷史紀錄"),
            HistoryBackupContributionModule(menuRepoA, "菜單掃描歷史紀錄"),
            DirectoryLogBackupContributionModule(deviceADir, "logs/notifications", "logs_notifications", "系統通知日誌") {},
            DirectoryLogBackupContributionModule(deviceADir, "logs/api", "logs_api", "AI API 互動日誌") {}
        )
        val engineA = BackupArchiveEngine(modulesA, engineTempDir)

        val zipOutStream = ByteArrayOutputStream()
        val backupResult = engineA.createBackupZip(zipOutStream)

        assertTrue(backupResult.isSuccess)
        val backupSummary = backupResult.getOrThrow()
        assertEquals(4, backupSummary.manifest.totalRecords)
        assertEquals(5, backupSummary.manifest.totalPhotos)
        assertEquals(2, backupSummary.manifest.totalLogs)
        assertTrue(backupSummary.outputSizeBytes > 0)

        // 3. 在全新的裝置 B 上執行還原
        val zipBytes = zipOutStream.toByteArray()
        val modulesB = listOf<BackupContributionModule>(
            HistoryBackupContributionModule(receiptRepoB, "收據歷史紀錄"),
            HistoryBackupContributionModule(productRepoB, "商品歷史紀錄"),
            HistoryBackupContributionModule(comparisonRepoB, "商品比對歷史紀錄"),
            HistoryBackupContributionModule(menuRepoB, "菜單掃描歷史紀錄"),
            DirectoryLogBackupContributionModule(deviceBDir, "logs/notifications", "logs_notifications", "系統通知日誌") {
                reloadNotificationCountB++
            },
            DirectoryLogBackupContributionModule(deviceBDir, "logs/api", "logs_api", "AI API 互動日誌") {
                reloadApiLogCountB++
            }
        )
        val engineB = BackupArchiveEngine(modulesB, engineTempDir)

        val restoreResult = engineB.restoreFromZip(ByteArrayInputStream(zipBytes), RestoreStrategy.MERGE_OVERWRITE)
        assertTrue(restoreResult.isSuccess)

        val restoreSummary = restoreResult.getOrThrow()
        assertEquals(4, restoreSummary.totalRecordsRestored)
        assertEquals(5, restoreSummary.totalPhotosRestored)
        assertEquals(2, restoreSummary.totalLogsRestored)
        assertEquals(1, reloadNotificationCountB)
        assertEquals(1, reloadApiLogCountB)

        // 4. 驗證裝置 B 的資料完整性
        val receiptsB = receiptRepoB.getAll()
        assertEquals(1, receiptsB.size)
        assertEquals("7-Eleven 台北", receiptsB[0].storeName)
        assertEquals("history/receipt_images/rec_1_0.jpg", receiptsB[0].imagePaths[0])
        val receiptPhotoOnB = File(deviceBDir, receiptsB[0].imagePaths[0])
        assertTrue(receiptPhotoOnB.exists())
        assertEquals("receipt_photo_data", receiptPhotoOnB.readText())

        val productsB = productRepoB.getAll()
        assertEquals(1, productsB.size)
        assertEquals("日清兵衛豆皮烏龍麵", productsB[0].productName)
        val prodPhotoOnB = File(deviceBDir, productsB[0].imagePaths[0])
        assertTrue(prodPhotoOnB.exists())
        assertEquals("product_photo_data", prodPhotoOnB.readText())

        val compsB = comparisonRepoB.getAll()
        assertEquals(1, compsB.size)
        assertEquals("烏龍麵 vs 拉麵", compsB[0].title)
        assertEquals(2, compsB[0].imagePaths.size)
        assertEquals("comp_photo_data_1", File(deviceBDir, compsB[0].imagePaths[0]).readText())
        assertEquals("comp_photo_data_2", File(deviceBDir, compsB[0].imagePaths[1]).readText())

        val menusB = menuRepoB.getAll()
        assertEquals(1, menusB.size)
        assertEquals("一蘭拉麵", menusB[0].storeName)
        val menuPhotoOnB = File(deviceBDir, menusB[0].imagePaths[0])
        assertTrue(menuPhotoOnB.exists())
        assertEquals("menu_photo_data", menuPhotoOnB.readText())

        // 驗證日誌檔在裝置 B 上存在
        val notifLogOnB = File(deviceBDir, "logs/notifications/20261103_notif_1.json")
        val apiLogOnB = File(deviceBDir, "logs/api/20261103_api_1.json")
        assertTrue(notifLogOnB.exists())
        assertTrue(apiLogOnB.exists())
    }

    @Test
    fun testCleanReplaceStrategy_cleansTargetAndReplaces() = runBlocking {
        // 先在裝置 B 加入一筆舊紀錄
        receiptRepoB.save(ReceiptScannerRecord(id = "old_rec", storeName = "Old Store"))
        assertEquals(1, receiptRepoB.getAll().size)

        // 在裝置 A 建立新紀錄並打包
        receiptRepoA.save(ReceiptScannerRecord(id = "new_rec", storeName = "New Store"))
        val modulesA = listOf<BackupContributionModule>(
            HistoryBackupContributionModule(receiptRepoA, "收據歷史紀錄")
        )
        val engineA = BackupArchiveEngine(modulesA, engineTempDir)
        val zipOutStream = ByteArrayOutputStream()
        engineA.createBackupZip(zipOutStream)

        // 在裝置 B 以 CLEAN_REPLACE 還原
        val modulesB = listOf<BackupContributionModule>(
            HistoryBackupContributionModule(receiptRepoB, "收據歷史紀錄")
        )
        val engineB = BackupArchiveEngine(modulesB, engineTempDir)
        engineB.restoreFromZip(ByteArrayInputStream(zipOutStream.toByteArray()), RestoreStrategy.CLEAN_REPLACE)

        val recordsB = receiptRepoB.getAll()
        assertEquals(1, recordsB.size)
        assertEquals("new_rec", recordsB[0].id)
        assertEquals("New Store", recordsB[0].storeName)
    }
}
