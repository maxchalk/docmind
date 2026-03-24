package com.docmind.repository;

import com.docmind.model.Document;
import com.docmind.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    List<DocumentChunk> findByDocumentOrderByChunkIndex(Document document);
    List<DocumentChunk> findTop5ByDocumentOrderByChunkIndex(Document document);
    List<DocumentChunk> findTop10ByDocumentOrderByChunkIndex(Document document);

    /** Bulk-deletes all chunks for a document — must run before deleting the document (FK constraint). */
    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentChunk dc WHERE dc.document = :document")
    void deleteByDocument(@Param("document") Document document);

    /**
     * Full-text search using PostgreSQL ts_rank to find the most semantically
     * relevant chunks for a given question. Returns up to maxResults chunks
     * ranked by relevance score descending.
     */
    @Query(value = """
        SELECT * FROM document_chunks
        WHERE document_id = :documentId
          AND content IS NOT NULL
          AND length(content) > 20
          AND to_tsvector('english', content) @@ plainto_tsquery('english', :question)
        ORDER BY ts_rank(to_tsvector('english', content),
                         plainto_tsquery('english', :question)) DESC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<DocumentChunk> findRelevantChunks(
            @Param("documentId") Long documentId,
            @Param("question") String question,
            @Param("maxResults") int maxResults);
}
