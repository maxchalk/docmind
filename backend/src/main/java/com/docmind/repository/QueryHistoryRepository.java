package com.docmind.repository;

import com.docmind.model.Document;
import com.docmind.model.QueryHistory;
import com.docmind.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
    List<QueryHistory> findByUserOrderByAskedAtDesc(User user);
    List<QueryHistory> findByDocumentOrderByAskedAtDesc(Document document);

    /** Bulk-deletes all query history for a document — must run before deleting the document (FK constraint). */
    @Modifying
    @Transactional
    @Query("DELETE FROM QueryHistory qh WHERE qh.document = :document")
    void deleteByDocument(@Param("document") Document document);
}
