package project_manager_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project_manager_api.entity.Task;
import project_manager_api.enums.TaskStatus;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProjectId(Long projectId);

    List<Task> findByAssigneeId(Long assigneeId);

    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);

    // Overdue tasks (due date passed, not yet DONE)
    @Query("SELECT t FROM Task t WHERE t.dueDate < :today AND t.status != 'DONE'")
    List<Task> findOverdueTasks(@Param("today") LocalDate today);

    // Overdue tasks for a specific user (assignee)
    @Query("SELECT t FROM Task t WHERE t.dueDate < :today AND t.status != 'DONE' AND t.assignee.id = :userId")
    List<Task> findOverdueTasksForUser(@Param("today") LocalDate today, @Param("userId") Long userId);

    // Count tasks grouped by status for a specific user
    @Query("""
        SELECT t.status, COUNT(t) FROM Task t
        WHERE t.assignee.id = :userId
        GROUP BY t.status
    """)
    List<Object[]> countTasksByStatusForUser(@Param("userId") Long userId);

    // All tasks in projects where user is a member
    @Query("""
        SELECT DISTINCT t FROM Task t
        JOIN ProjectMember pm ON pm.project.id = t.project.id
        WHERE pm.user.id = :userId OR t.assignee.id = :userId
    """)
    List<Task> findAllTasksForUser(@Param("userId") Long userId);
}