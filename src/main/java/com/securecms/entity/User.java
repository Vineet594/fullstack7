package com.securecms.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Application user.
 * - password       : BCrypt HASH (one-way)
 * - encryptedPhone : AES-GCM ENCRYPTED (reversible, only with the secret key)
 * Indexes on username and email speed up login and the duplicate checks during registration.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username", unique = true),
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, length = 30)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(name = "encrypted_phone", length = 255)
    private String encryptedPhone;

    /** Many users share one role. LAZY so that loading a user never triggers extra queries by accident. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /** One user writes many posts. Deleting a user removes his posts (cascade REMOVE). */
    @OneToMany(mappedBy = "author", cascade = CascadeType.REMOVE)
    private List<Post> posts = new ArrayList<>();
}
