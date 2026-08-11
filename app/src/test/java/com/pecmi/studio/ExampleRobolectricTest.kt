package com.pecmi.studio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pecmi.studio.domain.model.CanvasSettings
import com.pecmi.studio.domain.model.Layer
import com.pecmi.studio.editor.HistoryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pecmi", appName)
    }

    @Test
    fun `history manager push and undo`() {
        val historyManager = HistoryManager()
        val settings = CanvasSettings()
        val layers = listOf(Layer.Text(id = "1", text = "Hello Pecmi"))

        historyManager.pushState(layers, settings)
        historyManager.pushState(layers + Layer.Text(id = "2", text = "Sub"), settings)

        assertTrue(historyManager.canUndo())
        val previousState = historyManager.undo()
        assertEquals(1, previousState?.layers?.size)
    }
}
