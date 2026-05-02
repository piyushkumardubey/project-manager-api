package project_manager_api.service;

import project_manager_api.dto.ProjectRequest;
import project_manager_api.dto.response.ProjectResponse;
import project_manager_api.enums.Role;

import java.util.List;

public interface ProjectService {
    ProjectResponse createProject(ProjectRequest request, String email);
    List<ProjectResponse> getMyProjects(String email);
    ProjectResponse getProjectById(Long projectId, String email);
    ProjectResponse updateProject(Long projectId, ProjectRequest request, String email);
    void deleteProject(Long projectId, String email);
    void addMember(Long projectId, Long userId, Role role, String email);
    void removeMember(Long projectId, Long userId, String email);
}