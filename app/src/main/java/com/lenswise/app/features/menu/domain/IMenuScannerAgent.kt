package com.lenswise.app.features.menu.domain

import com.lenswise.app.core.ai.agent.IScannerAgent
import com.lenswise.app.domain.model.menu.MenuScannerRecord

/**
 * 菜單掃描與美食結構化分析業務契約介面 (IMenuScannerAgent)。
 * 繼承全域掃描分析契約 [IScannerAgent]，將輸入照片串流解析為漸進式菜單分析快照。
 */
interface IMenuScannerAgent : IScannerAgent<MenuScannerRecord>
