package com.dartscorer.backend.repository;

import com.dartscorer.backend.domain.RoundEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundRepository extends JpaRepository<RoundEntity, UUID> {
}
