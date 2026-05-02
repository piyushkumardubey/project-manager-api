package project_manager_api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import project_manager_api.dto.response.TaskResponse;
import project_manager_api.service.TaskService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TaskService taskService;

    // GET /api/dashboard/stats - Task counts by status
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getDashboardStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.getDashboardStats(userDetails.getUsername()));
    }

    // GET /api/dashboard/overdue - Overdue tasks for dashboard
    @GetMapping("/overdue")
    public ResponseEntity<List<TaskResponse>> getOverdueTasks(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(taskService.getOverdueTasks(userDetails.getUsername()));
    }
}