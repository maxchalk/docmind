package com.docmind.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "query_history")
public class QueryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String sourcesJson;

    private LocalDateTime askedAt;

    @PrePersist
    protected void onCreate() {
        this.askedAt = LocalDateTime.now();
    }

    public QueryHistory() {}

    public QueryHistory(Long id, User user, Document document, String question, String answer, String sourcesJson, LocalDateTime askedAt) {
        this.id = id;
        this.user = user;
        this.document = document;
        this.question = question;
        this.answer = answer;
        this.sourcesJson = sourcesJson;
        this.askedAt = askedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getSourcesJson() { return sourcesJson; }
    public void setSourcesJson(String sourcesJson) { this.sourcesJson = sourcesJson; }
    public LocalDateTime getAskedAt() { return askedAt; }
    public void setAskedAt(LocalDateTime askedAt) { this.askedAt = askedAt; }

    // Builder
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private Long id;
        private User user;
        private Document document;
        private String question;
        private String answer;
        private String sourcesJson;
        private LocalDateTime askedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder document(Document document) { this.document = document; return this; }
        public Builder question(String question) { this.question = question; return this; }
        public Builder answer(String answer) { this.answer = answer; return this; }
        public Builder sourcesJson(String sourcesJson) { this.sourcesJson = sourcesJson; return this; }
        public Builder askedAt(LocalDateTime askedAt) { this.askedAt = askedAt; return this; }
        public QueryHistory build() {
            return new QueryHistory(id, user, document, question, answer, sourcesJson, askedAt);
        }
    }
}
