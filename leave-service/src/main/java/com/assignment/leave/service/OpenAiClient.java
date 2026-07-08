package com.assignment.leave.service;

import com.assignment.leave.config.OpenAiConfig.OpenAiProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class OpenAiClient {
    private final RestClient openAiRestClient;
    private final OpenAiProperties openAiProperties;

    public String model() {
        return openAiProperties.getModel();
    }

    public String createTextResponse(String systemPrompt, String userPrompt) {
        ensureConfigured();

        Map<String, Object> body = Map.of(
                "model", openAiProperties.getModel(),
                "input", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            OpenAiResponse response = openAiRestClient.post()
                    .uri("/v1/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.getApiKey())
                    .body(body)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (request, clientResponse) -> {
                        String message = StreamUtils.copyToString(clientResponse.getBody(), StandardCharsets.UTF_8);
                        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, StringUtils.hasText(message) ? message : "OpenAI request failed");
                    })
                    .body(OpenAiResponse.class);

            String outputText = response == null ? null : response.outputText();
            if (!StringUtils.hasText(outputText)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI response did not include text output");
            }
            return outputText.trim();
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI request failed", ex);
        }
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(openAiProperties.getApiKey())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OpenAI API key is not configured");
        }
    }

    private record OpenAiResponse(List<OpenAiOutput> output) {
        String outputText() {
            if (output == null) {
                return null;
            }
            return output.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> "message".equals(item.type()))
                    .flatMap(item -> item.content() == null ? java.util.stream.Stream.empty() : item.content().stream())
                    .filter(Objects::nonNull)
                    .filter(content -> "output_text".equals(content.type()))
                    .map(OpenAiContent::text)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(null);
        }
    }

    private record OpenAiOutput(String type, List<OpenAiContent> content) {
    }

    private record OpenAiContent(String type, @JsonProperty("text") String text) {
    }
}
