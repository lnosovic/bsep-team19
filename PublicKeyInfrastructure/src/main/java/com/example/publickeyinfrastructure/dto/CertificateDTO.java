package com.example.publickeyinfrastructure.dto;

import lombok.Data;

import java.math.BigInteger;
import java.util.Date;

@Data
public class CertificateDTO {
    private Long id;
    private String serialNumber;
    private String subjectName;
    private String issuerName;
    private Date validFrom;
    private Date validTo;
    private boolean isCa;
}
