package com.assignment.leave.controller;

import com.assignment.leave.dto.ApplyLeaveRequest;
import com.assignment.leave.dto.DecisionRequest;
import com.assignment.leave.model.LeaveBalance;
import com.assignment.leave.model.LeaveRequest;
import com.assignment.leave.model.LeaveStatus;
import com.assignment.leave.security.AuthenticatedUser;
import com.assignment.leave.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {
    private final LeaveService leaveService;

    @GetMapping("/employees/{employeeId}/balances")
    public Collection<LeaveBalance> balances(@PathVariable Long employeeId, @AuthenticationPrincipal AuthenticatedUser actor) {
        return leaveService.getBalances(employeeId, actor);
    }

    @PostMapping("/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequest apply(@RequestBody ApplyLeaveRequest request, @AuthenticationPrincipal AuthenticatedUser actor) {
        return leaveService.apply(request, actor);
    }

    @GetMapping("/manager/requests")
    public List<LeaveRequest> managerRequests(@AuthenticationPrincipal AuthenticatedUser actor,
                                              @RequestParam(required = false) LeaveStatus status,
                                              @RequestParam(required = false) Long employeeId,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return leaveService.managerRequests(actor, status, employeeId, from, to);
    }

    @PatchMapping("/manager/requests/{requestId}/decision")
    public LeaveRequest decide(@PathVariable Long requestId, @RequestBody DecisionRequest decision, @AuthenticationPrincipal AuthenticatedUser actor) {
        return leaveService.decide(requestId, decision, actor);
    }

    @GetMapping("/employees/{employeeId}/history")
    public Page<LeaveRequest> history(@PathVariable Long employeeId, @AuthenticationPrincipal AuthenticatedUser actor,
                                      @RequestParam(required = false) LeaveStatus status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return leaveService.history(employeeId, actor, status, PageRequest.of(page, size));
    }
}
