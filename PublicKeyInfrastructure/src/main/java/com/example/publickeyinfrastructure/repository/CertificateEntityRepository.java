package com.example.publickeyinfrastructure.repository;

import com.example.publickeyinfrastructure.model.CertificateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigInteger;
import java.util.Optional;

public interface CertificateEntityRepository extends JpaRepository<CertificateEntity, Long> {
    Optional<CertificateEntity> findBySerialNumber(BigInteger serialNumber);
}