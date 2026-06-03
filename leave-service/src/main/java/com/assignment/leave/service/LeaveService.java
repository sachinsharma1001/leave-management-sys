package com.assignment.leave.service;

import com.assignment.leave.dto.ApplyLeaveRequest;
import com.assignment.leave.dto.DecisionRequest;
import com.assignment.leave.dto.NotificationEvent;
import com.assignment.leave.model.*;
import com.assignment.leave.repository.InMemoryStore;
import com.assignment.leave.security.AuthenticatedUser;
import com.assignment.leave.utils.MessageUtils;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final InMemoryStore store;
    private final NotificationPublisher publisher;

    public Collection<LeaveBalance> getBalances(Long employeeId, AuthenticatedUser actor) {
        enforceEmployeeOrTeamAccess(employeeId, actor);
        ensureEmployee(employeeId);
        return store.balances(employeeId).values();
    }

    public LeaveRequest apply(ApplyLeaveRequest request, AuthenticatedUser actor) {
        if (actor.getRole() != Role.EMPLOYEE || !Objects.equals(actor.getUserId(), request.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MessageUtils.EMPLOYEE_CAN_APPLY_THEMSELVES);
        }

        Employee employee = ensureEmployee(request.getEmployeeId());
        if (!Objects.equals(employee.getManagerId(), request.getReportingManagerId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageUtils.INVALID_REPORTING_MANAGER);
        }

        validateDates(request.getStartDate(), request.getEndDate(), request.getNumberOfDays());
        LeaveBalance balance = Optional.ofNullable(store.balances(request.getEmployeeId()).get(request.getLeaveType()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageUtils.INVALID_LEAVE_TYPE));

        if (balance.getRemaining() < request.getNumberOfDays()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageUtils.INSUFFICIENT_LEAVE_BALANCE);
        }
        boolean overlaps = store.requests().stream().anyMatch(r -> Objects.equals(r.getEmployeeId(), request.getEmployeeId()) && r.getStatus() != LeaveStatus.REJECTED && r.getStatus() != LeaveStatus.CANCELLED && overlaps(r.getStartDate(), r.getEndDate(), request.getStartDate(), request.getEndDate()));
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Overlapping leave request already exists");
        }
        LeaveRequest saved = store.saveRequest(setLeaveRequestData(request));

        publisher.publish(buildLeaveSubmittedNotificationEvent(request, saved));
        publisher.publish(buildLeaveSubmittedPendingApprovalNotificationEvent(request, saved));
        return saved;
    }

    private NotificationEvent buildLeaveSubmittedNotificationEvent(ApplyLeaveRequest request, LeaveRequest saved) {
        return NotificationEvent.builder()
                .userId(request.getEmployeeId())
                .title("Leave submitted")
                .message("Your leave request #" + saved.getId() + " is pending approval")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private NotificationEvent buildLeaveSubmittedPendingApprovalNotificationEvent(ApplyLeaveRequest request, LeaveRequest saved) {
        return NotificationEvent.builder()
                .userId(request.getEmployeeId())
                .title("Leave approval pending")
                .message("Leave request #" + saved.getId() + " requires your action")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private LeaveRequest setLeaveRequestData(ApplyLeaveRequest request) {
        return LeaveRequest.builder()
                .employeeId(request.getEmployeeId())
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .numberOfDays(request.getNumberOfDays())
                .reason(request.getReason())
                .reportingManagerId(request.getReportingManagerId())
                .build();
    }

    public List<LeaveRequest> managerRequests(AuthenticatedUser actor, LeaveStatus status, Long employeeId, LocalDate from, LocalDate to) {
        if (actor.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MessageUtils.ONLY_MANAGER_CAN_VIEW);
        }
        return store.requests().stream()
                .filter(r -> Objects.equals(r.getReportingManagerId(), actor.getUserId()))
                .filter(r -> status == null || r.getStatus() == status)
                .filter(r -> employeeId == null || Objects.equals(r.getEmployeeId(), employeeId))
                .filter(r -> from == null || !r.getStartDate().isBefore(from))
                .filter(r -> to == null || !r.getEndDate().isAfter(to))
                .sorted(Comparator.comparing(LeaveRequest::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public LeaveRequest decide(Long requestId, DecisionRequest decision, AuthenticatedUser actor) {
        if (actor.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MessageUtils.ONLY_MANAGER_CAN_APPROVE_OR_REJECT_LEAVES);
        }
        LeaveRequest leave = store.request(requestId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MessageUtils.LEAVE_REQUEST_NOT_FOUND));

        if (!Objects.equals(leave.getReportingManagerId(), actor.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MessageUtils.REQUEST_DOES_NOT_BELONG_TO_YOUR_TEAM);
        }
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MessageUtils.NO_PENDING_REQUEST_FOUND);
        }
        String normalized = decision.getDecision() == null ? "" : decision.getDecision().trim().toUpperCase();
        if ("APPROVED".equals(normalized)) {
            return updateApprovedLeavesCount(decision, leave);
        }
        if ("REJECTED".equals(normalized)) {
            return updateRejectLeaveCount(decision, leave);
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageUtils.INVALID_DECISION_TYPE);
    }

    private LeaveRequest updateRejectLeaveCount(DecisionRequest decision, LeaveRequest leave) {
        if (decision.getComments() == null || decision.getComments().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageUtils.REJECTION_COMMENTS_MANDATORY);
        }
        leave.reject(decision.getComments());
        publisher.publish(buildRejectLeaveNotificationEvent(decision, leave));
        return leave;
    }

    private NotificationEvent buildRejectLeaveNotificationEvent(DecisionRequest decision, LeaveRequest leave) {
        return NotificationEvent.builder()
                .userId(leave.getEmployeeId())
                .title("Leave rejected")
                .message("Your leave request #" + leave.getId() + " was rejected. Reason: " + decision.getComments())
                .createdAt(OffsetDateTime.now())
                .build();
    }

    private LeaveRequest updateApprovedLeavesCount(DecisionRequest decision, LeaveRequest leave) {
        LeaveBalance balance = store.balances(leave.getEmployeeId()).get(leave.getLeaveType());
        if (balance.getRemaining() < leave.getNumberOfDays()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageUtils.INSUFFICIENT_LEAVE_BALANCE_APPROVAL);
        }
        balance.deduct(leave.getNumberOfDays());
        leave.approve(decision.getComments());
        publisher.publish(buildApprovedLeaveNotificationEvent(decision, leave));
        return leave;
    }

    private NotificationEvent buildApprovedLeaveNotificationEvent(DecisionRequest decision, LeaveRequest leave) {
        return NotificationEvent.builder()
                .userId(leave.getEmployeeId())
                .title("Leave approved")
                .message("Your leave request #" + leave.getId() + " has been approved")
                .createdAt(OffsetDateTime.now())
                .build();
    }

    public Page<LeaveRequest> history(Long employeeId, AuthenticatedUser actor, LeaveStatus status, Pageable pageable) {
        enforceEmployeeOrTeamAccess(employeeId, actor);
        List<LeaveRequest> filtered = store.requests().stream()
                .filter(r -> Objects.equals(r.getEmployeeId(), employeeId))
                .filter(r -> status == null || r.getStatus() == status)
                .sorted(Comparator.comparing(LeaveRequest::getCreatedAt).reversed())
                .toList();
        int start = Math.min((int) pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    private Employee ensureEmployee(Long employeeId) {
        return store.employee(employeeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, MessageUtils.EMPLOYEE_NOT_FOUND));
    }

    private void enforceEmployeeOrTeamAccess(Long employeeId, AuthenticatedUser actor) {
        if (actor.getRole() == Role.EMPLOYEE && !Objects.equals(actor.getUserId(), employeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, MessageUtils.EMPLOYEE_CAN_ACCESS_THEIR_DATA);
        }

        if (actor.getRole() == Role.MANAGER) {
            Employee employee = ensureEmployee(employeeId);
            if (!Objects.equals(employee.getManagerId(), actor.getUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, MessageUtils.MANAGER_CAN_ACCESS_TEAM_MEMBER_DATA_ONLY);
            }
        }
    }

    private void validateDates(LocalDate start, LocalDate end, double days) {
        if (start == null || end == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageUtils.START_DATE_AND_END_DATE_MANDATORY);
        }
        if (start.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageUtils.PAST_DATES_NOT_ALLOWED);
        }
        if (start.isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageUtils.START_DATE_MUST_BEFORE_EQUAL_TO_END_DATE);
        }
        if (days <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, MessageUtils.NUMBERS_MUST_BE_POSITIVE);
        }
    }

    private boolean overlaps(LocalDate aStart, LocalDate aEnd, LocalDate bStart, LocalDate bEnd) {
        return !aEnd.isBefore(bStart) && !bEnd.isBefore(aStart);
    }
}
