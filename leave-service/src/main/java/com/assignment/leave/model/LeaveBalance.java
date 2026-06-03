package com.assignment.leave.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {
    private LeaveType leaveType;
    private double totalAllocated;
    private double used;

    public double getRemaining() {
        return totalAllocated - used;
    }

    public void deduct(double days) {
        this.used += days;
    }
}
