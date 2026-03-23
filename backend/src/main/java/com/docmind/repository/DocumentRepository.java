package com.docmind.repository;

import com.docmind.model.Document;
import com.docmind.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUploadedByOrderByUploadedAtDesc(User user);
    List<Document> findAllByOrderByUploadedAtDesc();
}
