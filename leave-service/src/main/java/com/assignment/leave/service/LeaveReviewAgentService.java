package com.assignment.leave.service;

import com.assignment.leave.dto.AiConfidence;
import com.assignment.leave.dto.AiRecommendation;
import com.assignment.leave.dto.LeaveReviewRequest;
import com.assignment.leave.dto.LeaveReviewResponse;
import com.assignment.leave.model.Employee;
import com.assignment.leave.model.LeaveBalance;
import com.assignment.leave.model.LeaveRequest;
import com.assignment.leave.model.LeaveStatus;
import com.assignment.leave.model.Role;
import com.assignment.leave.repository.InMemoryStore;
import com.assignment.leave.security.AuthenticatedUser;
import com.assignment.leave.utils.MessageUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LeaveReviewAgentService {
    private final InMemoryStore store;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    public LeaveReviewResponse review(Long requestId, LeaveReviewRequest request, AuthenticatedUser actor) {
        if (actor == null || actor.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MessageUtils.ONLY_MANAGER_CAN_VIEW);
        }

        LeaveRequest leave = store.request(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MessageUtils.LEAVE_REQUEST_NOT_FOUND));
        if (!Objects.equals(leave.getReportingManagerId(), actor.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MessageUtils.REQUEST_DOES_NOT_BELONG_TO_YOUR_TEAM);
        }

        String aiOutput = openAiClient.createTextResponse(systemPrompt(), userPrompt(leave, request));
        AiReviewPayload payload = parse(aiOutput);

        return new LeaveReviewResponse(
                payload.recommendation(),
                payload.confidence(),
                payload.reasoningMarkdown(),
                payload.suggestedManagerComment(),
                openAiClient.model()
        );
    }

    private String systemPrompt() {
        return """
                You are a leave request review assistant for managers.
                You do not approve or reject leave requests directly.
                Return only valid JSON with these exact fields:
                {
                  "recommendation": "APPROVE" | "REJECT" | "NEEDS_MORE_INFO",
                  "confidence": "LOW" | "MEDIUM" | "HIGH",
                  "reasoningMarkdown": "Markdown explanation with short sections and bullets",
                  "suggestedManagerComment": "Short comment a manager can use"
                }
                Base the recommendation only on the supplied leave data.
                Consider leave balance, request status, overlapping team leaves, employee history, duration, reason, and manager context.
                Do not invent policy facts or employee details.
                """;
    }

    private String userPrompt(LeaveRequest leave, LeaveReviewRequest request) {
        Employee employee = store.employee(leave.getEmployeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MessageUtils.EMPLOYEE_NOT_FOUND));
        LeaveBalance requestedBalance = store.balances(leave.getEmployeeId()).get(leave.getLeaveType());
        List<LeaveRequest> employeeHistory = employeeHistory(leave);
        List<LeaveRequest> overlappingTeamLeaves = overlappingTeamLeaves(leave);

        StringBuilder prompt = new StringBuilder();
        prompt.append("Review this pending leave request and recommend the manager's next action.\n\n");
        appendLeaveRequest(prompt, "Request under review", leave);
        prompt.append("\nEmployee:\n")
                .append("- id: ").append(employee.getId()).append('\n')
                .append("- name: ").append(employee.getName()).append('\n')
                .append("- managerId: ").append(employee.getManagerId()).append('\n');

        prompt.append("\nRequested leave type balance:\n");
        if (requestedBalance == null) {
            prompt.append("- No balance found for requested leave type\n");
        } else {
            prompt.append("- leaveType: ").append(requestedBalance.getLeaveType()).append('\n')
                    .append("- totalAllocated: ").append(requestedBalance.getTotalAllocated()).append('\n')
                    .append("- used: ").append(requestedBalance.getUsed()).append('\n')
                    .append("- remaining: ").append(requestedBalance.getRemaining()).append('\n');
        }

        prompt.append("\nRecent employee leave history:\n");
        if (employeeHistory.isEmpty()) {
            prompt.append("- None\n");
        } else {
            employeeHistory.forEach(history -> appendLeaveRequest(prompt, "History item", history));
        }

        prompt.append("\nOverlapping team leaves for the same manager:\n");
        if (overlappingTeamLeaves.isEmpty()) {
            prompt.append("- None\n");
        } else {
            overlappingTeamLeaves.forEach(overlap -> appendLeaveRequest(prompt, "Overlapping request", overlap));
        }

        if (request != null && StringUtils.hasText(request.additionalContext())) {
            prompt.append("\nAdditional manager context:\n")
                    .append(request.additionalContext().trim())
                    .append('\n');
        }
        return prompt.toString();
    }

    private List<LeaveRequest> employeeHistory(LeaveRequest leave) {
        return store.requests().stream()
                .filter(request -> Objects.equals(request.getEmployeeId(), leave.getEmployeeId()))
                .filter(request -> !Objects.equals(request.getId(), leave.getId()))
                .sorted(Comparator.comparing(LeaveRequest::getCreatedAt).reversed())
                .limit(5)
                .toList();
    }

    private List<LeaveRequest> overlappingTeamLeaves(LeaveRequest leave) {
        return store.requests().stream()
                .filter(request -> !Objects.equals(request.getId(), leave.getId()))
                .filter(request -> Objects.equals(request.getReportingManagerId(), leave.getReportingManagerId()))
                .filter(request -> request.getStatus() != LeaveStatus.REJECTED && request.getStatus() != LeaveStatus.CANCELLED)
                .filter(request -> overlaps(request.getStartDate(), request.getEndDate(), leave.getStartDate(), leave.getEndDate()))
                .sorted(Comparator.comparing(LeaveRequest::getStartDate))
                .toList();
    }

    private void appendLeaveRequest(StringBuilder prompt, String label, LeaveRequest leave) {
        prompt.append(label).append(":\n")
                .append("- id: ").append(leave.getId()).append('\n')
                .append("- employeeId: ").append(leave.getEmployeeId()).append('\n')
                .append("- leaveType: ").append(leave.getLeaveType()).append('\n')
                .append("- startDate: ").append(leave.getStartDate()).append('\n')
                .append("- endDate: ").append(leave.getEndDate()).append('\n')
                .append("- numberOfDays: ").append(leave.getNumberOfDays()).append('\n')
                .append("- reason: ").append(leave.getReason()).append('\n')
                .append("- reportingManagerId: ").append(leave.getReportingManagerId()).append('\n')
                .append("- status: ").append(leave.getStatus()).append('\n')
                .append("- managerComments: ").append(leave.getManagerComments()).append('\n');
    }

    private boolean overlaps(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        return !aEnd.isBefore(bStart) && !bEnd.isBefore(aStart);
    }

    private AiReviewPayload parse(String aiOutput) {
        try {
            return objectMapper.readValue(stripMarkdownFence(aiOutput), AiReviewPayload.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OpenAI response was not valid leave review JSON", ex);
        }
    }

    private String stripMarkdownFence(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("```json")) {
            return trimmed.substring(7, trimmed.length() - 3).trim();
        }
        if (trimmed.startsWith("```")) {
            return trimmed.substring(3, trimmed.length() - 3).trim();
        }
        return trimmed;
    }

    private record AiReviewPayload(
            AiRecommendation recommendation,
            AiConfidence confidence,
            String reasoningMarkdown,
            String suggestedManagerComment
    ) {
    }
}
