package app.notekeeper.service;

import java.util.UUID;

import app.notekeeper.event.NoteCreatedEvent;
import app.notekeeper.model.dto.request.RetrieveNoteRequest;
import app.notekeeper.model.dto.response.RetrieveNoteResponse;

public interface AIService {

    /**
     * Process note: classify into topic, extract content (for IMAGE/DOCUMENT), and
     * generate embedding
     */
    void processNote(NoteCreatedEvent event);

    /**
     * Retrieve and answer user query based on similar notes
     * 
     * @param request Retrieve note request with query and optional topic filter
     * @param userId  Current user ID
     * @return Response with AI-generated answer and relevant notes
     */
    RetrieveNoteResponse retrieveNotes(RetrieveNoteRequest request, UUID userId);

}
