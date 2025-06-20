package com.andreea.twoplayermoviepicker.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "players")

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String playerSessionId;

    @JoinColumn(name = "session_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Session session;

    @Column(name = "likes")
    private String likesString;

    @Transient
    public Set<String> getLikes() {
        if (likesString == null || likesString.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(likesString.split(",")));
    }

    @Transient
    public void setLikes(Set<String> sequence) {
        this.likesString = String.join(",", sequence);
    }

    public void addToLikes(String value) {
        Set<String> current = getLikes();
        current.add(value);
        setLikes(current);
    }

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ColumnDefault("0")
    private Integer seedIndex;
}
