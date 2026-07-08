package com.assignment.leave.controller;

import com.assignment.leave.dto.LeaveReviewRequest;
import com.assignment.leave.dto.LeaveReviewResponse;
import com.assignment.leave.security.AuthenticatedUser;
import com.assignment.leave.service.LeaveReviewAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/leave-requests")
@RequiredArgsConstructor
public class AiLeaveReviewController {
    private final LeaveReviewAgentService leaveReviewAgentService;

    @PostMapping("/{requestId}/review")
    public LeaveReviewResponse review(@PathVariable Long requestId,
                                      @RequestBody(required = false) LeaveReviewRequest request,
                                      @AuthenticationPrincipal AuthenticatedUser actor) {
        return leaveReviewAgentService.review(requestId, request, actor);
    }
}
