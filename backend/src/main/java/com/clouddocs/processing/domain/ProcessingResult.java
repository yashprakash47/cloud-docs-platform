package com.clouddocs.processing.domain;

public record ProcessingResult(long sizeBytes, String sha256, boolean checksumVerified) {
}
