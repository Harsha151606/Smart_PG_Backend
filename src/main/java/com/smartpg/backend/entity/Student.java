package com.smartpg.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "pg_id")
    private PG pg;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    public Student() {
    }

    public Student(User user, PG pg, Room room) {
        this.user = user;
        this.pg = pg;
        this.room = room;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PG getPg() {
        return pg;
    }

    public void setPg(PG pg) {
        this.pg = pg;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}