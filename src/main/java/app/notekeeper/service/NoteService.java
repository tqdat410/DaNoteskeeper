package app.notekeeper.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import app.notekeeper.model.dto.request.NoteUpdateRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.NoteResponse;
import app.notekeeper.model.enums.NoteType;

public interface NoteService {

    JSendResponse<NoteResponse> getNoteDetail(UUID noteId);

    JSendResponse<NoteResponse> updateTextNote(UUID noteId, NoteUpdateRequest request);

    JSendResponse<Void> deleteNote(UUID noteId);

    JSendResponse<Page<NoteResponse>> getNotes(UUID topicId, NoteType type, Pageable pageable);

}
