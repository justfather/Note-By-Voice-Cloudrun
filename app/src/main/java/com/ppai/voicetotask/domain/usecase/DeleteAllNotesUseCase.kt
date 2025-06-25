package com.ppai.voicetotask.domain.usecase

import com.ppai.voicetotask.domain.repository.NoteRepository
import javax.inject.Inject

class DeleteAllNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke() {
        noteRepository.deleteAllNotes()
    }
}