package com.lenswise.app.features.receipt.domain

import com.lenswise.app.core.ai.agent.IScannerAgent
import com.lenswise.app.domain.model.receipt.ReceiptScannerRecord

/**
 * 收據萃取與分析業務契約介面 (IReceiptScannerAgent)。
 * 繼承全域掃描分析契約 [IScannerAgent]，將輸入照片串流解析為漸進式收據快照。
 */
interface IReceiptScannerAgent : IScannerAgent<ReceiptScannerRecord>
