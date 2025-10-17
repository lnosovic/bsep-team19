package com.example.publickeyinfrastructure.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Data
@Getter
@Setter
@NoArgsConstructor
public class PasswordEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String siteName;
    private String username;

    @Lob // Large Object - za čuvanje enkriptovanih podataka
    @Column(length = 1024)
    private byte[] encryptedPassword;
}
