package com.ates.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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
        task.setStatus(Task.TaskStatus.OPEN);
        task.setAssigneeId(getRandomEmployeeId(getAllEmployeeIds()));
        taskRepository.save(task);
    }

    @PostMapping("/tasks/shuffle")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public void shuffleTasks() {
        List<UUID> allEmployeeIds = getAllEmployeeIds();
        List<Task> updatedTasks = StreamSupport
                .stream(taskRepository.findAll().spliterator(), false)
                .filter(task -> Task.TaskStatus.OPEN == task.getStatus())
                .peek(task -> task.setAssigneeId(getRandomEmployeeId(allEmployeeIds)))
                .collect(Collectors.toList());
        taskRepository.saveAll(updatedTasks);
    }

    @PatchMapping("/tasks/{taskId}")
    @PreAuthorize("hasRole('ROLE_EMPLOYEE')")
    public ResponseEntity<String> patchTask(@PathVariable UUID taskId, @RequestBody Task task, Principal principal) {
        UUID profileId = UUID.fromString(principal.getName());
        Task dbTask = taskRepository.findById(taskId).orElse(null);
        if (dbTask == null || !dbTask.getAssigneeId().equals(profileId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        if (task.getStatus() != null) {
            dbTask.setStatus(task.getStatus());
        }

        taskRepository.save(dbTask);

        return ResponseEntity.ok("");
    }

    private List<UUID> getAllEmployeeIds() {
        return StreamSupport
                .stream(profileRepository.findAll().spliterator(), false)
                .filter(profile -> "employee".equals(profile.getRole()))
                .map(Profile::getId)
                .collect(Collectors.toList());
    }

    private UUID getRandomEmployeeId(List<UUID> employeesIds) {
        int employeeIndex = randomInt.nextInt(employeesIds.size());
        return employeesIds.get(employeeIndex);
    }

}
