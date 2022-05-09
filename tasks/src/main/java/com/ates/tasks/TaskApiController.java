package com.ates.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api")
public class TaskApiController {
    @Autowired
    TaskRepository taskRepository;
    @Autowired
    ProfileRepository profileRepository;

    private final Random randomInt = new Random();

    @PostMapping("/tasks")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EMPLOYEE')")
    public void createTask(@RequestBody Task task) {
        //TODO make idempotent
        task.setStatus(Task.TaskStatus.OPEN);
        task.setAssigneeId(getRandomEmployeeId());
        taskRepository.save(task);
    }

    private UUID getRandomEmployeeId() {
        List<UUID> employeesIds = StreamSupport
                .stream(profileRepository.findAll().spliterator(), false)
                .filter(profile -> "employee".equals(profile.getRole()))
                .map(Profile::getId)
                .collect(Collectors.toList());
        int employeeIndex = randomInt.nextInt(employeesIds.size());
        return employeesIds.get(employeeIndex);
    }

}