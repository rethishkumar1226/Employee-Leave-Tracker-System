package com.leavemanager.service;

import com.leavemanager.dto.EmployeeCreateRequest;
import com.leavemanager.dto.EmployeeUpdateRequest;
import com.leavemanager.entity.*;
import com.leavemanager.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * INTERVIEW NOTE:
 * This service controls administrator operations: managing employees (CRUD), departments,
 * leave policies, and generating analytics reports (leave usage, department stats, defaulters).
 *
 * Core Interview Points:
 * 1. Soft-Delete Pattern: In `deleteEmployee`, we do not delete the user record (which would break
 *    foreign key constraints on historic leave requests/balances). Instead, we mark `isActive(false)`.
 * 2. Cascading Updates: In `updateLeaveType`, if the default policy allocation changes, we
 *    automatically adjust all existing active employees' balances by recalculating remaining days.
 * 3. Reporting Stats: Database statistical queries group data directly inside MySQL (e.g. SUM/GROUP BY)
 *    to minimize memory overhead on the application server.
 * 4. Simplification: Complex Java Stream mappings are replaced with imperative loops for clear whiteboard explanation.
 */
@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // --- Employee Management ---

    public List<User> getAllEmployees() {
        return userRepository.findAll();
    }

    /**
     * Creates a new employee, hashes their password, and allocates default balances.
     */
    @Transactional
    public User createEmployee(EmployeeCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Department dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        }

        User mgr = null;
        if (request.getManagerId() != null) {
            mgr = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new IllegalArgumentException("Manager not found"));
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .role(request.getRole())
                .department(dept)
                .manager(mgr)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        // Auto-initialize leave balances for all existing leave types in the database
        List<LeaveType> types = leaveTypeRepository.findAll();
        for (LeaveType type : types) {
            LeaveBalance balance = LeaveBalance.builder()
                    .user(savedUser)
                    .leaveType(type)
                    .allocatedDays(type.getDefaultDays())
                    .usedDays(0)
                    .remainingDays(type.getDefaultDays())
                    .build();
            leaveBalanceRepository.save(balance);
        }

        return savedUser;
    }

    /**
     * Updates employee details. Ensures the manager isn't the employee themselves.
     */
    @Transactional
    public User updateEmployee(Long employeeId, EmployeeUpdateRequest request) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        // Verify email uniqueness if it was changed
        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Department dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        }

        User mgr = null;
        if (request.getManagerId() != null) {
            if (request.getManagerId().equals(employeeId)) {
                throw new IllegalArgumentException("An employee cannot report to themselves");
            }
            mgr = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new IllegalArgumentException("Manager not found"));
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setDepartment(dept);
        user.setManager(mgr);
        user.setActive(request.isActive());

        return userRepository.save(user);
    }

    /**
     * Soft-deletes an employee by setting active status to false.
     * Prevents relational integrity breaks on historic leave records.
     */
    @Transactional
    public void deleteEmployee(Long employeeId) {
        User user = userRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        user.setActive(false); // Soft delete
        userRepository.save(user);
    }

    // --- Department Management ---

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Transactional
    public Department createDepartment(Department department) {
        if (departmentRepository.existsByName(department.getName())) {
            throw new IllegalArgumentException("Department name already exists");
        }
        return departmentRepository.save(department);
    }

    @Transactional
    public Department updateDepartment(Long id, Department details) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        if (!dept.getName().equalsIgnoreCase(details.getName()) && departmentRepository.existsByName(details.getName())) {
            throw new IllegalArgumentException("Department name already exists");
        }
        dept.setName(details.getName());
        dept.setDescription(details.getDescription());
        return departmentRepository.save(dept);
    }

    /**
     * Deletes a department and handles foreign key references safely by unlinking users.
     */
    @Transactional
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new IllegalArgumentException("Department not found");
        }
        // Safely unlink users assigned to this department
        List<User> users = userRepository.findByDepartmentId(id);
        for (User u : users) {
            u.setDepartment(null);
            userRepository.save(u);
        }
        departmentRepository.deleteById(id);
    }

    // --- Leave Type Management ---

    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    /**
     * Creates a new leave type and auto-allocates a balance for all existing active users.
     */
    @Transactional
    public LeaveType createLeaveType(LeaveType leaveType) {
        if (leaveTypeRepository.existsByName(leaveType.getName())) {
            throw new IllegalArgumentException("Leave type name already exists");
        }
        LeaveType savedType = leaveTypeRepository.save(leaveType);

        // Auto-seed balance entries for existing active employees
        List<User> employees = userRepository.findAll();
        for (User emp : employees) {
            LeaveBalance balance = LeaveBalance.builder()
                    .user(emp)
                    .leaveType(savedType)
                    .allocatedDays(savedType.getDefaultDays())
                    .usedDays(0)
                    .remainingDays(savedType.getDefaultDays())
                    .build();
            leaveBalanceRepository.save(balance);
        }

        return savedType;
    }

    /**
     * Updates leave type policy. Adjusts remaining balances for users if allocation changes.
     */
    @Transactional
    public LeaveType updateLeaveType(Long id, LeaveType details) {
        LeaveType type = leaveTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));
        
        if (!type.getName().equalsIgnoreCase(details.getName()) && leaveTypeRepository.existsByName(details.getName())) {
            throw new IllegalArgumentException("Leave type name already exists");
        }
        
        int oldDefault = type.getDefaultDays();
        type.setName(details.getName());
        type.setDefaultDays(details.getDefaultDays());
        type.setDescription(details.getDescription());

        LeaveType updatedType = leaveTypeRepository.save(type);

        // Adjust all active employee balances if default days changed
        if (oldDefault != details.getDefaultDays()) {
            List<LeaveBalance> balances = leaveBalanceRepository.findByLeaveTypeId(id);
            for (LeaveBalance bal : balances) {
                int diff = details.getDefaultDays() - oldDefault;
                bal.setAllocatedDays(details.getDefaultDays());
                bal.setRemainingDays(Math.max(0, bal.getRemainingDays() + diff));
                leaveBalanceRepository.save(bal);
            }
        }

        return updatedType;
    }

    /**
     * Deletes leave type and deletes all related user balance records.
     */
    @Transactional
    public void deleteLeaveType(Long id) {
        if (!leaveTypeRepository.existsById(id)) {
            throw new IllegalArgumentException("Leave type not found");
        }
        List<LeaveBalance> balances = leaveBalanceRepository.findByLeaveTypeId(id);
        leaveBalanceRepository.deleteAll(balances);
        leaveTypeRepository.deleteById(id);
    }

    // --- Reports ---

    /**
     * Query leave types statistics for Recharts Pie Chart.
     */
    public List<Map<String, Object>> getLeaveUsageReport() {
        List<Object[]> results = leaveRequestRepository.getLeaveTypeUsageStats();
        List<Map<String, Object>> report = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", row[0]);
            map.put("days", row[1]);
            report.add(map);
        }
        return report;
    }

    /**
     * Query department leave metrics for Recharts Bar Chart.
     */
    public List<Map<String, Object>> getDepartmentStatsReport() {
        List<Object[]> results = leaveRequestRepository.getDepartmentLeaveUsageStats();
        List<Map<String, Object>> report = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("department", row[0] != null ? row[0] : "Unassigned");
            map.put("days", row[1]);
            report.add(map);
        }
        return report;
    }

    /**
     * Generates a report of defaulter employees (exceeded 15 leave days or remaining balance <= 0).
     * Replaced Java Streams with simple for-loops for interview code readability.
     */
    public List<Map<String, Object>> getDefaultersReport() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> report = new ArrayList<>();

        for (User user : users) {
            if (!user.isActive()) continue; // Skip soft-deleted employees

            List<LeaveBalance> balances = leaveBalanceRepository.findByUserId(user.getId());
            
            // Loop calculation is easier to whiteboard than stream sum
            int totalUsed = 0;
            for (LeaveBalance bal : balances) {
                totalUsed += bal.getUsedDays();
            }
            
            boolean isDefaulter = false;
            String reason = "";

            if (totalUsed > 15) {
                isDefaulter = true;
                reason = "Exceeded 15 total leave days (" + totalUsed + " used)";
            } else {
                for (LeaveBalance bal : balances) {
                    if (bal.getRemainingDays() <= 0) {
                        isDefaulter = true;
                        reason = "No remaining days left for: " + bal.getLeaveType().getName();
                        break;
                    }
                }
            }

            if (isDefaulter) {
                Map<String, Object> map = new HashMap<>();
                map.put("employeeId", user.getId());
                map.put("employeeName", user.getFirstName() + " " + user.getLastName());
                map.put("username", user.getUsername());
                map.put("department", user.getDepartment() != null ? user.getDepartment().getName() : "Unassigned");
                map.put("usedDays", totalUsed);
                map.put("reason", reason);
                report.add(map);
            }
        }

        return report;
    }
}
