package com.clouddocs.processing.infrastructure;

import com.clouddocs.processing.application.ProcessingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@ConditionalOnProperty(name = "clouddocs.processing.message-source", havingValue = "sqs")
public class SqsProcessingPoller implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(SqsProcessingPoller.class);
    private final ProcessingWorker worker;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "clouddocs-sqs-processing-poller");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean running;

    public SqsProcessingPoller(ProcessingWorker worker) {
        this.worker = worker;
    }

    @Override
    public void start() {
        if (running) return;
        running = true;
        executor.submit(this::poll);
    }

    private void poll() {
        while (running) {
            try {
                worker.processNext();
            } catch (Exception exception) {
                log.error("Processing queue poll or message handling failed; message was not acknowledged", exception);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    @Override
    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public boolean isAutoStartup() { return true; }
}
