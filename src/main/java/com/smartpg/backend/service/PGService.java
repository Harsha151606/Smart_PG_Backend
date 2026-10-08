package com.smartpg.backend.service;

import com.smartpg.backend.dto.PGRequest;
import com.smartpg.backend.entity.PG;
import com.smartpg.backend.entity.Role;
import com.smartpg.backend.entity.User;
import com.smartpg.backend.repository.PGRepository;
import com.smartpg.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class PGService {

    private final PGRepository pgRepository;
    private final UserRepository userRepository;

    public PGService(PGRepository pgRepository,
                     UserRepository userRepository) {
        this.pgRepository = pgRepository;
        this.userRepository = userRepository;
    }

    public PG createPG(PGRequest request, String ownerEmail) {

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        if (owner.getRole() != Role.OWNER) {
            throw new RuntimeException("Only owners can create a PG");
        }

        PG pg = new PG(
                request.getName(),
                request.getAddress(),
                owner
        );

        return pgRepository.save(pg);
    }
}