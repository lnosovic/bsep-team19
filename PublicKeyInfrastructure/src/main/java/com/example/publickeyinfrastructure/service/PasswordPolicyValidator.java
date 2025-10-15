package com.example.publickeyinfrastructure.service;
import com.example.publickeyinfrastructure.dto.RegisterRequest;
import com.nulabinc.zxcvbn.Zxcvbn;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate; // Ili WebClient

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
@Service
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 12;
    private static final int MIN_ZXCVBN_SCORE = 3; // Zahtevamo skor "Safely unguessable" ili bolji

    /**
     * Glavna metoda koja pokreće sve provere.
     * Baca izuzetak ako bilo koja provera ne uspe.
     */
    public void validate(RegisterRequest request) {
        String password = request.getPassword();

        // 1. Provera poklapanja lozinki
        if (!password.equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Lozinke se ne poklapaju.");
        }

        // 2. Provera minimalne dužine
        if (password.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Lozinka mora imati najmanje " + MIN_LENGTH + " karaktera.");
        }

        // 3. Provera protiv kontekstualnih podataka
        checkAgainstContextualInfo(password, request);

        // 4. Provera jačine pomoću zxcvbn
        checkStrength(password);

        // 5. Provera protiv poznatih procurelih lozinki (Pwned Passwords)
        checkIfPwned(password);
    }

    /**
     * Proverava da lozinka ne sadrži delove imena, prezimena ili email-a.
     */
    private void checkAgainstContextualInfo(String password, RegisterRequest request) {
        String lowerCasePassword = password.toLowerCase();
        if (lowerCasePassword.contains(request.getFirstName().toLowerCase()) ||
                lowerCasePassword.contains(request.getLastName().toLowerCase()) ||
                lowerCasePassword.contains(request.getPassword().split("@")[0].toLowerCase())) {
            throw new IllegalArgumentException("Lozinka ne sme sadržati vaše ime, prezime ili email adresu.");
        }
    }

    /**
     * Koristi zxcvbn biblioteku da proceni jačinu lozinke.
     * Skorovi: 0 (veoma slaba) do 4 (veoma jaka). Zahtevamo minimum 3.
     */
    private void checkStrength(String password) {
        Zxcvbn zxcvbn = new Zxcvbn();
        var strength = zxcvbn.measure(password);

        if (strength.getScore() < MIN_ZXCVBN_SCORE) {
            // Možete vratiti i konkretne sugestije korisniku
            // String feedback = String.join("\n", strength.getFeedback().getSuggestions());
            throw new IllegalArgumentException("Lozinka je preslaba. Pokušajte da dodate još reči ili simbola.");
        }
    }

    /**
     * Proverava lozinku protiv "Have I Been Pwned" Pwned Passwords API-ja
     * na bezbedan način (koristeći k-Anonymity).
     */
    private void checkIfPwned(String password) {
        try {
            // 1. Kreiraj SHA-1 heš lozinke
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(password.getBytes());
            String hexHash = toHexString(hash);

            // 2. Uzmi prvih 5 karaktera heša za API poziv
            String prefix = hexHash.substring(0, 5);
            String suffix = hexHash.substring(5);

            // 3. Pozovi Pwned Passwords API
            RestTemplate restTemplate = new RestTemplate();
            String apiUrl = "https://api.pwnedpasswords.com/range/" + prefix;
            String response = restTemplate.getForObject(apiUrl, String.class);

            // 4. Proveri da li se ostatak heša (sufiks) nalazi u odgovoru
            if (response != null && response.toUpperCase().contains(suffix.toUpperCase())) {
                throw new IllegalArgumentException("Ova lozinka je kompromitovana u nekom od prethodnih curenja podataka. Molimo izaberite drugu.");
            }
        } catch (NoSuchAlgorithmException e) {
            // Logovati grešku, ali možda ne blokirati registraciju ako SHA-1 nije dostupan
            // (što je skoro nemoguće u standardnom Java okruženju)
            System.err.println("SHA-1 algoritam nije pronađen.");
        } catch (Exception e) {
            // Ako API nije dostupan, ne treba blokirati registraciju.
            // Logovati grešku za buduću analizu.
            System.err.println("Nije moguće proveriti lozinku protiv Pwned Passwords API-ja: " + e.getMessage());
        }
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
