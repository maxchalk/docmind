package com.docmind.repository;

import com.docmind.model.Document;
import com.docmind.model.QueryHistory;
import com.docmind.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueryHistoryRepository extends JpaRepository<QueryHistory, Long> {
    List<QueryHistory> findByUserOrderByAskedAtDesc(User user);
    List<QueryHistory> findByDocumentOrderByAskedAtDesc(Document document);
}
