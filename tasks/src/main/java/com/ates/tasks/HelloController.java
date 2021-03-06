package com.ates.tasks;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/tasks")
public class HelloController {

    @GetMapping("/hello")
    @PreAuthorize("hasRole('ROLE_EMPLOYEE')")
    public String hello(@RequestParam(name = "name", required = false, defaultValue = "xxx") String name, Model model) {
        model.addAttribute("name", name);
        return "hello";
    }
}
