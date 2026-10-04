package com.clouddocs.processing.application;

import com.clouddocs.processing.domain.ProcessingJob;

public record ProcessingFailedEvent(ProcessingJob job, String reason) {
}
