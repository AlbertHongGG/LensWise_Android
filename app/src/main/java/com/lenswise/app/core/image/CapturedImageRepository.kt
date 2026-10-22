package com.lenswise.app.core.image

import kotlinx.coroutines.flow.StateFlow

/**
 * 待分析照片暫存倉儲契約 (CapturedImageRepository)。
 *
 * 核心保證：
 * 1. 唯一真實來源 (SSOT)：統一管理相機拍攝與相簿挑選的待處理照片集合。
 * 2. 跨模式持久化：無論使用者在相機主畫面如何切換模式（收據、商品、比對、菜單），照片狀態始終保持。
 * 3. 精確消費清除：分析成功落盤後，精確清除已消費照片；分析失敗時完整保留。
 */
interface CapturedImageRepository {

    /** 響應式待處理相片集合狀態流 */
    val images: StateFlow<List<AppImageEntity>>

    /** 當前照片張數快照 */
    val count: Int get() = images.value.size

    /** 新增一組照片至暫存池 */
    fun addImages(newImages: List<AppImageEntity>)

    /** 依索引移除單張照片 */
    fun removeImage(index: Int)

    /** 依 ID 移除特定照片 */
    fun removeImageById(id: String)

    /** 全數清空所有暫存照片 */
    fun clear()

    /**
     * 成功完成業務消費時呼叫。
     * 精確比對相片實體 ID 並自暫存池中移除已處理的照片，
     * 若在分析過程中或併發情境下有新照片加入，新照片將安全保留。
     */
    fun clearConsumed(consumedImages: List<AppImageEntity>)
}
