package com.assignment.leave.service;

import com.assignment.leave.dto.SummaryRequest;
import com.assignment.leave.dto.SummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SummaryService {
    private final OpenAiClient openAiClient;

    public SummaryResponse summarize(SummaryRequest request) {
        validate(request);
        String summary = openAiClient.createTextResponse(
                "You summarize user-provided text as clean Markdown. Use headings, bullet points, and a short key takeaways section. Do not invent facts.",
                buildUserPrompt(request)
        );
        return new SummaryResponse(summary, openAiClient.model());
    }

    private void validate(SummaryRequest request) {
        if (request == null || !StringUtils.hasText(request.text())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text is required");
        }
    }

    private String buildUserPrompt(SummaryRequest request) {
        if (StringUtils.hasText(request.focus())) {
            return "Focus: " + request.focus().trim() + "\n\nText:\n" + request.text().trim();
        }
        return "Text:\n" + request.text().trim();
    }
}
