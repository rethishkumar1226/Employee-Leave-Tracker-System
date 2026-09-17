package com.leavemanager.controller;

import com.leavemanager.dto.LeaveApplyRequest;
import com.leavemanager.entity.LeaveBalance;
import com.leavemanager.entity.LeaveRequest;
import com.leavemanager.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(@Valid @RequestBody LeaveApplyRequest request) {
        try {
            LeaveRequest leaveRequest = leaveService.applyLeave(request);
            return ResponseEntity.ok(leaveRequest);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error applying for leave: " + e.getMessage());
        }
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<LeaveRequest>> getMyRequests() {
        return ResponseEntity.ok(leaveService.getMyRequests());
    }

    @GetMapping("/balances")
    public ResponseEntity<List<LeaveBalance>> getMyBalances() {
        return ResponseEntity.ok(leaveService.getMyBalances());
    }

    @PutMapping("/cancel/{id}")
    public ResponseEntity<?> cancelPendingRequest(@PathVariable Long id) {
        try {
            LeaveRequest request = leaveService.cancelPendingRequest(id);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error cancelling request: " + e.getMessage());
        }
    }
}
