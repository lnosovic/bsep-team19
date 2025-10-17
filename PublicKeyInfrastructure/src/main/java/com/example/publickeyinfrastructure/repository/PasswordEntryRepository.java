package com.example.publickeyinfrastructure.repository;

import com.example.publickeyinfrastructure.model.PasswordEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordEntryRepository extends JpaRepository<PasswordEntry, Long> {
}
