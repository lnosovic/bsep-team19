package com.example.publickeyinfrastructure.repository;
import com.example.publickeyinfrastructure.dto.CertificateDTO;
import com.example.publickeyinfrastructure.util.IssuerData;
import com.example.publickeyinfrastructure.util.SubjectData;
import java.math.BigInteger;
import java.security.KeyPair;
import java.util.List;

public interface CertificateRepository {
    void saveRootCertificate(SubjectData subjectData, IssuerData issuerData, String privateKeyPassword, String alias) throws Exception;
    void saveChainedCertificate(SubjectData subjectData, KeyPair keyPair, BigInteger issuerSerialNumber, boolean isCa, String privateKeyPassword, String alias) throws Exception;
    List<CertificateDTO> findAllCertificates() throws Exception;
    byte[] getCertificateFile(BigInteger serialNumber) throws Exception;
}