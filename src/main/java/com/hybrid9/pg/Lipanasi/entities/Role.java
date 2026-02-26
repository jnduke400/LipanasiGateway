package com.hybrid9.pg.Lipanasi.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;

@Getter
@Setter
@Entity
@Table(name = "c2b_roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    @Column(name = "name")
    private String name;

    //@ManyToMany(mappedBy = "roles",cascade = CascadeType.ALL)
    @ManyToMany(fetch = FetchType.LAZY,
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE
            }, mappedBy = "roles")
    private Collection<AppUser> users = new ArrayList<>();

    public Role() {}

    public Role(String name) {
        this.name = name;
    }
}
