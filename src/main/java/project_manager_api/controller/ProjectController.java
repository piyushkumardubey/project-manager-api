package project_manager_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import project_manager_api.dto.ProjectRequest;
import project_manager_api.dto.response.ProjectResponse;
import project_manager_api.enums.Role;
import project_manager_api.service.ProjectService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // GET /api/projects - Get all projects for current user
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getMyProjects(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getMyProjects(userDetails.getUsername()));
    }

    // GET /api/projects/{id}
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.getProjectById(id, userDetails.getUsername()));
    }

    // POST /api/projects - Create project
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(201)
                .body(projectService.createProject(request, userDetails.getUsername()));
    }

    // PUT /api/projects/{id} - Update project (Admin only)
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(projectService.updateProject(id, request, userDetails.getUsername()));
    }

    // DELETE /api/projects/{id} - Delete project (Owner only)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectService.deleteProject(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // POST /api/projects/{id}/members - Add member (Admin only)
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.valueOf(body.get("userId").toString());
        Role role = body.containsKey("role")
                ? Role.valueOf(body.get("role").toString())
                : Role.MEMBER;

        projectService.addMember(id, userId, role, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    // DELETE /api/projects/{id}/members/{userId} - Remove member (Admin only)
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        projectService.removeMember(id, userId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}