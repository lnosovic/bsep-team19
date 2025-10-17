package com.example.publickeyinfrastructure.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigInteger;

@Entity
@Table(name = "certificates_metadata")
@Getter
@Setter
@NoArgsConstructor
public class CertificateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, columnDefinition = "NUMERIC(40,0)")
    private BigInteger serialNumber;

    @Column(nullable = false, unique = true)
    private String alias;

    @Column(nullable = false)
    private String privateKeyPassword;

    public CertificateEntity(BigInteger serialNumber, String alias, String privateKeyPassword) {
        this.serialNumber = serialNumber;
        this.alias = alias;
        this.privateKeyPassword = privateKeyPassword;
    }
}