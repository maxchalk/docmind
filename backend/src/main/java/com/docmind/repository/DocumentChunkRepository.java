package com.docmind.repository;

import com.docmind.model.Document;
import com.docmind.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentOrderByChunkIndex(Document document);
    List<DocumentChunk> findTop5ByDocumentOrderByChunkIndex(Document document);
    void deleteByDocument(Document document);
}
