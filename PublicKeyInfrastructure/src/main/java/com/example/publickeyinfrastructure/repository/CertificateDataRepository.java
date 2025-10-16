package com.example.publickeyinfrastructure.repository;
import com.example.publickeyinfrastructure.model.CertificateData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.Optional;
@Repository
public interface CertificateDataRepository extends JpaRepository<CertificateData, Long> {
    Optional<CertificateData> findBySerialNumber(BigInteger serialNumber);
}
