package com.smartpg.backend.repository;

import com.smartpg.backend.entity.PG;
import com.smartpg.backend.entity.Room;
import com.smartpg.backend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    List<Student> findByPg(PG pg);

    long countByRoom(Room room);
}