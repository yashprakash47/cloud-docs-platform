package com.clouddocs.processing.application;

import com.clouddocs.processing.domain.ProcessingJob;
import com.clouddocs.processing.domain.ProcessingResult;

public record ProcessingCompletedEvent(ProcessingJob job, ProcessingResult result) {
}
