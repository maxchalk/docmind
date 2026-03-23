package com.docmind.service;

import com.docmind.model.*;
import com.docmind.repository.DocumentChunkRepository;
import com.docmind.repository.DocumentRepository;
import com.docmind.repository.QueryHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private QueryHistoryRepository queryHistoryRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private QueryService queryService;

    private User adminUser;
    private User employeeUser;
    private User otherEmployee;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .role(Role.ADMIN)
                .build();

        employeeUser = User.builder()
                .id(2L)
                .email("employee@test.com")
                .role(Role.EMPLOYEE)
                .build();

        otherEmployee = User.builder()
                .id(3L)
                .email("other@test.com")
                .role(Role.EMPLOYEE)
                .build();

        testDocument = Document.builder()
                .id(1L)
                .originalName("test.pdf")
                .status(DocumentStatus.READY)
                .uploadedBy(employeeUser)
                .build();
    }

    @Test
    void askQuestion_employeeNoAccess_throwsException() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        assertThrows(AccessDeniedException.class,
                () -> queryService.askQuestion(1L, "What is the PTO policy?", otherEmployee));
    }

    @Test
    void askQuestion_documentNotFound_throwsException() {
        when(documentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> queryService.askQuestion(999L, "test question", adminUser));
    }

    @Test
    void askQuestion_documentNotReady_throwsException() {
        Document processingDoc = Document.builder()
                .id(2L)
                .status(DocumentStatus.PROCESSING)
                .uploadedBy(adminUser)
                .build();

        when(documentRepository.findById(2L)).thenReturn(Optional.of(processingDoc));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> queryService.askQuestion(2L, "test question", adminUser));

        assertTrue(exception.getMessage().contains("not ready"));
    }

    @Test
    void getHistory_returnsUserHistory() {
        List<QueryHistory> mockHistory = List.of(
                QueryHistory.builder().id(1L).question("Q1").answer("A1").build(),
                QueryHistory.builder().id(2L).question("Q2").answer("A2").build()
        );

        when(queryHistoryRepository.findByUserOrderByAskedAtDesc(adminUser)).thenReturn(mockHistory);

        List<QueryHistory> result = queryService.getHistory(adminUser);

        assertEquals(2, result.size());
        verify(queryHistoryRepository).findByUserOrderByAskedAtDesc(adminUser);
    }
}
