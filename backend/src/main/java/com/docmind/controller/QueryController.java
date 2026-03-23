package com.docmind.controller;

import com.docmind.dto.QueryRequest;
import com.docmind.dto.QueryResponse;
import com.docmind.model.QueryHistory;
import com.docmind.model.User;
import com.docmind.service.QueryService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@RequestBody QueryRequest request,
                                         @AuthenticationPrincipal User user) {
        try {
            QueryResponse response = queryService.askQuestion(
                    request.getDocumentId(),
                    request.getQuestion(),
                    user
            );
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<QueryHistory>> getHistory(@AuthenticationPrincipal User user) {
        List<QueryHistory> history = queryService.getHistory(user);
        return ResponseEntity.ok(history);
    }
}
