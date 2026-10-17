package com.clouddocs.document.application;

import com.clouddocs.document.infrastructure.JdbcDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class DocumentCleanupService {
    private static final Logger log = LoggerFactory.getLogger(DocumentCleanupService.class);

    private final JdbcDocumentRepository repository;
    private final DocumentService documentService;

    public DocumentCleanupService(JdbcDocumentRepository repository, DocumentService documentService) {
        this.repository = repository;
        this.documentService = documentService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTrash() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(30));
        log.info("Running scheduled cleanup for trashed documents deleted before {}", cutoff);
        List<JdbcDocumentRepository.ExpiredTrashItem> expiredItems = repository.findExpiredTrash(cutoff);
        int deletedCount = 0;
        for (JdbcDocumentRepository.ExpiredTrashItem item : expiredItems) {
            try {
                documentService.deletePermanently(item.tenantId(), item.documentId());
                deletedCount++;
            } catch (Exception e) {
                log.error("Failed to permanently delete expired document {}", item.documentId(), e);
            }
        }
        log.info("Scheduled trash cleanup completed. Purged {} expired documents.", deletedCount);
    }
}
