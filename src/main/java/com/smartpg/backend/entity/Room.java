package com.smartpg.backend.entity;

import jakarta.persistence.*;

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

    @Column(nullable = false)
    private Double rent;

    @ManyToOne
    @JoinColumn(name = "pg_id", nullable = false)
    private PG pg;

    public Room() {
    }

    public Room(String roomNumber, Integer capacity, Double rent, PG pg) {
        this.roomNumber = roomNumber;
        this.capacity = capacity;
        this.rent = rent;
        this.pg = pg;
    }

    public Long getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Double getRent() {
        return rent;
    }

    public void setRent(Double rent) {
        this.rent = rent;
    }

    public PG getPg() {
        return pg;
    }

    public void setPg(PG pg) {
        this.pg = pg;
    }

    public int getOccupied() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOccupied'");
    }

    public void setOccupied(int i) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setOccupied'");
    }
}