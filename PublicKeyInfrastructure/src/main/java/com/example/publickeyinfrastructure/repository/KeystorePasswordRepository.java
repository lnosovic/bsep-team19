package com.example.publickeyinfrastructure.repository;

import com.example.publickeyinfrastructure.model.KeystorePassword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface KeystorePasswordRepository extends JpaRepository<KeystorePassword, Long> {
    @Query("SELECT kp FROM KeystorePassword kp ORDER BY kp.id DESC LIMIT 1")
    KeystorePassword findLatest();
}
