package com.lenswise.app

import com.lenswise.app.core.storage.SettingsPreferencesDataSource
import com.lenswise.app.presentation.features.settings.SettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {

    private class FakeSettingsPreferencesDataSource(
        private var dark: Boolean = true,
        private var notif: Boolean = true,
        private var api: Boolean = true,
        private var lang: String = "繁體中文"
    ) : SettingsPreferencesDataSource {
        override fun isDarkMode(): Boolean = dark
        override fun setDarkMode(enabled: Boolean) { dark = enabled }
        override fun isNotificationLogEnabled(): Boolean = notif
        override fun setNotificationLogEnabled(enabled: Boolean) { notif = enabled }
        override fun isApiLogEnabled(): Boolean = api
        override fun setApiLogEnabled(enabled: Boolean) { api = enabled }
        override fun getTargetLanguage(): String = lang
        override fun setTargetLanguage(language: String) { lang = language }
    }

    @Test
    fun testSettingsInitialStateRestoration() {
        val fakeSource = FakeSettingsPreferencesDataSource(
            dark = false,
            notif = true,
            api = false,
            lang = "English"
        )
        val viewModel = SettingsViewModel(fakeSource)

        assertFalse(viewModel.uiState.value.isDarkMode)
        assertTrue(viewModel.uiState.value.isNotificationLogEnabled)
        assertFalse(viewModel.uiState.value.isApiLogEnabled)
        assertEquals("English", viewModel.uiState.value.targetLanguage)
    }

    @Test
    fun testSettingsToggleUpdatesStateAndPersistence() {
        val fakeSource = FakeSettingsPreferencesDataSource()
        val viewModel = SettingsViewModel(fakeSource)

        viewModel.setDarkMode(false)
        assertFalse(viewModel.uiState.value.isDarkMode)
        assertFalse(fakeSource.isDarkMode())

        viewModel.setNotificationLogEnabled(false)
        assertFalse(viewModel.uiState.value.isNotificationLogEnabled)
        assertFalse(fakeSource.isNotificationLogEnabled())

        viewModel.setApiLogEnabled(false)
        assertFalse(viewModel.uiState.value.isApiLogEnabled)
        assertFalse(fakeSource.isApiLogEnabled())

        viewModel.setTargetLanguage("日語")
        assertEquals("日語", viewModel.uiState.value.targetLanguage)
        assertEquals("日語", fakeSource.getTargetLanguage())
    }
}
