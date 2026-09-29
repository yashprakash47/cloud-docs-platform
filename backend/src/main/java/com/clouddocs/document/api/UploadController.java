package com.clouddocs.document.api;

import com.clouddocs.document.application.UploadIntentService;
import com.clouddocs.document.domain.DocumentVersion;
import com.clouddocs.identity.application.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/documents/{documentId}/uploads")
public class UploadController {
    private final AuthenticationService authentication;
    private final UploadIntentService uploads;

    public UploadController(AuthenticationService authentication, UploadIntentService uploads) {
        this.authentication = authentication;
        this.uploads = uploads;
    }

    @PostMapping("/intents")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadIntentService.UploadIntent createIntent(@PathVariable UUID tenantId, @PathVariable UUID documentId,
                                                         @Valid @RequestBody CreateUploadIntentRequest body, HttpServletRequest request) {
        return uploads.create(authentication.requireCurrentUser(request), tenantId, documentId, body.originalFilename(), body.contentType(), body.sizeBytes());
    }

    @RequestMapping(value = "/intents/{intentId}/content", method = {RequestMethod.PUT, RequestMethod.POST})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upload(@PathVariable UUID intentId, @RequestPart("file") MultipartFile file, HttpServletRequest request) {
        uploads.upload(authentication.requireCurrentUser(request), intentId, file);
    }

    @PostMapping("/intents/{intentId}/complete")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentVersion complete(@PathVariable UUID intentId, HttpServletRequest request) {
        return uploads.complete(authentication.requireCurrentUser(request), intentId);
    }

    record CreateUploadIntentRequest(@NotBlank @Size(max = 255) String originalFilename,
                                     @NotBlank @Size(max = 255) String contentType,
                                     @PositiveOrZero long sizeBytes) {}
}
