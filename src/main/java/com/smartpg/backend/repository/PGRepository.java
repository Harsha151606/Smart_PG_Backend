package com.smartpg.backend.repository;

import com.smartpg.backend.entity.PG;
import com.smartpg.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PGRepository extends JpaRepository<PG, Long> {

    List<PG> findByOwner(User owner);

    Optional<PG> findByIdAndOwner(Long id, User owner);
}