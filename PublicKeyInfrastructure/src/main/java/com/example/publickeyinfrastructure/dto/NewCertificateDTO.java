package com.example.publickeyinfrastructure.dto;

import lombok.Data;

import java.util.Date;

@Data
public class NewCertificateDTO {
    private String commonName;
    private String organization;
    private String organizationalUnit;
    private String country;
    private String email;
    private Date validFrom;
    private Date validTo;
    private String certificateType;
    private String issuerSerialNumber;
}
