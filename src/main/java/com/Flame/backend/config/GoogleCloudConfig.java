package com.Flame.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Registers Google Cloud Storage client as a Spring bean.
 * Reads credentials from the JSON key file path set in application.properties
 * via the GOOGLE_APPLICATION_CREDENTIALS environment variable.
 */
@Configuration
public class GoogleCloudConfig {

    @Value("${google.application.credentials}")
    private String credentialsPath;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        return GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                .createScoped("https://www.googleapis.com/auth/cloud-platform");
    }

    @Bean
    public Storage googleCloudStorage(GoogleCredentials googleCredentials) throws IOException {
        return StorageOptions.newBuilder()
                .setCredentials(googleCredentials)
                .build()
                .getService();
    }
}