package com.docmind.dto;

import com.docmind.model.Document;
import com.docmind.model.DocumentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DocumentResponse {
    private Long id;
    private String filename;
    private String originalName;
    private String fileType;
    private Long fileSizeBytes;
    private String uploadedByEmail;
    private LocalDateTime uploadedAt;
    private LocalDate lastVerifiedDate;
    private DocumentStatus status;
    private Integer chunkCount;
    private String errorMessage;

    public DocumentResponse() {}

    public DocumentResponse(Long id, String filename, String originalName, String fileType,
                            Long fileSizeBytes, String uploadedByEmail, LocalDateTime uploadedAt,
                            LocalDate lastVerifiedDate, DocumentStatus status, Integer chunkCount, String errorMessage) {
        this.id = id;
        this.filename = filename;
        this.originalName = originalName;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedByEmail = uploadedByEmail;
        this.uploadedAt = uploadedAt;
        this.lastVerifiedDate = lastVerifiedDate;
        this.status = status;
        this.chunkCount = chunkCount;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public String getUploadedByEmail() { return uploadedByEmail; }
    public void setUploadedByEmail(String uploadedByEmail) { this.uploadedByEmail = uploadedByEmail; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public LocalDate getLastVerifiedDate() { return lastVerifiedDate; }
    public void setLastVerifiedDate(LocalDate lastVerifiedDate) { this.lastVerifiedDate = lastVerifiedDate; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long id;
        private String filename;
        private String originalName;
        private String fileType;
        private Long fileSizeBytes;
        private String uploadedByEmail;
        private LocalDateTime uploadedAt;
        private LocalDate lastVerifiedDate;
        private DocumentStatus status;
        private Integer chunkCount;
        private String errorMessage;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder filename(String filename) { this.filename = filename; return this; }
        public Builder originalName(String originalName) { this.originalName = originalName; return this; }
        public Builder fileType(String fileType) { this.fileType = fileType; return this; }
        public Builder fileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; return this; }
        public Builder uploadedByEmail(String uploadedByEmail) { this.uploadedByEmail = uploadedByEmail; return this; }
        public Builder uploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; return this; }
        public Builder lastVerifiedDate(LocalDate lastVerifiedDate) { this.lastVerifiedDate = lastVerifiedDate; return this; }
        public Builder status(DocumentStatus status) { this.status = status; return this; }
        public Builder chunkCount(Integer chunkCount) { this.chunkCount = chunkCount; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public DocumentResponse build() {
            return new DocumentResponse(id, filename, originalName, fileType, fileSizeBytes,
                    uploadedByEmail, uploadedAt, lastVerifiedDate, status, chunkCount, errorMessage);
        }
    }

    public static DocumentResponse from(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .filename(doc.getFilename())
                .originalName(doc.getOriginalName())
                .fileType(doc.getFileType())
                .fileSizeBytes(doc.getFileSizeBytes())
                .uploadedByEmail(doc.getUploadedBy() != null ? doc.getUploadedBy().getEmail() : null)
                .uploadedAt(doc.getUploadedAt())
                .lastVerifiedDate(doc.getLastVerifiedDate())
                .status(doc.getStatus())
                .chunkCount(doc.getChunkCount())
                .errorMessage(doc.getErrorMessage())
                .build();
    }
}
