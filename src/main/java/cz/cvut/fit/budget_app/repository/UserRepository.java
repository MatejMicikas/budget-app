package cz.cvut.fit.budget_app.repository;

import cz.cvut.fit.budget_app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    long countByRole(User.Role role);

    long countByTeam_Id(Long teamId);
}
