package com.dartscorer.backend.repository;

import com.dartscorer.backend.domain.ThrowEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ThrowRepository extends JpaRepository<ThrowEntity, UUID> {
}
