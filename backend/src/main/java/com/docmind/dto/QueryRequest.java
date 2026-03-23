package com.docmind.dto;

public class QueryRequest {
    private Long documentId;
    private String question;

    public QueryRequest() {}
    public QueryRequest(Long documentId, String question) {
        this.documentId = documentId;
        this.question = question;
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
