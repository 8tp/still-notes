package dev.chuds.stillnotes.ui.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.chuds.stillnotes.data.NotesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(
    private val repository: NotesRepository,
    val noteId: String,
) : ViewModel() {

    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /**
     * Live word + character counts for the current body. Words are runs of non-whitespace.
     */
    val counts: StateFlow<Counts> = _body
        .map { body ->
            val chars = body.length
            val words = if (body.isBlank()) 0 else body.trim().split(Regex("\\s+")).size
            Counts(words = words, chars = chars)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Counts(0, 0))

    private var pendingSave: Job? = null

    init {
        viewModelScope.launch {
            _body.value = repository.read(noteId)
            _loaded.value = true
        }
    }

    fun update(text: String) {
        _body.value = text
        pendingSave?.cancel()
        pendingSave = viewModelScope.launch {
            delay(450)
            repository.save(noteId, text)
        }
    }

    /**
     * Flush any pending debounced save synchronously. Call from onPause / onBack.
     */
    suspend fun flush() {
        pendingSave?.cancel()
        pendingSave = null
        repository.save(noteId, _body.value)
    }

    data class Counts(val words: Int, val chars: Int)

    companion object {
        fun factory(repository: NotesRepository, noteId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    NoteViewModel(repository, noteId) as T
            }
    }
}
