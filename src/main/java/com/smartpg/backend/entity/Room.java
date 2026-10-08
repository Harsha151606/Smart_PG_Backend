package com.smartpg.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roomNumber;

    @Column(nullable = false)
    private Integer capacity;

    @Column(nullable = false, columnDefinition = "int default 0")
    private Integer occupied = 0;

    @Column(nullable = false)
    private Double rent;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "pg_id", nullable = false)
    private PG pg;

    public Room() {}

    public Room(String roomNumber, Integer capacity, Double rent, PG pg) {
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.occupied = 0;
        this.rent = rent;
        this.pg = pg;
    }

    public Long getId() { return id; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public Integer getOccupied() { return occupied != null ? occupied : 0; }
    public void setOccupied(Integer occupied) { this.occupied = occupied; }

    public boolean isAvailable() { return getOccupied() < getCapacity(); }

    public Double getRent() { return rent; }
    public void setRent(Double rent) { this.rent = rent; }

    public PG getPg() { return pg; }
    public void setPg(PG pg) { this.pg = pg; }
}