package com.andreea.twoplayermoviepicker.repositories;

import com.andreea.twoplayermoviepicker.models.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {
    @Query(value = "SELECT * FROM sessions WHERE split_part(seed_sequence, ',', 1) = :seed", nativeQuery = true)
    Optional<Session> findByFirstSeed(String seed);
}
