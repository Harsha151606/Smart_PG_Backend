package com.smartpg.backend.service;

import com.smartpg.backend.dto.RoomRequest;
import com.smartpg.backend.entity.PG;
import com.smartpg.backend.entity.Room;
import com.smartpg.backend.entity.Role;
import com.smartpg.backend.entity.User;
import com.smartpg.backend.repository.PGRepository;
import com.smartpg.backend.repository.RoomRepository;
import com.smartpg.backend.repository.StudentRepository;
import com.smartpg.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final PGRepository pgRepository;
    private final UserRepository userRepository;
    public RoomService(RoomRepository roomRepository,
                       PGRepository pgRepository,
                       UserRepository userRepository,
                       StudentRepository studentRepository) {
        this.roomRepository = roomRepository;
        this.pgRepository = pgRepository;
        this.userRepository = userRepository;
    }

    public Room createRoom(RoomRequest request, String ownerEmail) {

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        if (owner.getRole() != Role.OWNER && owner.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only owners can create rooms");
        }

        PG pg;
        if (owner.getRole() == Role.ADMIN) {
            pg = pgRepository.findById(request.getPgId())
                    .orElseThrow(() -> new RuntimeException("PG not found"));
        } else {
            pg = pgRepository.findByIdAndOwner(request.getPgId(), owner)
                    .orElseThrow(() -> new RuntimeException("PG not found or does not belong to owner"));
        }

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

        PG pg;
        if (owner.getRole() == Role.ADMIN) {
            pg = pgRepository.findById(pgId)
                    .orElseThrow(() -> new RuntimeException("PG not found"));
        } else {
            pg = pgRepository.findByIdAndOwner(pgId, owner)
                    .orElseThrow(() -> new RuntimeException("PG not found or does not belong to owner"));
        }

        return roomRepository.findByPg(pg);
    }

    public List<Room> getAllRoomsForOwner(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        if (owner.getRole() == Role.ADMIN) {
            return roomRepository.findAll();
        }

        List<PG> pgs = pgRepository.findByOwner(owner);
        List<Room> allRooms = new ArrayList<>();
        for (PG pg : pgs) {
            allRooms.addAll(roomRepository.findByPg(pg));
        }
        return allRooms;
    }

    public Room updateRoom(Long roomId, RoomRequest request, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (owner.getRole() != Role.ADMIN && !room.getPg().getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("You do not have permission to update this room");
        }

        if (request.getCapacity() < room.getOccupied()) {
            throw new RuntimeException("New capacity cannot be less than currently occupied count (" + room.getOccupied() + ")");
        }

        room.setRoomNumber(request.getRoomNumber());
        room.setCapacity(request.getCapacity());
        room.setRent(request.getRent());

        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long roomId, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (owner.getRole() != Role.ADMIN && !room.getPg().getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("You do not have permission to delete this room");
        }

        if (room.getOccupied() > 0) {
            throw new RuntimeException("Cannot delete room that currently has occupied students");
        }

        roomRepository.delete(room);
    }
}