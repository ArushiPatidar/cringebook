package com.cringebook.app.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity(name="simple_interest")
public class Interest {
    public Integer getIntital_balance() {
        return intital_balance;
    }

    public Integer getId() {
        return id;
    }

    @Id
    @Column(name="id")
    private Integer id;

    @Column(name="intital_balance")
    private Integer intital_balance ;
}
