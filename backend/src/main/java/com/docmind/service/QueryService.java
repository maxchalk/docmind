package com.docmind.service;

import com.docmind.dto.QueryResponse;
import com.docmind.model.*;
import com.docmind.repository.DocumentChunkRepository;
import com.docmind.repository.DocumentRepository;
import com.docmind.repository.QueryHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AccessDeniedException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class QueryService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final QueryHistoryRepository queryHistoryRepository;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model}")
    private String groqModel;

    private final HttpClient httpClient;

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    public QueryService(DocumentRepository documentRepository,
                        DocumentChunkRepository documentChunkRepository,
                        QueryHistoryRepository queryHistoryRepository,
                        ObjectMapper objectMapper) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.queryHistoryRepository = queryHistoryRepository;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public QueryResponse askQuestion(Long documentId, String question, User user) throws AccessDeniedException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Check access
        if (user.getRole() == Role.EMPLOYEE &&
                !document.getUploadedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You don't have access to this document");
        }

        if (document.getStatus() != DocumentStatus.READY) {
            throw new RuntimeException("Document is not ready for queries. Current status: " + document.getStatus());
        }

        // Get top 5 chunks
        List<DocumentChunk> chunks = documentChunkRepository.findTop5ByDocumentOrderByChunkIndex(document);

        // Build context
        StringBuilder contextBuilder = new StringBuilder();
        for (DocumentChunk chunk : chunks) {
            contextBuilder.append("Page ").append(chunk.getPageNumber())
                    .append(": ").append(chunk.getContent())
                    .append("\n\n");
        }
        String context = contextBuilder.toString();

        // Check staleness
        boolean isStale = document.getLastVerifiedDate() == null ||
                document.getLastVerifiedDate().isBefore(LocalDate.now().minusDays(180));

        // Call Groq API
        String answer = callGroqApi(context, question);

        // Build sources
        List<QueryResponse.Source> sources = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            String snippet = chunk.getContent();
            if (snippet.length() > 150) {
                snippet = snippet.substring(0, 150);
            }
            sources.add(QueryResponse.Source.builder()
                    .chunkIndex(chunk.getChunkIndex())
                    .pageNumber(chunk.getPageNumber())
                    .snippet(snippet)
                    .build());
        }

        // Save to history
        try {
            String sourcesJson = objectMapper.writeValueAsString(sources);
            QueryHistory history = QueryHistory.builder()
                    .user(user)
                    .document(document)
                    .question(question)
                    .answer(answer)
                    .sourcesJson(sourcesJson)
                    .build();
            queryHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("Failed to save query history", e);
        }

        return QueryResponse.builder()
                .answer(answer)
                .sources(sources)
                .isStale(isStale)
                .documentName(document.getOriginalName())
                .build();
    }

    private String callGroqApi(String context, String question) {
        try {
            String systemMessage = "You are a document assistant. Answer ONLY from the provided document context. " +
                    "Always mention the page number when you cite information. " +
                    "If the answer is not in the context, say 'I could not find this information in the document.' " +
                    "Be concise and professional.";

            String userMessage = "Context:\n" + context + "\n\nQuestion: " + question;

            // Build JSON using Jackson ObjectMapper for proper escaping
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", groqModel);
            root.put("max_tokens", 1000);
            root.put("temperature", 0.1);

            ArrayNode messages = root.putArray("messages");

            ObjectNode sysMsg = messages.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemMessage);

            ObjectNode usrMsg = messages.addObject();
            usrMsg.put("role", "user");
            usrMsg.put("content", userMessage);

            String jsonBody = objectMapper.writeValueAsString(root);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(groqApiUrl))
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Groq API error: {} - {}", response.statusCode(), response.body());
                throw new RuntimeException("AI service returned error: " + response.statusCode());
            }

            JsonNode responseRoot = objectMapper.readTree(response.body());
            return responseRoot.path("choices").get(0).path("message").path("content").asText();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call Groq API", e);
            throw new RuntimeException("Failed to get AI response: " + e.getMessage());
        }
    }

    public List<QueryHistory> getHistory(User user) {
        return queryHistoryRepository.findByUserOrderByAskedAtDesc(user);
    }
}
