package com.leavemanager.config;

import com.leavemanager.entity.*;
import com.leavemanager.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Departments
        if (departmentRepository.count() == 0) {
            Department hr = new Department(null, "Human Resources", "Handles recruitment, employee relations, and payroll.");
            Department eng = new Department(null, "Engineering", "Builds and maintains core tech products.");
            Department sales = new Department(null, "Sales", "Drives customer acquisition and business growth.");
            Department mkt = new Department(null, "Marketing", "Manages brand positioning and outreach campaigns.");
            departmentRepository.saveAll(Arrays.asList(hr, eng, sales, mkt));
        }

        // 2. Seed Leave Types
        if (leaveTypeRepository.count() == 0) {
            LeaveType sick = new LeaveType(null, "Sick Leave", 12, "For personal medical reasons and recovery.");
            LeaveType casual = new LeaveType(null, "Casual Leave", 15, "For personal, unplanned short-term needs.");
            LeaveType earned = new LeaveType(null, "Earned Leave", 24, "Vacation days accumulated through service.");
            leaveTypeRepository.saveAll(Arrays.asList(sick, casual, earned));
        }

        // Fetch seeded departments and leave types for users
        Department hrDept = departmentRepository.findByName("Human Resources").orElse(null);
        Department engDept = departmentRepository.findByName("Engineering").orElse(null);
        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();

        // 3. Seed Users
        if (userRepository.count() == 0) {
            // Seed Admin
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("System")
                    .lastName("Admin")
                    .email("admin@leavemanager.com")
                    .role(Role.ADMIN)
                    .department(hrDept)
                    .isActive(true)
                    .build();
            User savedAdmin = userRepository.save(admin);
            seedBalancesForUser(savedAdmin, leaveTypes);

            // Seed Manager
            User manager = User.builder()
                    .username("manager")
                    .password(passwordEncoder.encode("manager123"))
                    .firstName("Sarah")
                    .lastName("Connor")
                    .email("manager@leavemanager.com")
                    .role(Role.MANAGER)
                    .department(engDept)
                    .manager(savedAdmin)
                    .isActive(true)
                    .build();
            User savedManager = userRepository.save(manager);
            seedBalancesForUser(savedManager, leaveTypes);

            // Seed Employee
            User employee = User.builder()
                    .username("employee")
                    .password(passwordEncoder.encode("employee123"))
                    .firstName("John")
                    .lastName("Doe")
                    .email("employee@leavemanager.com")
                    .role(Role.EMPLOYEE)
                    .department(engDept)
                    .manager(savedManager)
                    .isActive(true)
                    .build();
            User savedEmployee = userRepository.save(employee);
            seedBalancesForUser(savedEmployee, leaveTypes);
        }
    }

    private void seedBalancesForUser(User user, List<LeaveType> leaveTypes) {
        for (LeaveType type : leaveTypes) {
            LeaveBalance balance = LeaveBalance.builder()
                    .user(user)
                    .leaveType(type)
                    .allocatedDays(type.getDefaultDays())
                    .usedDays(0)
                    .remainingDays(type.getDefaultDays())
                    .build();
            leaveBalanceRepository.save(balance);
        }
    }
}
