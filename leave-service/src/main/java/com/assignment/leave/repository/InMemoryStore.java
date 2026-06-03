package com.assignment.leave.repository;

import com.assignment.leave.model.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryStore {

    private final Map<Long, Employee> employees = new ConcurrentHashMap<>();
    private final Map<Long, Map<LeaveType, LeaveBalance>> balances = new ConcurrentHashMap<>();
    private final Map<Long, LeaveRequest> requests = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @PostConstruct
    void init() {
        createEmployee(new Employee(100L, "Meera Manager", "manager1@company.com", Role.MANAGER, null));
        createEmployee(new Employee(1L, "Amit Employee", "employee1@company.com", Role.EMPLOYEE, 100L));
        createEmployee(new Employee(2L, "Priya Employee", "employee2@company.com", Role.EMPLOYEE, 100L));
    }

    public Employee createEmployee(Employee employee) {
        employees.put(employee.getId(), employee);
        if (employee.getRole() == Role.EMPLOYEE) {
            Map<LeaveType, LeaveBalance> initial = new EnumMap<>(LeaveType.class);
            initial.put(LeaveType.CASUAL, new LeaveBalance(LeaveType.CASUAL, 12, 0));
            initial.put(LeaveType.SICK, new LeaveBalance(LeaveType.SICK, 10, 0));
            initial.put(LeaveType.PRIVILEGE, new LeaveBalance(LeaveType.PRIVILEGE, 15, 0));
            balances.put(employee.getId(), initial);
        }
        return employee;
    }

    public Optional<Employee> employee(Long id) {
        return Optional.ofNullable(employees.get(id));
    }

    public Collection<Employee> employees() {
        return employees.values();
    }

    public Map<LeaveType, LeaveBalance> balances(Long employeeId) {
        return balances.getOrDefault(employeeId, Map.of());
    }

    public LeaveRequest saveRequest(LeaveRequest request) {
        LeaveRequest withId = LeaveRequest.builder()
                .id(sequence.incrementAndGet())
                .employeeId(request.getEmployeeId())
                .leaveType(request.getLeaveType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .numberOfDays(request.getNumberOfDays())
                .reason(request.getReason())
                .reportingManagerId(request.getReportingManagerId())
                .build();
        requests.put(withId.getId(), withId);
        return withId;
    }

    public Optional<LeaveRequest> request(Long id) {
        return Optional.ofNullable(requests.get(id));
    }

    public Collection<LeaveRequest> requests() {
        return requests.values();
    }
}
