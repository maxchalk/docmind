package com.docmind.model;

import jakarta.persistence.*;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(columnDefinition = "TEXT")
    private String content;

    private int chunkIndex;
    private int pageNumber;
    private int wordCount;

    public DocumentChunk() {}

    public DocumentChunk(Long id, Document document, String content, int chunkIndex, int pageNumber, int wordCount) {
        this.id = id;
        this.document = document;
        this.content = content;
        this.chunkIndex = chunkIndex;
        this.pageNumber = pageNumber;
        this.wordCount = wordCount;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long id;
        private Document document;
        private String content;
        private int chunkIndex;
        private int pageNumber;
        private int wordCount;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder document(Document document) { this.document = document; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder chunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public Builder pageNumber(int pageNumber) { this.pageNumber = pageNumber; return this; }
        public Builder wordCount(int wordCount) { this.wordCount = wordCount; return this; }
        public DocumentChunk build() {
            return new DocumentChunk(id, document, content, chunkIndex, pageNumber, wordCount);
        }
    }
}
