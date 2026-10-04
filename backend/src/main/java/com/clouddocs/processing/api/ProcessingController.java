package com.clouddocs.processing.api;

import com.clouddocs.identity.application.AuthenticationService;
import com.clouddocs.processing.application.ProcessingService;
import com.clouddocs.processing.domain.ProcessingJob;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}")
public class ProcessingController {
    private final AuthenticationService authentication;
    private final ProcessingService processing;

    public ProcessingController(AuthenticationService authentication, ProcessingService processing) {
        this.authentication = authentication;
        this.processing = processing;
    }

    @GetMapping("/processing-jobs/{jobId}")
    public ProcessingJob get(@PathVariable UUID tenantId, @PathVariable UUID jobId, HttpServletRequest request) {
        return processing.get(authentication.requireCurrentUser(request), tenantId, jobId);
    }

    @GetMapping("/documents/{documentId}/processing-jobs")
    public List<ProcessingJob> list(@PathVariable UUID tenantId, @PathVariable UUID documentId, HttpServletRequest request) {
        return processing.listForDocument(authentication.requireCurrentUser(request), tenantId, documentId);
    }

    @PostMapping("/processing-jobs/{jobId}/retry")
    @ResponseStatus(HttpStatus.OK)
    public ProcessingJob retry(@PathVariable UUID tenantId, @PathVariable UUID jobId, HttpServletRequest request) {
        return processing.retry(authentication.requireCurrentUser(request), tenantId, jobId);
    }
}
