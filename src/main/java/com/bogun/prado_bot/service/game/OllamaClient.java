package com.bogun.prado_bot.service.game;

import com.bogun.prado_bot.config.OllamaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class OllamaClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OllamaProperties properties;

    public OllamaClient(ObjectMapper objectMapper, OllamaProperties properties) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String generate(String model, String prompt) throws IOException, InterruptedException {
        var request = new GenerateRequest(model, prompt, false);
        var json = objectMapper.writeValueAsString(request);
        var httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(properties.baseUrl() + "/api/generate"))
                .header("Content-Type", "application/json")
                .timeout(properties.timeout())
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Ollama responded with status " + response.statusCode());
        }
        GenerateResponse payload = objectMapper.readValue(response.body(), GenerateResponse.class);
        return payload.response();
    }

    private record GenerateRequest(String model, String prompt, boolean stream) {}

    private record GenerateResponse(String response) {}
}
