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

    public QueryResponse askQuestion(Long documentId, String question, User user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (document.getStatus() != DocumentStatus.READY) {
            throw new RuntimeException("Document is not ready for queries. Current status: " + document.getStatus());
        }

        // Retrieve the most relevant chunks via PostgreSQL full-text search.
        // Fall back to sequential top-10 if the question yields no ts_rank results
        // (e.g. very short query or all stop-words).
        List<DocumentChunk> chunks;
        try {
            chunks = documentChunkRepository.findRelevantChunks(document.getId(), question, 8);
        } catch (Exception e) {
            log.warn("Full-text search failed ({}), using sequential fallback", e.getMessage());
            chunks = documentChunkRepository.findTop10ByDocumentOrderByChunkIndex(document);
        }
        if (chunks.isEmpty()) {
            log.debug("No FTS results for question, falling back to sequential chunks");
            chunks = documentChunkRepository.findTop10ByDocumentOrderByChunkIndex(document);
        }

        // Build context — clear page markers help the AI cite accurately
        StringBuilder contextBuilder = new StringBuilder();
        for (DocumentChunk chunk : chunks) {
            contextBuilder
                    .append("--- Page ").append(chunk.getPageNumber())
                    .append(", Section ").append(chunk.getChunkIndex() + 1).append(" ---\n")
                    .append(chunk.getContent())
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
        if (groqApiKey == null || groqApiKey.isBlank()) {
            throw new RuntimeException(
                "Groq API key is not configured. Set GROQ_API_KEY in your .env file and restart the server.");
        }
        try {
            String systemMessage = """
                    You are a precise document assistant. Answer questions using ONLY the information in the provided document context.

                    STRICT RULES:
                    1. Always cite the page number(s) when you use information, e.g. "According to Page 2..." or "(Source: Page 3)".
                    2. Format your response using Markdown:
                       - Use **bold** for key terms, policy names, roles, and important values.
                       - Use bullet points (- item) or numbered lists (1. item) for multi-part answers.
                       - Use clear paragraph breaks between sections.
                    3. Be thorough: include ALL relevant details from the context that answer the question — do not omit important conditions or exceptions.
                    4. If the answer spans multiple pages, cite every relevant page.
                    5. If the answer is NOT present in the provided context, respond with exactly: "I could not find this information in the provided document."
                    6. Never add knowledge from outside the provided context.
                    """;


            String userMessage = "Context:\n" + context + "\n\nQuestion: " + question;

            // Build JSON using Jackson ObjectMapper for proper escaping
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", groqModel);
            root.put("max_tokens", 2000);
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
