package com.ates.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.StreamHandler;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
@RequestMapping("/tasks")
public class AppMvcController {
    @Autowired
    TaskRepository taskRepository;

    @GetMapping("/assign")
    @PreAuthorize("hasRole('ROLE_ADMIN') or hasRole('ROLE_EMPLOYEE')")
    public String assignTasks(Model model) {

        List<Task> tasks = new ArrayList<>();
        taskRepository
                .findAll()
                .forEach(tasks::add);
        model.addAttribute("tasks", tasks);
        return "assign";
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_EMPLOYEE')")
    public String listTasks(Model model, Principal principal) {
        UUID profileId = UUID.fromString(principal.getName());
        List<Task> tasks = StreamSupport
                .stream(taskRepository.findAll().spliterator(), false)
                .filter(task -> profileId.equals(task.getAssigneeId()))
                .collect(Collectors.toList());
        model.addAttribute("tasks", tasks);

        return "tasks";
    }
}
