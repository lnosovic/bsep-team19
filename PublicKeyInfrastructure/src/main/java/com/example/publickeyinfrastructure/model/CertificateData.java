//package com.example.publickeyinfrastructure.model;
//
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.math.BigInteger;
//import java.util.Date;
//
//@Entity
//@Table(name = "certificates")
//@Getter
//@Setter
//@NoArgsConstructor
//public class CertificateData {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(unique = true, nullable = false, columnDefinition = "NUMERIC(40,0)")
//    private BigInteger serialNumber;
//
//    @Column(nullable = false)
//    private String subjectName;
//
//    @Column(nullable = false)
//    private String issuerName;
//
//    @Column(nullable = false)
//    private Date validFrom;
//
//    @Column(nullable = false)
//    private Date validTo;
//
//    @Column(nullable = false)
//    private boolean isCa;
//}
