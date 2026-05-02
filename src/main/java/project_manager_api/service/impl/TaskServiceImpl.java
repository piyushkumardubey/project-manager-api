package project_manager_api.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project_manager_api.dto.TaskRequest;
import project_manager_api.dto.response.TaskResponse;
import project_manager_api.entity.Project;
import project_manager_api.entity.Task;
import project_manager_api.entity.User;
import project_manager_api.enums.Role;
import project_manager_api.enums.TaskStatus;
import project_manager_api.exception.ApiException;
import project_manager_api.repository.ProjectMemberRepository;
import project_manager_api.repository.ProjectRepository;
import project_manager_api.repository.TaskRepository;
import project_manager_api.repository.UserRepository;
import project_manager_api.service.TaskService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository memberRepository;

    @Override
    public TaskResponse createTask(TaskRequest request, String email) {
        User user = getUserByEmail(email);
        Project project = getProjectOrThrow(request.getProjectId());

        // Only ADMIN members or project owner can create tasks
        checkAdminOrOwner(project, user);

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO)
                .dueDate(request.getDueDate())
                .project(project)
                .build();

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ApiException("Assignee not found"));
            task.setAssignee(assignee);
        }

        return toResponse(taskRepository.save(task));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId, String email) {
        User user = getUserByEmail(email);
        Project project = getProjectOrThrow(projectId);
        checkMemberAccess(project, user);

        return taskRepository.findByProjectId(projectId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getMyTasks(String email) {
        User user = getUserByEmail(email);
        return taskRepository.findByAssigneeId(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public TaskResponse updateTask(Long taskId, TaskRequest request, String email) {
        User user = getUserByEmail(email);
        Task task = getTaskOrThrow(taskId);
        checkAdminOrOwner(task.getProject(), user);

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setDueDate(request.getDueDate());

        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ApiException("Assignee not found"));
            task.setAssignee(assignee);
        }

        return toResponse(taskRepository.save(task));
    }

    @Override
    public TaskResponse updateTaskStatus(Long taskId, TaskStatus status, String email) {
        User user = getUserByEmail(email);
        Task task = getTaskOrThrow(taskId);

        // Assignee or admin can update status
        boolean isAssignee = task.getAssignee() != null
                && task.getAssignee().getId().equals(user.getId());
        boolean isAdminOrOwner = isAdminOrOwner(task.getProject(), user);

        if (!isAssignee && !isAdminOrOwner) {
            throw new AccessDeniedException("You are not authorized to update this task status");
        }

        task.setStatus(status);
        return toResponse(taskRepository.save(task));
    }

    @Override
    public void deleteTask(Long taskId, String email) {
        User user = getUserByEmail(email);
        Task task = getTaskOrThrow(taskId);
        checkAdminOrOwner(task.getProject(), user);
        taskRepository.delete(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> getOverdueTasks(String email) {
        User user = getUserByEmail(email);
        return taskRepository.findOverdueTasksForUser(LocalDate.now(), user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getDashboardStats(String email) {
        User user = getUserByEmail(email);
        List<Object[]> rawStats = taskRepository.countTasksByStatusForUser(user.getId());

        Map<String, Long> stats = new HashMap<>();
        stats.put("TODO", 0L);
        stats.put("IN_PROGRESS", 0L);
        stats.put("DONE", 0L);

        for (Object[] row : rawStats) {
            TaskStatus status = (TaskStatus) row[0];
            Long count = (Long) row[1];
            stats.put(status.name(), count);
        }

        long overdueCount = taskRepository
                .findOverdueTasksForUser(LocalDate.now(), user.getId()).size();
        stats.put("OVERDUE", overdueCount);

        return stats;
    }

    // --- Helpers ---

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found: " + email));
    }

    private Project getProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ApiException("Project not found: " + id));
    }

    private Task getTaskOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ApiException("Task not found: " + id));
    }

    private void checkMemberAccess(Project project, User user) {
        boolean isOwner = project.getOwner().getId().equals(user.getId());
        boolean isMember = memberRepository.existsByProjectIdAndUserId(project.getId(), user.getId());
        if (!isOwner && !isMember) {
            throw new AccessDeniedException("You are not a member of this project");
        }
    }

    private void checkAdminOrOwner(Project project, User user) {
        if (!isAdminOrOwner(project, user)) {
            throw new AccessDeniedException("Only project admins can perform this action");
        }
    }

    private boolean isAdminOrOwner(Project project, User user) {
        if (project.getOwner().getId().equals(user.getId())) return true;
        return memberRepository.findByProjectIdAndUserId(project.getId(), user.getId())
                .map(m -> m.getRole() == Role.ADMIN)
                .orElse(false);
    }

    private TaskResponse toResponse(Task task) {
        boolean overdue = task.getDueDate() != null
                && task.getDueDate().isBefore(LocalDate.now())
                && task.getStatus() != TaskStatus.DONE;

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .dueDate(task.getDueDate())
                .overdue(overdue)
                .projectId(task.getProject().getId())
                .projectName(task.getProject().getName())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .assigneeName(task.getAssignee() != null ? task.getAssignee().getName() : null)
                .createdAt(task.getCreatedAt())
                .build();
    }
}