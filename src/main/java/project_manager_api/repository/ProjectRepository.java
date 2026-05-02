package project_manager_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import project_manager_api.entity.Project;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // All projects where user is the owner OR a member
    @Query("""
        SELECT DISTINCT p FROM Project p
        LEFT JOIN p.members m
        WHERE p.owner.id = :userId OR m.user.id = :userId
        ORDER BY p.createdAt DESC
    """)
    List<Project> findAllByUserId(@Param("userId") Long userId);

    List<Project> findByOwnerId(Long ownerId);
}