package com.ates.analytics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/analytics")
public class AppMvcController {
    @Autowired
    AnalyticsLogic analyticsLogic;

    @GetMapping("/today")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String todayStats(Model model) {
        int revenueForToday = analyticsLogic.calculateRevenueForToday();
        model.addAttribute("revenue", revenueForToday);

        analyticsLogic
                .getTopPricedTaskForToday()
                .ifPresent(task -> model.addAttribute("topTask", task.getDescription()));

        return "stats";
    }
}
