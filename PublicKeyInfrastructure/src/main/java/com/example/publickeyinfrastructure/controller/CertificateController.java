package com.example.publickeyinfrastructure.controller;
import com.example.publickeyinfrastructure.dto.CertificateDTO;
import com.example.publickeyinfrastructure.dto.NewCertificateDTO;
import com.example.publickeyinfrastructure.service.CertificateService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;

    public CertificateController(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('CA_USER')")
    public ResponseEntity<String> createCertificate(@RequestBody NewCertificateDTO certificateDTO) {
        try {
            certificateService.createCertificate(certificateDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body("Sertifikat je uspešno kreiran.");
        } catch (Exception e) {
            // Logovati grešku za detaljniju analizu
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Došlo je do greške prilikom kreiranja sertifikata: " + e.getMessage());
        }
    }
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('CA_USER')")
    public ResponseEntity<List<CertificateDTO>> getAllCertificates() {
        List<CertificateDTO> certificates = certificateService.getAllCertificates();
        return ResponseEntity.ok(certificates);
    }
    @GetMapping("/download/{serialNumber}")
    //@PreAuthorize("isAuthenticated()") // Bilo koji ulogovani korisnik može da preuzme
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable String serialNumber) {
        try {
            byte[] certificateData = certificateService.downloadCertificate(serialNumber);

            // Kreiramo HTTP hedere za odgovor
            HttpHeaders headers = new HttpHeaders();

            // Heder koji kaže browseru da je ovo fajl za preuzimanje i sugeriše ime fajla
            // .cer je standardna ekstenzija za sertifikate
            headers.setContentDispositionFormData("attachment", serialNumber + ".cer");

            // Heder koji deklariše tip sadržaja.
            // "application/x-x509-ca-cert" je standardni MIME tip za sertifikate.
            headers.setContentType(MediaType.valueOf("application/x-x509-ca-cert"));

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(certificateData);

        } catch (RuntimeException e) {
            // Ako sertifikat nije pronađen, vrati 404 Not Found
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // Za sve druge greške, vrati 500 Internal Server Error
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
