package com.clouddocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.clouddocs.processing.infrastructure.SqsProcessingProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(SqsProcessingProperties.class)
public class CloudDocsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudDocsApplication.class, args);
    }
}
