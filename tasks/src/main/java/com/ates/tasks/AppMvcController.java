package com.ates.tasks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

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
}
