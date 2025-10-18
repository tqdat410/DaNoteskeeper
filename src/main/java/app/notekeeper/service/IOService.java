package app.notekeeper.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import app.notekeeper.model.dto.request.FileUploadRequest;
import app.notekeeper.model.dto.request.TextUploadRequest;
import app.notekeeper.model.dto.response.JSendResponse;

public interface IOService {

    JSendResponse<Void> uploadFile(MultipartFile file, FileUploadRequest fileUploadRequest);

    JSendResponse<Void> uploadText(TextUploadRequest textUploadRequest);

    Resource loadFileAsResource(String fileUrl);

    void deleteFile(String fileUrl);

}
