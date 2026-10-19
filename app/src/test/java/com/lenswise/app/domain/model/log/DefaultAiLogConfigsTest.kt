package com.lenswise.app.domain.model.log

import com.lenswise.app.features.ai.domain.AiAgentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAiLogConfigsTest {

    @Test
    fun testAllTabs_containsEightFixedTabsWithAllAsFirst() {
        val tabs = DefaultAiLogConfigs.allTabs

        assertEquals(8, tabs.size)
        assertEquals("all", tabs[0].id)
        assertEquals("全部", tabs[0].tabName)
        assertNull(tabs[0].agentType)

        val agentTabs = tabs.drop(1)
        assertEquals(7, agentTabs.size)

        // 確保涵蓋所有現有的 AiAgentType
        val configuredTypes = agentTabs.mapNotNull { it.agentType }.toSet()
        val expectedTypes = AiAgentType.entries.toSet()
        assertEquals(expectedTypes, configuredTypes)

        // 確保每個 Tab 均有非空的空狀態標題與說明
        tabs.forEach { tab ->
            assertTrue(tab.tabName.isNotBlank())
            assertTrue(tab.emptyStateTitle.isNotBlank())
            assertTrue(tab.emptyStateDescription.isNotBlank())
        }
    }
}
