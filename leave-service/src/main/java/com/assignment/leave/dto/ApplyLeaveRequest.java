package com.assignment.leave.dto;

import com.assignment.leave.model.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplyLeaveRequest {
    private Long employeeId;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private double numberOfDays;
    private String reason;
    private Long reportingManagerId;
}
