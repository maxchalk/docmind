package com.docmind.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String originalName;
    private String fileType;
    private Long fileSizeBytes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    private int chunkCount;
    private LocalDate lastVerifiedDate;
    private String errorMessage;
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
    }

    public Document() {}

    public Document(Long id, String filename, String originalName, String fileType,
                    Long fileSizeBytes, User uploadedBy, DocumentStatus status,
                    int chunkCount, LocalDate lastVerifiedDate, String errorMessage, LocalDateTime uploadedAt) {
        this.id = id;
        this.filename = filename;
        this.originalName = originalName;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedBy = uploadedBy;
        this.status = status;
        this.chunkCount = chunkCount;
        this.lastVerifiedDate = lastVerifiedDate;
        this.errorMessage = errorMessage;
        this.uploadedAt = uploadedAt;
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
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }
    public LocalDate getLastVerifiedDate() { return lastVerifiedDate; }
    public void setLastVerifiedDate(LocalDate lastVerifiedDate) { this.lastVerifiedDate = lastVerifiedDate; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long id;
        private String filename;
        private String originalName;
        private String fileType;
        private Long fileSizeBytes;
        private User uploadedBy;
        private DocumentStatus status;
        private int chunkCount;
        private LocalDate lastVerifiedDate;
        private String errorMessage;
        private LocalDateTime uploadedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder filename(String filename) { this.filename = filename; return this; }
        public Builder originalName(String originalName) { this.originalName = originalName; return this; }
        public Builder fileType(String fileType) { this.fileType = fileType; return this; }
        public Builder fileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; return this; }
        public Builder uploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; return this; }
        public Builder status(DocumentStatus status) { this.status = status; return this; }
        public Builder chunkCount(int chunkCount) { this.chunkCount = chunkCount; return this; }
        public Builder lastVerifiedDate(LocalDate lastVerifiedDate) { this.lastVerifiedDate = lastVerifiedDate; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder uploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; return this; }
        public Document build() {
            return new Document(id, filename, originalName, fileType, fileSizeBytes,
                    uploadedBy, status, chunkCount, lastVerifiedDate, errorMessage, uploadedAt);
        }
    }
}
