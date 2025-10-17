//package com.example.publickeyinfrastructure.controller;
//import com.example.publickeyinfrastructure.dto.PasswordEntryDTO;
//import com.example.publickeyinfrastructure.service.PasswordManagerService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/passwords")
//@PreAuthorize("hasAuthority('ADMIN')") // SAMO ADMIN MOŽE DA PRISTUPI
//public class PasswordManagerController {
//
//    private final PasswordManagerService service;
//
//    public PasswordManagerController(PasswordManagerService service) {
//        this.service = service;
//    }
//
//    @PostMapping
//    public ResponseEntity<PasswordEntryDTO> savePassword(@RequestBody PasswordEntryDTO dto) throws Exception {
//        return ResponseEntity.ok(service.savePassword(dto));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<PasswordEntryDTO> getPassword(@PathVariable Long id) throws Exception {
//        return ResponseEntity.ok(service.getPassword(id));
//    }
//
//    @GetMapping
//    public ResponseEntity<List<PasswordEntryDTO>> getAll() {
//        return ResponseEntity.ok(service.getAllEntries());
//    }
//}