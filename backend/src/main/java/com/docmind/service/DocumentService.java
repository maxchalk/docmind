package com.docmind.service;

import com.docmind.model.*;
import com.docmind.repository.DocumentChunkRepository;
import com.docmind.repository.DocumentRepository;
import com.docmind.repository.QueryHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.file.AccessDeniedException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final QueryHistoryRepository queryHistoryRepository;
    private final ExecutorService executorService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentChunkRepository documentChunkRepository,
                           QueryHistoryRepository queryHistoryRepository) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.queryHistoryRepository = queryHistoryRepository;
        this.executorService = Executors.newFixedThreadPool(4);
    }

    // ------------------------------------------------------------------
    // Upload — with duplicate detection (filename + content hash)
    // ------------------------------------------------------------------

    public Document uploadDocument(MultipartFile file, User user) {
        String originalName = file.getOriginalFilename();
        String contentType = file.getContentType();

        boolean isPdf = (contentType != null && contentType.equals("application/pdf"))
                || (originalName != null && originalName.toLowerCase().endsWith(".pdf"));
        boolean isDocx = (contentType != null &&
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                || (originalName != null && originalName.toLowerCase().endsWith(".docx"));

        if (!isPdf && !isDocx) {
            throw new RuntimeException("Only PDF and DOCX files are supported");
        }

        // Read bytes early so we can hash before persisting anything
        final byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read uploaded file: " + e.getMessage());
        }

        // ---- Duplicate detection (Issue 1) ----

        // 1a. Content-hash check — catches re-uploads of identical files regardless of name
        String contentHash = computeSha256(fileBytes);
        if (contentHash != null && documentRepository.existsByContentHash(contentHash)) {
            throw new RuntimeException(
                "This document has already been uploaded to the platform (identical content detected). " +
                "Please upload a different or updated version.");
        }

        // 1b. Filename check (case-insensitive, platform-wide)
        if (originalName != null && documentRepository.existsByOriginalNameIgnoreCase(originalName)) {
            throw new RuntimeException(
                "A document named \"" + originalName + "\" already exists on the platform. " +
                "Rename the file or delete the existing one before uploading.");
        }

        // ---- Persist document record immediately (PROCESSING) ----

        String fileType = isPdf ? "PDF" : "DOCX";

        Document document = Document.builder()
                .filename(originalName != null ? originalName : "unnamed." + fileType.toLowerCase())
                .originalName(originalName)
                .fileType(fileType)
                .fileSizeBytes(file.getSize())
                .uploadedBy(user)
                .status(DocumentStatus.PROCESSING)
                .chunkCount(0)
                .contentHash(contentHash)
                .build();

        document = documentRepository.save(document);
        log.info("Document '{}' saved (id={}) — starting async parse", originalName, document.getId());

        // ---- Async parsing ----

        final Document savedDoc = document;
        final boolean finalIsPdf = isPdf;

        executorService.submit(() -> {
            try {
                List<DocumentChunk> chunks = finalIsPdf
                        ? parsePdf(fileBytes, savedDoc)
                        : parseDocx(fileBytes, savedDoc);

                documentChunkRepository.saveAll(chunks);

                savedDoc.setStatus(DocumentStatus.READY);
                savedDoc.setChunkCount(chunks.size());
                documentRepository.save(savedDoc);

                log.info("Document '{}' processed — {} chunks indexed",
                        savedDoc.getOriginalName(), chunks.size());
            } catch (Exception e) {
                log.error("Failed to process document '{}'", savedDoc.getOriginalName(), e);
                savedDoc.setStatus(DocumentStatus.FAILED);
                savedDoc.setErrorMessage("Processing failed: " + e.getMessage());
                documentRepository.save(savedDoc);
            }
        });

        return savedDoc;
    }

    // ------------------------------------------------------------------
    // List documents — ALL roles see ALL documents.
    // Access restriction is enforced at the DELETE / VERIFY action level.
    // ------------------------------------------------------------------

    public List<Document> getDocumentsForUser(User user) {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

    // ------------------------------------------------------------------
    // Delete — Admin/HR can delete any document; Employee only their own.
    // Also removes query history first to satisfy FK constraint.
    // ------------------------------------------------------------------

    @Transactional
    public void deleteDocument(Long id, User user) throws AccessDeniedException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // RBAC: only employees are restricted to their own documents
        if (user.getRole() == Role.EMPLOYEE
                && !document.getUploadedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only delete your own documents");
        }

        // Delete dependent records first (FK constraint order matters)
        queryHistoryRepository.deleteByDocument(document);
        documentChunkRepository.deleteByDocument(document);
        documentRepository.delete(document);

        log.info("Document '{}' (id={}) deleted by {}", document.getOriginalName(), id, user.getEmail());
    }

    // ------------------------------------------------------------------
    // Verify — Admin / HR only
    // ------------------------------------------------------------------

    public Document verifyDocument(Long id, User user) throws AccessDeniedException {
        if (user.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Only ADMIN or HR can verify documents");
        }
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        document.setLastVerifiedDate(LocalDate.now());
        return documentRepository.save(document);
    }

    // ------------------------------------------------------------------
    // Status fetch (used by frontend polling)
    // ------------------------------------------------------------------

    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    // ------------------------------------------------------------------
    // PDF parsing
    // ------------------------------------------------------------------

    private List<DocumentChunk> parsePdf(byte[] fileBytes, Document document) throws Exception {
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        try (PDDocument pdfDoc = Loader.loadPDF(fileBytes)) {
            int totalPages = pdfDoc.getNumberOfPages();
            for (int page = 1; page <= totalPages; page++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(pdfDoc).trim();
                if (pageText.isEmpty()) continue;

                for (String chunkText : splitIntoChunks(pageText, 500)) {
                    chunks.add(DocumentChunk.builder()
                            .document(document)
                            .content(chunkText)
                            .chunkIndex(chunkIndex++)
                            .pageNumber(page)
                            .wordCount(chunkText.split("\\s+").length)
                            .build());
                }
            }
        }
        return chunks;
    }

    // ------------------------------------------------------------------
    // DOCX parsing
    // ------------------------------------------------------------------

    private List<DocumentChunk> parseDocx(byte[] fileBytes, Document document) throws Exception {
        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkIndex = 0;

        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            List<XWPFParagraph> paragraphs = docx.getParagraphs();
            int pageNumber = 1;
            StringBuilder pageText = new StringBuilder();
            int paragraphCount = 0;

            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (text == null || text.trim().isEmpty()) continue;

                pageText.append(text).append("\n");
                paragraphCount++;

                if (paragraphCount >= 40) {
                    String pageContent = pageText.toString().trim();
                    if (!pageContent.isEmpty()) {
                        for (String chunkText : splitIntoChunks(pageContent, 500)) {
                            chunks.add(DocumentChunk.builder()
                                    .document(document).content(chunkText)
                                    .chunkIndex(chunkIndex++).pageNumber(pageNumber)
                                    .wordCount(chunkText.split("\\s+").length).build());
                        }
                    }
                    pageNumber++;
                    pageText = new StringBuilder();
                    paragraphCount = 0;
                }
            }

            // Flush remaining text
            String remaining = pageText.toString().trim();
            if (!remaining.isEmpty()) {
                for (String chunkText : splitIntoChunks(remaining, 500)) {
                    chunks.add(DocumentChunk.builder()
                            .document(document).content(chunkText)
                            .chunkIndex(chunkIndex++).pageNumber(pageNumber)
                            .wordCount(chunkText.split("\\s+").length).build());
                }
            }
        }
        return chunks;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private List<String> splitIntoChunks(String text, int maxWords) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        int count = 0;

        for (String word : words) {
            if (count >= maxWords) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
                count = 0;
            }
            current.append(word).append(" ");
            count++;
        }
        if (!current.isEmpty()) chunks.add(current.toString().trim());
        return chunks;
    }

    /**
     * Computes the SHA-256 hex digest of the given bytes.
     * Returns null if computation unexpectedly fails (content check is then skipped).
     */
    private String computeSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("Could not compute SHA-256 hash: {}", e.getMessage());
            return null;
        }
    }
}
