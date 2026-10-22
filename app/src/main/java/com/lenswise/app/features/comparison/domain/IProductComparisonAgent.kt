package com.lenswise.app.features.comparison.domain

import com.lenswise.app.core.ai.agent.IScannerAgent
import com.lenswise.app.domain.model.comparison.ProductComparisonRecord

/**
 * 雙雄商品比對分析業務契約介面 (IProductComparisonAgent)。
 * 繼承全域掃描分析契約 [IScannerAgent]，將輸入照片串流解析為漸進式商品比對快照。
 */
interface IProductComparisonAgent : IScannerAgent<ProductComparisonRecord>
