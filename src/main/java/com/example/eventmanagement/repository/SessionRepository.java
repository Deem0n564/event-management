package com.example.eventmanagement.repository;

import com.example.eventmanagement.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByEventId(Long eventId);

    @Query("SELECT DISTINCT s FROM Session s LEFT JOIN FETCH s.speakers LEFT JOIN FETCH s.event")
    List<Session> findAllWithSpeakersAndEvent();

    @EntityGraph(attributePaths = {"speakers", "event"})
    Optional<Session> findById(Long id);

    @Query(value = "SELECT DISTINCT s FROM Session s " +
        "LEFT JOIN FETCH s.event e " +
        "LEFT JOIN FETCH s.speakers sp " +
        "WHERE (:speakerFirstName IS NULL OR LOWER(sp.firstName) LIKE LOWER(CONCAT('%', :speakerFirstName, '%'))) " +
        "AND (:title IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :title, '%')))",
        countQuery = "SELECT COUNT(DISTINCT s) FROM Session s " +
            "LEFT JOIN s.speakers sp " +
            "WHERE (:speakerFirstName IS NULL OR LOWER(sp.firstName) LIKE LOWER(CONCAT('%', :speakerFirstName, '%'))) "
            +            "AND (:title IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', :title, '%')))")
    Page<Session> searchSessions(@Param("speakerFirstName") String speakerFirstName,
                                 @Param("title") String title,
                                 Pageable pageable);

    @Query(value = "SELECT DISTINCT s.* FROM sessions s " +
        "LEFT JOIN session_speaker ss ON s.id = ss.session_id " +
        "LEFT JOIN speakers sp ON ss.speaker_id = sp.id " +
        "WHERE (?1 IS NULL OR LOWER(sp.first_name) LIKE LOWER(CONCAT('%', ?1, '%'))) " +
        "AND (?2 IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', ?2, '%')))",
        countQuery = "SELECT COUNT(DISTINCT s.id) FROM sessions s " +
            "LEFT JOIN session_speaker ss ON s.id = ss.session_id " +
            "LEFT JOIN speakers sp ON ss.speaker_id = sp.id " +
            "WHERE (?1 IS NULL OR LOWER(sp.first_name) LIKE LOWER(CONCAT('%', ?1, '%'))) " +
            "AND (?2 IS NULL OR LOWER(s.title) LIKE LOWER(CONCAT('%', ?2, '%')))",
        nativeQuery = true)
    Page<Session> searchSessionsNative(String speakerFirstName, String title, Pageable pageable);
}
