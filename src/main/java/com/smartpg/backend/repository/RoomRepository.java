package com.smartpg.backend.repository;

import com.smartpg.backend.entity.PG;
import com.smartpg.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByPg(PG pg);
}