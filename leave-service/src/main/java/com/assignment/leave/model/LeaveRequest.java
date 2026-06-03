package com.assignment.leave.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {
    private Long id;
    private Long employeeId;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private double numberOfDays;
    private String reason;
    private Long reportingManagerId;

    @Builder.Default
    private LeaveStatus status = LeaveStatus.PENDING;

    private String managerComments;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public void approve(String comments) {
        this.status = LeaveStatus.APPROVED;
        this.managerComments = comments;
        this.updatedAt = OffsetDateTime.now();
    }

    public void reject(String comments) {
        this.status = LeaveStatus.REJECTED;
        this.managerComments = comments;
        this.updatedAt = OffsetDateTime.now();
    }
}
