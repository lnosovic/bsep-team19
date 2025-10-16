package com.example.publickeyinfrastructure.util;
import org.bouncycastle.asn1.x500.X500Name;
import java.security.PrivateKey;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IssuerData {
    private PrivateKey privateKey;
    private X500Name x500name;
}