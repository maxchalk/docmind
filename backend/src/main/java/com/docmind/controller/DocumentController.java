package com.docmind.controller;

import com.docmind.dto.DocumentResponse;
import com.docmind.model.Document;
import com.docmind.model.User;
import com.docmind.service.DocumentService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file,
                                            @AuthenticationPrincipal User user) {
        try {
            Document document = documentService.uploadDocument(file, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(document));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getDocuments(@AuthenticationPrincipal User user) {
        List<Document> documents = documentService.getDocumentsForUser(user);
        List<DocumentResponse> responses = documents.stream()
                .map(DocumentResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id,
                                            @AuthenticationPrincipal User user) {
        try {
            documentService.deleteDocument(id, user);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<?> getDocumentStatus(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            return ResponseEntity.ok(Map.of(
                    "status", document.getStatus(),
                    "chunkCount", document.getChunkCount()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/verify")
    public ResponseEntity<?> verifyDocument(@PathVariable Long id,
                                            @AuthenticationPrincipal User user) {
        try {
            Document document = documentService.verifyDocument(id, user);
            return ResponseEntity.ok(DocumentResponse.from(document));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
