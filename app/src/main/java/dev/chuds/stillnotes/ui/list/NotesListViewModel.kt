package dev.chuds.stillnotes.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.chuds.stillnotes.data.Note
import dev.chuds.stillnotes.data.NotesRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class NotesListViewModel(
    private val repository: NotesRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _tagFilter = MutableStateFlow<String?>(null)
    val tagFilter: StateFlow<String?> = _tagFilter.asStateFlow()

    /**
     * The list shown to the UI. Combines repository state with the current query and tag filter.
     * Search is debounced and falls back to the full repository list when the query is empty.
     */
    val visibleNotes: StateFlow<List<Note>> = combine(
        repository.notes,
        _query.debounce(120).distinctUntilChanged(),
        _tagFilter,
    ) { notes, query, tag ->
        Triple(notes, query, tag)
    }.flatMapLatest { (notes, query, tag) ->
        val base = if (query.isBlank()) flowOf(notes) else flowOf(repository.search(query))
        base.mapLatest { list ->
            if (tag == null) list else list.filter { it.tags.contains(tag) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Distinct set of tags across the unfiltered library, sorted alphabetically.
     */
    val knownTags: StateFlow<List<String>> = repository.notes
        .mapLatest { notes -> notes.flatMap { it.tags }.toSortedSet().toList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch { repository.load() }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setTagFilter(tag: String?) {
        _tagFilter.value = tag
    }

    fun create(onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val note = repository.create()
            onCreated(note.id)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun togglePinned(id: String) {
        viewModelScope.launch { repository.togglePinned(id) }
    }

    companion object {
        fun factory(repository: NotesRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    NotesListViewModel(repository) as T
            }
    }
}
