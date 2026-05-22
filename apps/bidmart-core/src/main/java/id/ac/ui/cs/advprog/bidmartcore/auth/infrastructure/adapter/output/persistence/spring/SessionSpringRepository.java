package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.adapter.output.persistence.spring;

import id.ac.ui.cs.advprog.bidmartcore.auth.domain.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionSpringRepository extends JpaRepository<Session, String> {

    @Query("SELECT s FROM Session s WHERE s.user.id = :userId")
    List<Session> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(s) FROM Session s WHERE s.user.id = :userId AND s.isActive = true")
    int countActiveByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.user.id = :userId AND s.id <> :currentSessionId")
    void deactivateAllByUserIdExcept(@Param("userId") UUID userId, @Param("currentSessionId") String currentSessionId);

    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.user.id = :userId")
    void deactivateAllByUserId(@Param("userId") UUID userId);
}

