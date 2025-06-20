package com.andreea.twoplayermoviepicker.models;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "sessions")

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "seed_sequence")
    private String seedSequenceString;

    @Transient
    public List<String> getSeedSequence() {
        if (seedSequenceString == null || seedSequenceString.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(seedSequenceString.split(",")));
    }

    @Transient
    public void setSeedSequence(List<String> sequence) {
        this.seedSequenceString = String.join(",", sequence);
    }

    public void addToSeedSequence(String value) {
        List<String> current = getSeedSequence();
        current.add(value);
        setSeedSequence(current);
    }

    @Column(name = "all_likes")
    private String allLikesString;

    @Transient
    public Set<String> getAllLikes() {
        if (allLikesString == null || allLikesString.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(allLikesString.split(",")));
    }

    @Transient
    public void setAllLikes(Set<String> sequence) {
        this.allLikesString = String.join(",", sequence);
    }

    public void addToAllLikes(String value) {
        Set<String> current = getAllLikes();
        current.add(value);
        setAllLikes(current);
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players;

    public void addPlayer(Player player) {
        List<Player> currentPlayers = getPlayers();
        if (currentPlayers == null) {
            currentPlayers = new ArrayList<>();
        }
        currentPlayers.add(player);
        player.setSession(this);
        setPlayers(currentPlayers);
    }

}
