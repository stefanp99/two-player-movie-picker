package com.andreea.twoplayermoviepicker.repositories;

import com.andreea.twoplayermoviepicker.models.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Integer> {
    @Query(value = "SELECT id FROM sessions WHERE split_part(seed_sequence, ',', 1) = :seed", nativeQuery = true)
    Optional<Integer> findSessionIdByFirstSeed(String seed);


    @Query(value = "SELECT split_part(seed_sequence, ',', -1) FROM sessions WHERE id = :id", nativeQuery = true)
    Optional<String> findLastSeedInSequenceById(Integer id);
}
