package com.dartscorer.backend.repository;

import com.dartscorer.backend.domain.PlayerEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<PlayerEntity, UUID> {
}
