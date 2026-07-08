package com.assignment.leave.dto;

public record LeaveReviewResponse(
        AiRecommendation recommendation,
        AiConfidence confidence,
        String reasoningMarkdown,
        String suggestedManagerComment,
        String model
) {
}
