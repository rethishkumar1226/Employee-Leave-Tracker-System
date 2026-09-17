package com.leavemanager.service;

import com.leavemanager.dto.LeaveApplyRequest;
import com.leavemanager.entity.*;
import com.leavemanager.repository.LeaveBalanceRepository;
import com.leavemanager.repository.LeaveRequestRepository;
import com.leavemanager.repository.LeaveTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * INTERVIEW NOTE:
 * This service controls the employee leave application lifecycle: applying for leaves,
 * verifying balances, checking constraints (start date not after end date, not in the past),
 * listing balances, and cancelling pending requests.
 *
 * Design choices:
 * - Uses @Transactional on modification operations (applyLeave, cancelPendingRequest) to ensure
 *   data integrity in case of database errors.
 * - Auto-initializes (Self-Heals) leave balances when getting my balances. This prevents
 *   issues when new leave types are added post-user registration.
 */
@Service
public class LeaveService {

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private AuthService authService;

    /**
     * Creates a new leave request after validation.
     */
    @Transactional
    public LeaveRequest applyLeave(LeaveApplyRequest request) {
        User currentUser = authService.getCurrentUser();

        // Rule 1: Start date must not be after end date
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        // Rule 2: Start date must be today or in the future
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        LeaveType leaveType = leaveTypeRepository.findById(request.getLeaveTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Leave type not found"));

        // ChronoUnit.DAYS.between calculates exclusive days (e.g. 20th to 22nd is 2 days).
        // Adding +1 makes it inclusive (3 days total).
        int numberOfDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;

        // Ensure leave balance exists for this user and leave type. If missing, initialize it.
        Optional<LeaveBalance> balanceOpt = leaveBalanceRepository.findByUserIdAndLeaveTypeId(currentUser.getId(), leaveType.getId());
        LeaveBalance balance;
        if (balanceOpt.isPresent()) {
            balance = balanceOpt.get();
        } else {
            // Self-healing block: create standard default balance
            LeaveBalance newBalance = LeaveBalance.builder()
                    .user(currentUser)
                    .leaveType(leaveType)
                    .allocatedDays(leaveType.getDefaultDays())
                    .usedDays(0)
                    .remainingDays(leaveType.getDefaultDays())
                    .build();
            balance = leaveBalanceRepository.save(newBalance);
        }

        // Rule 3: Check remaining balance
        if (balance.getRemainingDays() < numberOfDays) {
            throw new IllegalArgumentException("Insufficient leave balance. Remaining: " 
                    + balance.getRemainingDays() + ", Requested: " + numberOfDays);
        }

        // Construct leave request. Note that status is PENDING.
        // Balance is NOT deducted here; it is deducted when the manager approves.
        LeaveRequest leaveRequest = LeaveRequest.builder()
                .user(currentUser)
                .leaveType(leaveType)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .numberOfDays(numberOfDays)
                .reason(request.getReason())
                .status(LeaveStatus.PENDING)
                .build();

        return leaveRequestRepository.save(leaveRequest);
    }

    /**
     * Returns leave requests submitted by the currently logged-in user.
     */
    public List<LeaveRequest> getMyRequests() {
        User currentUser = authService.getCurrentUser();
        return leaveRequestRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
    }

    /**
     * Gets or seeds leave balances for the logged-in user.
     * Replaces complex streams with readable loops for clear whiteboard explanations.
     */
    @Transactional
    public List<LeaveBalance> getMyBalances() {
        User currentUser = authService.getCurrentUser();
        List<LeaveType> allTypes = leaveTypeRepository.findAll();
        List<LeaveBalance> currentBalances = leaveBalanceRepository.findByUserId(currentUser.getId());

        // Self-healing check: if user has fewer balances than active leave types, seed the missing ones.
        if (currentBalances.size() < allTypes.size()) {
            List<LeaveBalance> updatedBalances = new ArrayList<>();
            
            for (LeaveType type : allTypes) {
                boolean exists = false;
                // Plain loop is easier to explain than Stream.anyMatch
                for (LeaveBalance bal : currentBalances) {
                    if (bal.getLeaveType().getId().equals(type.getId())) {
                        exists = true;
                        break;
                    }
                }
                
                if (!exists) {
                    LeaveBalance newBalance = LeaveBalance.builder()
                            .user(currentUser)
                            .leaveType(type)
                            .allocatedDays(type.getDefaultDays())
                            .usedDays(0)
                            .remainingDays(type.getDefaultDays())
                            .build();
                    updatedBalances.add(leaveBalanceRepository.save(newBalance));
                }
            }
            currentBalances.addAll(updatedBalances);
        }

        return currentBalances;
    }

    /**
     * Cancels a pending request.
     */
    @Transactional
    public LeaveRequest cancelPendingRequest(Long requestId) {
        User currentUser = authService.getCurrentUser();
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        // Security check: Only the request creator can cancel it
        if (!request.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("You do not have permission to cancel this leave request");
        }

        // Only PENDING requests can be cancelled.
        // Once approved or rejected, the request is finalized.
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalArgumentException("Only pending leave requests can be cancelled");
        }

        request.setStatus(LeaveStatus.CANCELLED);
        return leaveRequestRepository.save(request);
    }
}
