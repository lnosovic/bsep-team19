package com.example.publickeyinfrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Automatski generiše gettere, settere, toString(), equals() i hashCode()
@NoArgsConstructor // Automatski generiše prazan konstruktor
@AllArgsConstructor // Automatski generiše konstruktor sa svim poljima
public class PasswordEntryDTO {

    private Long id;
    private String siteName;
    private String username;

    // Ovo polje sadrži neenkriptovanu lozinku prilikom slanja na backend,
    // ili prilikom čitanja sa backenda.
    // Prilikom listanja svih unosa, ovo polje treba da bude null.
    private String password;
}
