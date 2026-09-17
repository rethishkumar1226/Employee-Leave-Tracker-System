package com.leavemanager.service;

import com.leavemanager.dto.ManagerCommentRequest;
import com.leavemanager.entity.*;
import com.leavemanager.repository.LeaveBalanceRepository;
import com.leavemanager.repository.LeaveRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * INTERVIEW NOTE:
 * This service controls manager operations: viewing team leave requests, approving requests
 * (with balance deduction), rejecting requests, and viewing the team calendar.
 *
 * Core Interview Points:
 * 1. Balance deduction ONLY occurs on approval (to avoid locking days for requests that might get rejected).
 * 2. @Transactional is crucial here because we perform two database writes in `approveRequest`:
 *    updating the employee's LeaveBalance and updating the LeaveRequest status. Both MUST succeed or fail together.
 * 3. `validateManagerPermission` prevents a manager from maliciously approving/rejecting requests
 *    of employees who do not report directly to them.
 */
@Service
public class ManagerService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private AuthService authService;

    /**
     * Lists all leave requests submitted by the manager's team.
     * If the current user is an Admin, they can view all requests globally.
     */
    public List<LeaveRequest> getTeamRequests() {
        User manager = authService.getCurrentUser();
        if (manager.getRole() == Role.ADMIN) {
            return leaveRequestRepository.findAll();
        }
        return leaveRequestRepository.findByUserManagerIdOrderByCreatedAtDesc(manager.getId());
    }

    /**
     * Approves a pending request and deducts leave days from the user's balance.
     */
    @Transactional
    public LeaveRequest approveRequest(Long requestId, ManagerCommentRequest commentRequest) {
        User manager = authService.getCurrentUser();
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        // 1. Authorize manager: Check if the employee reports to the current manager
        validateManagerPermission(manager, request.getUser());

        // 2. Validate request state
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("Leave request is not in PENDING status");
        }

        // 3. Retrieve the employee's balance for this leave type
        LeaveBalance balance = leaveBalanceRepository.findByUserIdAndLeaveTypeId(
                request.getUser().getId(), request.getLeaveType().getId())
                .orElseThrow(() -> new IllegalArgumentException("Leave balance not found for the user"));

        // 4. Double check balance before updating (defensive programming)
        if (balance.getRemainingDays() < request.getNumberOfDays()) {
            throw new IllegalArgumentException("User has insufficient leave balance");
        }

        // 5. Update and deduct balance
        balance.setUsedDays(balance.getUsedDays() + request.getNumberOfDays());
        balance.setRemainingDays(balance.getRemainingDays() - request.getNumberOfDays());
        leaveBalanceRepository.save(balance);

        // 6. Update request status & add manager comment
        request.setStatus(LeaveStatus.APPROVED);
        if (commentRequest != null && commentRequest.getComment() != null) {
            request.setManagerComment(commentRequest.getComment());
        }
        
        return leaveRequestRepository.save(request);
    }

    /**
     * Rejects a leave request. No balance is deducted.
     */
    @Transactional
    public LeaveRequest rejectRequest(Long requestId, ManagerCommentRequest commentRequest) {
        User manager = authService.getCurrentUser();
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        // 1. Authorize manager
        validateManagerPermission(manager, request.getUser());

        // 2. Validate request state
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("Leave request is not in PENDING status");
        }

        // 3. Update status and add manager comment
        request.setStatus(LeaveStatus.REJECTED);
        if (commentRequest != null && commentRequest.getComment() != null) {
            request.setManagerComment(commentRequest.getComment());
        }

        return leaveRequestRepository.save(request);
    }

    /**
     * Retrieves leave requests for the team calendar.
     */
    public List<LeaveRequest> getTeamCalendar() {
        User manager = authService.getCurrentUser();
        if (manager.getRole() == Role.ADMIN) {
            return leaveRequestRepository.findAll();
        }
        return leaveRequestRepository.findByUserManagerIdOrderByCreatedAtDesc(manager.getId());
    }

    /**
     * Authorization helper: ensures that a manager can only manage their direct reports.
     * Admins are bypassed and allowed to manage any employee's leave.
     */
    private void validateManagerPermission(User manager, User employee) {
        if (manager.getRole() == Role.ADMIN) {
            return; // Admins are superusers
        }
        if (employee.getManager() == null || !Objects.equals(employee.getManager().getId(), manager.getId())) {
            throw new SecurityException("You do not have permission to manage this employee's leave requests");
        }
    }
}
