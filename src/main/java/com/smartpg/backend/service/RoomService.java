package com.smartpg.backend.service;

import com.smartpg.backend.dto.RoomRequest;
import com.smartpg.backend.entity.PG;
import com.smartpg.backend.entity.Room;
import com.smartpg.backend.entity.Role;
import com.smartpg.backend.entity.User;
import com.smartpg.backend.repository.PGRepository;
import com.smartpg.backend.repository.RoomRepository;
import com.smartpg.backend.repository.UserRepository;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final PGRepository pgRepository;
    private final UserRepository userRepository;

    public RoomService(RoomRepository roomRepository,
            PGRepository pgRepository,
            UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.pgRepository = pgRepository;
        this.userRepository = userRepository;
    }

    public Room createRoom(RoomRequest request, String ownerEmail) {

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        if (owner.getRole() != Role.OWNER) {
            throw new RuntimeException("Only owners can create rooms");
        }

        PG pg = pgRepository.findByIdAndOwner(request.getPgId(), owner)
                .orElseThrow(() -> new RuntimeException("PG not found or does not belong to owner"));

        Room room = new Room(
                request.getRoomNumber(),
                request.getCapacity(),
                request.getRent(),
                pg);
        return roomRepository.save(room);
    }

    public List<Room> getRoomsByPG(Long pgId, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        PG pg = pgRepository.findByIdAndOwner(pgId, owner)
                .orElseThrow(() -> new RuntimeException("PG not found or does not belong to owner"));

        return roomRepository.findByPg(pg);
    }
}