package project_manager_api.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project_manager_api.dto.ProjectRequest;
import project_manager_api.dto.response.ProjectResponse;
import project_manager_api.entity.Project;
import project_manager_api.entity.ProjectMember;
import project_manager_api.entity.User;
import project_manager_api.enums.Role;
import project_manager_api.exception.ApiException;
import project_manager_api.repository.ProjectMemberRepository;
import project_manager_api.repository.ProjectRepository;
import project_manager_api.repository.UserRepository;
import project_manager_api.service.ProjectService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Override
    public ProjectResponse createProject(ProjectRequest request, String email) {
        User owner = getUserByEmail(email);

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        Project saved = projectRepository.save(project);

        // Auto-add owner as ADMIN member
        ProjectMember ownerMember = ProjectMember.builder()
                .project(saved)
                .user(owner)
                .role(Role.ADMIN)
                .build();
        memberRepository.save(ownerMember);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getMyProjects(String email) {
        User user = getUserByEmail(email);
        return projectRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long projectId, String email) {
        User user = getUserByEmail(email);
        Project project = getProjectOrThrow(projectId);
        checkMemberAccess(project, user);
        return toResponse(project);
    }

    @Override
    public ProjectResponse updateProject(Long projectId, ProjectRequest request, String email) {
        User user = getUserByEmail(email);
        Project project = getProjectOrThrow(projectId);
        checkAdminAccess(project, user);

        project.setName(request.getName());
        project.setDescription(request.getDescription());

        return toResponse(projectRepository.save(project));
    }

    @Override
    public void deleteProject(Long projectId, String email) {
        User user = getUserByEmail(email);
        Project project = getProjectOrThrow(projectId);

        if (!project.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the project owner can delete this project");
        }

        projectRepository.delete(project);
    }

    @Override
    public void addMember(Long projectId, Long userId, Role role, String email) {
        User requestingUser = getUserByEmail(email);
        Project project = getProjectOrThrow(projectId);
        checkAdminAccess(project, requestingUser);

        if (memberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new ApiException("User is already a member of this project");
        }

        User newMember = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found with id: " + userId));

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(newMember)
                .role(role != null ? role : Role.MEMBER)
                .build();

        memberRepository.save(member);
    }

    @Override
    public void removeMember(Long projectId, Long userId, String email) {
        User requestingUser = getUserByEmail(email);
        Project project = getProjectOrThrow(projectId);
        checkAdminAccess(project, requestingUser);

        if (project.getOwner().getId().equals(userId)) {
            throw new ApiException("Cannot remove the project owner");
        }

        memberRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    // --- Helpers ---

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found: " + email));
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException("Project not found with id: " + projectId));
    }

    private void checkMemberAccess(Project project, User user) {
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        boolean isMember = memberRepository.existsByProjectIdAndUserId(project.getId(), user.getId());
        if (!isOwner && !isMember) {
            throw new AccessDeniedException("You are not a member of this project");
        }
    }

    private void checkAdminAccess(Project project, User user) {
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        if (isOwner) return;

        ProjectMember member = memberRepository
                .findByProjectIdAndUserId(project.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("You are not a member of this project"));

        if (member.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only admins can perform this action");
        }
    }

    private ProjectResponse toResponse(Project project) {
        List<ProjectResponse.MemberInfo> members = project.getMembers().stream()
                .map(m -> new ProjectResponse.MemberInfo(
                        m.getUser().getId(),
                        m.getUser().getName(),
                        m.getUser().getEmail(),
                        m.getRole().name()
                ))
                .collect(Collectors.toList());

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .ownerId(project.getOwner().getId())
                .ownerName(project.getOwner().getName())
                .memberCount(project.getMembers().size())
                .taskCount(project.getTasks().size())
                .createdAt(project.getCreatedAt())
                .members(members)
                .build();
    }
}