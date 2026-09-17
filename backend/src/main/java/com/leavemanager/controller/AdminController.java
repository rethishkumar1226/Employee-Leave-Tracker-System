package com.leavemanager.controller;

import com.leavemanager.dto.EmployeeCreateRequest;
import com.leavemanager.dto.EmployeeUpdateRequest;
import com.leavemanager.entity.Department;
import com.leavemanager.entity.LeaveType;
import com.leavemanager.entity.User;
import com.leavemanager.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // --- Employee Management ---

    @GetMapping("/employees")
    public ResponseEntity<List<User>> getAllEmployees() {
        return ResponseEntity.ok(adminService.getAllEmployees());
    }

    @PostMapping("/employees")
    public ResponseEntity<?> createEmployee(@Valid @RequestBody EmployeeCreateRequest request) {
        try {
            User user = adminService.createEmployee(request);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating employee: " + e.getMessage());
        }
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<?> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {
        try {
            User user = adminService.updateEmployee(id, request);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating employee: " + e.getMessage());
        }
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        try {
            adminService.deleteEmployee(id);
            return ResponseEntity.ok("Employee deactivated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deactivating employee: " + e.getMessage());
        }
    }

    // --- Department Management ---

    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(adminService.getAllDepartments());
    }

    @PostMapping("/departments")
    public ResponseEntity<?> createDepartment(@Valid @RequestBody Department department) {
        try {
            Department dept = adminService.createDepartment(department);
            return ResponseEntity.ok(dept);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating department: " + e.getMessage());
        }
    }

    @PutMapping("/departments/{id}")
    public ResponseEntity<?> updateDepartment(@PathVariable Long id, @Valid @RequestBody Department department) {
        try {
            Department dept = adminService.updateDepartment(id, department);
            return ResponseEntity.ok(dept);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating department: " + e.getMessage());
        }
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        try {
            adminService.deleteDepartment(id);
            return ResponseEntity.ok("Department deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting department: " + e.getMessage());
        }
    }

    // --- Leave Type Management ---

    @GetMapping("/leave-types")
    public ResponseEntity<List<LeaveType>> getAllLeaveTypes() {
        return ResponseEntity.ok(adminService.getAllLeaveTypes());
    }

    @PostMapping("/leave-types")
    public ResponseEntity<?> createLeaveType(@Valid @RequestBody LeaveType leaveType) {
        try {
            LeaveType type = adminService.createLeaveType(leaveType);
            return ResponseEntity.ok(type);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error creating leave type: " + e.getMessage());
        }
    }

    @PutMapping("/leave-types/{id}")
    public ResponseEntity<?> updateLeaveType(@PathVariable Long id, @Valid @RequestBody LeaveType leaveType) {
        try {
            LeaveType type = adminService.updateLeaveType(id, leaveType);
            return ResponseEntity.ok(type);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error updating leave type: " + e.getMessage());
        }
    }

    @DeleteMapping("/leave-types/{id}")
    public ResponseEntity<?> deleteLeaveType(@PathVariable Long id) {
        try {
            adminService.deleteLeaveType(id);
            return ResponseEntity.ok("Leave type deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting leave type: " + e.getMessage());
        }
    }

    // --- Reports ---

    @GetMapping("/reports/leave-usage")
    public ResponseEntity<?> getLeaveUsageReport() {
        return ResponseEntity.ok(adminService.getLeaveUsageReport());
    }

    @GetMapping("/reports/department-stats")
    public ResponseEntity<?> getDepartmentStatsReport() {
        return ResponseEntity.ok(adminService.getDepartmentStatsReport());
    }

    @GetMapping("/reports/defaulters")
    public ResponseEntity<?> getDefaultersReport() {
        return ResponseEntity.ok(adminService.getDefaultersReport());
    }
}
