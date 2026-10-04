package com.clouddocs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.clouddocs.processing.infrastructure.SqsProcessingProperties;

@SpringBootApplication
@EnableConfigurationProperties(SqsProcessingProperties.class)
public class CloudDocsApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudDocsApplication.class, args);
    }
}
