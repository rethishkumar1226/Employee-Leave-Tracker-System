package com.leavemanager.repository;

import com.leavemanager.entity.LeaveRequest;
import com.leavemanager.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<LeaveRequest> findByUserManagerIdOrderByCreatedAtDesc(Long managerId);
    
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.user.manager.id = :managerId AND lr.status = 'APPROVED' AND " +
           "((lr.startDate BETWEEN :start AND :end) OR (lr.endDate BETWEEN :start AND :end) OR " +
           "(lr.startDate <= :start AND lr.endDate >= :end))")
    List<LeaveRequest> findTeamApprovedLeavesInPeriod(
            @Param("managerId") Long managerId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("SELECT lr.leaveType.name, SUM(lr.numberOfDays) FROM LeaveRequest lr WHERE lr.status = 'APPROVED' GROUP BY lr.leaveType.name")
    List<Object[]> getLeaveTypeUsageStats();

    @Query("SELECT lr.user.department.name, SUM(lr.numberOfDays) FROM LeaveRequest lr WHERE lr.status = 'APPROVED' GROUP BY lr.user.department.name")
    List<Object[]> getDepartmentLeaveUsageStats();

    List<LeaveRequest> findByStatus(LeaveStatus status);
}
