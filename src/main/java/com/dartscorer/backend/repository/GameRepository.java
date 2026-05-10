package com.dartscorer.backend.repository;

import com.dartscorer.backend.domain.GameEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<GameEntity, UUID> {
}
