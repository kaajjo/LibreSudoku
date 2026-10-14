package com.kaajjo.libresudoku.fileimport

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.kaajjo.libresudoku.ui.import_from_file.ImportFromFileViewModel
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

@OptIn(ExperimentalCoroutinesApi::class)
class ImportFromFileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ImportFromFileViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ImportFromFileViewModel(
            insertFolderUseCase = mockk(),
            boardRepository = mockk(),
            savedStateHandle = SavedStateHandle(mapOf("folderUid" to -1L, "fromDeepLink" to false))
        )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun detectsRealExportAndClosesStream() = runTest(dispatcher) {
        val content = requireNotNull(javaClass.getResource("/file-import/opensudoku-v3-game.opensudoku")).readBytes()
        val stream = TrackingStream(content)
        read(stream)

        assertEquals(1, viewModel.sudokuListToImport.value.size)
        assertFalse(viewModel.importError.value)
        assertFalse(viewModel.isLoading)
        assertTrue(stream.closed)
    }

    @Test
    fun invalidFileClearsPreviousBoardsAndSubsequentSuccessClearsError() = runTest(dispatcher) {
        val content = requireNotNull(javaClass.getResource("/file-import/opensudoku-v3-game.opensudoku")).readBytes()
        read(TrackingStream(content))
        assertEquals(1, viewModel.sudokuListToImport.value.size)

        read(TrackingStream("<opensudoku/>".toByteArray()))
        assertTrue(viewModel.importError.value)
        assertTrue(viewModel.sudokuListToImport.value.isEmpty())
        assertFalse(viewModel.isLoading)

        read(TrackingStream(content))
        assertFalse(viewModel.importError.value)
        assertEquals(1, viewModel.sudokuListToImport.value.size)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun readFailureClosesStreamAndStopsLoading() = runTest(dispatcher) {
        var closed = false
        val stream = object : InputStream() {
            override fun read(): Int = throw IOException("Unreadable import")
            override fun close() {
                closed = true
            }
        }
        read(stream)

        assertTrue(closed)
        assertTrue(viewModel.importError.value)
        assertTrue(viewModel.sudokuListToImport.value.isEmpty())
        assertFalse(viewModel.isLoading)
    }

    private suspend fun read(stream: InputStream) {
        viewModel.readData(InputStreamReader(stream, Charsets.UTF_8))
        viewModel.viewModelScope.coroutineContext.job.children.toList().joinAll()
    }

    private class TrackingStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var closed = false
            private set

        override fun close() {
            closed = true
            super.close()
        }
    }
}
