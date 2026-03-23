package com.docmind.service;

import com.docmind.model.*;
import com.docmind.repository.DocumentChunkRepository;
import com.docmind.repository.DocumentRepository;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final ExecutorService executorService;

    public DocumentService(DocumentRepository documentRepository, DocumentChunkRepository documentChunkRepository) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.executorService = Executors.newFixedThreadPool(4);
    }

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

        String fileType = isPdf ? "PDF" : "DOCX";

        Document document = Document.builder()
                .filename(originalName != null ? originalName : "unnamed." + fileType.toLowerCase())
                .originalName(originalName)
                .fileType(fileType)
                .fileSizeBytes(file.getSize())
                .uploadedBy(user)
                .status(DocumentStatus.PROCESSING)
                .chunkCount(0)
                .build();

        document = documentRepository.save(document);

        final Document savedDoc = document;
        final byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (Exception e) {
            savedDoc.setStatus(DocumentStatus.FAILED);
            savedDoc.setErrorMessage("Failed to read uploaded file: " + e.getMessage());
            documentRepository.save(savedDoc);
            return savedDoc;
        }

        executorService.submit(() -> {
            try {
                List<DocumentChunk> chunks;
                if (isPdf) {
                    chunks = parsePdf(fileBytes, savedDoc);
                } else {
                    chunks = parseDocx(fileBytes, savedDoc);
                }

                documentChunkRepository.saveAll(chunks);

                savedDoc.setStatus(DocumentStatus.READY);
                savedDoc.setChunkCount(chunks.size());
                documentRepository.save(savedDoc);

                log.info("Document '{}' processed successfully with {} chunks",
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

                List<String> pageChunks = splitIntoChunks(pageText, 500);
                for (String chunkText : pageChunks) {
                    DocumentChunk chunk = DocumentChunk.builder()
                            .document(document)
                            .content(chunkText)
                            .chunkIndex(chunkIndex++)
                            .pageNumber(page)
                            .wordCount(chunkText.split("\\s+").length)
                            .build();
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

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
                        List<String> pageChunks = splitIntoChunks(pageContent, 500);
                        for (String chunkText : pageChunks) {
                            DocumentChunk chunk = DocumentChunk.builder()
                                    .document(document)
                                    .content(chunkText)
                                    .chunkIndex(chunkIndex++)
                                    .pageNumber(pageNumber)
                                    .wordCount(chunkText.split("\\s+").length)
                                    .build();
                            chunks.add(chunk);
                        }
                    }
                    pageNumber++;
                    pageText = new StringBuilder();
                    paragraphCount = 0;
                }
            }

            // Process remaining text
            String remaining = pageText.toString().trim();
            if (!remaining.isEmpty()) {
                List<String> pageChunks = splitIntoChunks(remaining, 500);
                for (String chunkText : pageChunks) {
                    DocumentChunk chunk = DocumentChunk.builder()
                            .document(document)
                            .content(chunkText)
                            .chunkIndex(chunkIndex++)
                            .pageNumber(pageNumber)
                            .wordCount(chunkText.split("\\s+").length)
                            .build();
                    chunks.add(chunk);
                }
            }
        }

        return chunks;
    }

    private List<String> splitIntoChunks(String text, int maxWords) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");

        StringBuilder currentChunk = new StringBuilder();
        int wordCount = 0;

        for (String word : words) {
            if (wordCount >= maxWords) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
                wordCount = 0;
            }
            currentChunk.append(word).append(" ");
            wordCount++;
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    public List<Document> getDocumentsForUser(User user) {
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.HR) {
            return documentRepository.findAllByOrderByUploadedAtDesc();
        }
        return documentRepository.findByUploadedByOrderByUploadedAtDesc(user);
    }

    @Transactional
    public void deleteDocument(Long id, User user) throws AccessDeniedException {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (user.getRole() == Role.EMPLOYEE &&
                !document.getUploadedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only delete your own documents");
        }

        documentChunkRepository.deleteByDocument(document);
        documentRepository.delete(document);
    }

    public Document verifyDocument(Long id, User user) throws AccessDeniedException {
        if (user.getRole() == Role.EMPLOYEE) {
            throw new AccessDeniedException("Only ADMIN or HR can verify documents");
        }

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.setLastVerifiedDate(LocalDate.now());
        return documentRepository.save(document);
    }

    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }
}
