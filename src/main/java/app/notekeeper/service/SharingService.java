package app.notekeeper.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import app.notekeeper.model.dto.request.ShareNoteRequest;
import app.notekeeper.model.dto.request.ShareTopicRequest;
import app.notekeeper.model.dto.response.JSendResponse;
import app.notekeeper.model.dto.response.NoteResponse;
import app.notekeeper.model.dto.response.SharedNoteResponse;
import app.notekeeper.model.dto.response.SharedTopicResponse;

public interface SharingService {

    // ==================== TOPIC SHARING (Owner perspective) ====================

    /**
     * Share a topic with another user via email
     * 
     * @param request containing topicId and user email
     * @return SharedTopicResponse with sharing details
     */
    JSendResponse<SharedTopicResponse> shareTopic(ShareTopicRequest request);

    /**
     * Remove shared access to a topic
     * 
     * @param sharedTopicId ID of the SharedTopic record to delete
     * @return success message
     */
    JSendResponse<Void> unshareTopic(UUID sharedTopicId);

    /**
     * Get all users who have access to a specific topic
     * 
     * @param topicId ID of the topic
     * @return list of SharedTopicResponse
     */
    JSendResponse<List<SharedTopicResponse>> getSharedUsers(UUID topicId);

    // ==================== NOTE SHARING (Owner perspective) ====================

    /**
     * Share a note with another user via email
     * 
     * @param request containing noteId and user email
     * @return SharedNoteResponse with sharing details
     */
    JSendResponse<SharedNoteResponse> shareNote(ShareNoteRequest request);

    /**
     * Remove shared access to a note
     * 
     * @param sharedNoteId ID of the SharedNote record to delete
     * @return success message
     */
    JSendResponse<Void> unshareNote(UUID sharedNoteId);

    /**
     * Get all users who have access to a specific note
     * 
     * @param noteId ID of the note
     * @return list of SharedNoteResponse
     */
    JSendResponse<List<SharedNoteResponse>> getSharedUsersForNote(UUID noteId);

    // ==================== SHARED WITH ME (Recipient perspective)
    // ====================

    /**
     * Get all topics shared with the current user (paginated)
     * 
     * @param pageable pagination parameters
     * @return page of SharedTopicResponse
     */
    JSendResponse<Page<SharedTopicResponse>> getTopicsSharedWithMe(Pageable pageable);

    /**
     * Get details of a specific topic shared with me
     * 
     * @param topicId ID of the shared topic
     * @return SharedTopicResponse with topic and owner details
     */
    JSendResponse<SharedTopicResponse> getSharedTopicDetail(UUID topicId);

    /**
     * Get all notes shared with the current user (paginated)
     * 
     * @param pageable pagination parameters
     * @return page of SharedNoteResponse
     */
    JSendResponse<Page<SharedNoteResponse>> getNotesSharedWithMe(Pageable pageable);

    /**
     * Get shared note detail (for all note types)
     * Returns content for TEXT notes, fileUrl for IMAGE/DOCUMENT notes
     * 
     * @param noteId ID of the shared note
     * @return NoteResponse with note details
     */
    JSendResponse<NoteResponse> getSharedNoteDetail(UUID noteId);

    /**
     * Get all notes in a topic shared with the current user (paginated)
     * 
     * @param topicId  ID of the shared topic
     * @param pageable pagination parameters
     * @return page of NoteResponse
     */
    JSendResponse<Page<NoteResponse>> getNotesInSharedTopic(UUID topicId, Pageable pageable);

    /**
     * Get detail of a note in a topic shared with the current user
     * 
     * @param topicId ID of the shared topic
     * @param noteId  ID of the note
     * @return NoteResponse with note details
     */
    JSendResponse<NoteResponse> getNoteDetailInSharedTopic(UUID topicId, UUID noteId);
}
