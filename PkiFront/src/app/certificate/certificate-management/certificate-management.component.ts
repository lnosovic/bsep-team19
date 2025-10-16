import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CertificateService } from '../certificate.service';
import { CertificateDTO, NewCertificateDTO } from 'src/app/model/certificate.model';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-certificate-management',
  templateUrl: './certificate-management.component.html',
  styleUrls: ['./certificate-management.component.css']
})
export class CertificateManagementComponent implements OnInit {
  
  createForm!: FormGroup;
  certificates: CertificateDTO[] = [];
  isLoading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private certificateService: CertificateService
  ) {}

  ngOnInit(): void {
    this.createForm = this.fb.group({
      commonName: ['', Validators.required],
      organization: ['', Validators.required],
      organizationalUnit: ['', Validators.required],
      country: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      validFrom: ['', Validators.required],
      validTo: ['', Validators.required],
      certificateType: ['END_ENTITY', Validators.required],
      issuerSerialNumber: [''] // Nije obavezno na početku
    });

    this.loadCertificates();

    // Dodajemo logiku da issuerSerialNumber bude obavezan ako nije ROOT_CA
    this.createForm.get('certificateType')?.valueChanges.subscribe(type => {
      const issuerControl = this.createForm.get('issuerSerialNumber');
      if (type !== 'ROOT_CA') {
        issuerControl?.setValidators([Validators.required]);
      } else {
        issuerControl?.clearValidators();
      }
      issuerControl?.updateValueAndValidity();
    });
  }

  loadCertificates(): void {
    this.isLoading = true;
    this.certificateService.getAllCertificates().subscribe({
      next: (data) => {
        this.certificates = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load certificates.';
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    this.createForm.markAllAsTouched();
    if (this.createForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const formData: NewCertificateDTO = this.createForm.value;
    
    // Ukloni issuerSerialNumber ako je ROOT_CA
    if (formData.certificateType === 'ROOT_CA') {
      delete formData.issuerSerialNumber;
    }

    this.certificateService.createCertificate(formData).subscribe({
      next: (response) => {
        this.successMessage = response;
        this.isLoading = false;
        this.createForm.reset({ certificateType: 'END_ENTITY' }); // Resetuj formu
        this.loadCertificates(); // Ponovo učitaj listu
      },
      error: (err) => {
        this.errorMessage = err.error?.message || err.error || 'Certificate creation failed.';
        this.isLoading = false;
      }
    });
  }
}