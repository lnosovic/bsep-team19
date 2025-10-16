import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
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
  today: string;

  constructor(
    private fb: FormBuilder,
    private certificateService: CertificateService
  ) {
    const date = new Date();
    this.today = date.toISOString().split('T')[0]

  }

  ngOnInit(): void {
    this.createForm = this.fb.group({
      commonName: ['', Validators.required],
      organization: ['', Validators.required],
      organizationalUnit: ['', Validators.required],
      country: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(4)]],
      email: ['', [Validators.required, Validators.email]],
      validFrom: ['', Validators.required],
      validTo: ['', Validators.required],
      certificateType: ['END_ENTITY', Validators.required],
      issuerSerialNumber: [''] // Nije obavezno na početku
    }, { validators: dateRangeValidator() }); 

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
export function dateRangeValidator(): ValidatorFn {
  return (formGroup: AbstractControl): ValidationErrors | null => {
    const validFromControl = formGroup.get('validFrom');
    const validToControl = formGroup.get('validTo');

    const validFrom = validFromControl?.value;
    const validTo = validToControl?.value;

    // --- DODATA VALIDACIJA ZA PROŠLOST ---
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Postavi na početak dana za precizno poređenje

    if (validFrom) {
        const fromDate = new Date(validFrom);
        if (fromDate < today) {
            validFromControl?.setErrors({ pastDate: true });
        } else if (validFromControl?.hasError('pastDate')) {
            // Ukloni grešku ako je ispravljeno
            validFromControl?.setErrors(null);
        }
    }
    // --- KRAJ VALIDACIJE ZA PROŠLOST ---


    // --- Postojeća validacija opsega ---
    if (validFrom && validTo) {
      const fromDate = new Date(validFrom);
      const toDate = new Date(validTo);

      if (fromDate > toDate) {
        validToControl?.setErrors({ dateInvalid: true });
        return { dateInvalid: true }; // Vrati grešku za formu
      }
    }
    
    if (validToControl?.hasError('dateInvalid')) {
        const errors = validToControl.errors;
        if (errors) {
            delete errors['dateInvalid'];
            if (Object.keys(errors).length === 0) {
                validToControl.setErrors(null);
            } else {
                 validToControl.setErrors(errors);
            }
        }
    }

    return null; // Vrati null ako je sve validno
  };
}