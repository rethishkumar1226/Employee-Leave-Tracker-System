package com.leavemanager.controller;

import com.leavemanager.dto.ManagerCommentRequest;
import com.leavemanager.entity.LeaveRequest;
import com.leavemanager.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
public class ManagerController {

    @Autowired
    private ManagerService managerService;

    @GetMapping("/requests")
    public ResponseEntity<List<LeaveRequest>> getTeamRequests() {
        return ResponseEntity.ok(managerService.getTeamRequests());
    }

    @PutMapping("/requests/{id}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ManagerCommentRequest commentRequest) {
        try {
            LeaveRequest request = managerService.approveRequest(id, commentRequest);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error approving request: " + e.getMessage());
        }
    }

    @PutMapping("/requests/{id}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ManagerCommentRequest commentRequest) {
        try {
            LeaveRequest request = managerService.rejectRequest(id, commentRequest);
            return ResponseEntity.ok(request);
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error rejecting request: " + e.getMessage());
        }
    }

    @GetMapping("/team-calendar")
    public ResponseEntity<List<LeaveRequest>> getTeamCalendar() {
        return ResponseEntity.ok(managerService.getTeamCalendar());
    }
}
