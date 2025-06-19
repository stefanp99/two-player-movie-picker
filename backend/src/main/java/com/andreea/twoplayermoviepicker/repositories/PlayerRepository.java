package com.andreea.twoplayermoviepicker.repositories;

import com.andreea.twoplayermoviepicker.models.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {
    Optional<Player> findByPlayerSessionId(String playerSessionId);
}
