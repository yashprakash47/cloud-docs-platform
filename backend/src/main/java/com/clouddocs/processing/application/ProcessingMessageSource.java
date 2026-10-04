package com.clouddocs.processing.application;

import java.util.Optional;

public interface ProcessingMessageSource {
    Optional<ProcessingMessage> receive();
}
