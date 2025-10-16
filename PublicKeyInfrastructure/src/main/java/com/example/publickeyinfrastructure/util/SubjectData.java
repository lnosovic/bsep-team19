package com.example.publickeyinfrastructure.util;
import lombok.Getter;
import org.bouncycastle.asn1.x500.X500Name;
import java.security.PublicKey;
import java.math.BigInteger;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Getter
@AllArgsConstructor
public class SubjectData {
    private PublicKey publicKey;
    private X500Name x500name;
    private BigInteger serialNumber;
    private Date startDate;
    private Date endDate;
}
