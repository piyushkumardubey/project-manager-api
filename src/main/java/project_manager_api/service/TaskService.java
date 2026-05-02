package project_manager_api.service;

import project_manager_api.dto.TaskRequest;
import project_manager_api.dto.response.TaskResponse;
import project_manager_api.enums.TaskStatus;

import java.util.List;
import java.util.Map;

public interface TaskService {
    TaskResponse createTask(TaskRequest request, String email);
    List<TaskResponse> getTasksByProject(Long projectId, String email);
    List<TaskResponse> getMyTasks(String email);
    TaskResponse updateTask(Long taskId, TaskRequest request, String email);
    TaskResponse updateTaskStatus(Long taskId, TaskStatus status, String email);
    void deleteTask(Long taskId, String email);
    List<TaskResponse> getOverdueTasks(String email);
    Map<String, Long> getDashboardStats(String email);
}