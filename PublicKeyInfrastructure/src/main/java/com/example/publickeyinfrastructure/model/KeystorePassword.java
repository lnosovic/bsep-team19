package com.example.publickeyinfrastructure.model;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "keystore_password")
@Data
@NoArgsConstructor
public class KeystorePassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String password;

    public KeystorePassword(String password) {
        this.password = password;
    }
}
