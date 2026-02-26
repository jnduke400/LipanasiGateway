package com.hybrid9.pg.Lipanasi.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.hybrid9.pg.Lipanasi.entities.vendorx.VendorDetails;
import com.hybrid9.pg.Lipanasi.models.BaseAudit;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@Table(name = "c2b_users")
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "id"
)
public class AppUser extends BaseAudit<String> implements UserDetails {
    private String password;
    private String firstname;
    private String middlename;
    private String lastname;
    private String phoneNumber;
    private String username;
    private String address;
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "users_roles",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id")},
            inverseJoinColumns = {@JoinColumn(name = "role_id", referencedColumnName = "id")})
    @Builder.Default
    private Collection<Role> roles = new ArrayList<>();
    /*@OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "vendor_id")
    private VendorDetails vendorx;*/
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }


    public AppUser(String password, String firstname, String middlename, String lastname, String phoneNumber, String username, String address, Collection<Role> role) {
        this.password = password;
        this.firstname = firstname;
        this.middlename = middlename;
        this.lastname = lastname;
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.address = address;
        this.roles = role;
    }


}
