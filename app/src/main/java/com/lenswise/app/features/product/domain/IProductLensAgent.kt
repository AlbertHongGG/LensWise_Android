package com.lenswise.app.features.product.domain

import com.lenswise.app.core.ai.agent.IScannerAgent
import com.lenswise.app.domain.model.product.ProductLensRecord

/**
 * 商品影像特徵反查與分析業務契約介面 (IProductLensAgent)。
 * 繼承全域掃描分析契約 [IScannerAgent]，將輸入照片串流解析為漸進式商品透視快照。
 */
interface IProductLensAgent : IScannerAgent<ProductLensRecord>
