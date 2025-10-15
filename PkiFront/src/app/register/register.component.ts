import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Subscription } from 'rxjs';
import * as zxcvbn from 'zxcvbn';
import { AuthService, RegisterRequest } from '../auth/auth.service';// Corrected path

// Custom validator to check if passwords match
export function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password');
  const confirmPassword = control.get('confirmPassword');

  // Return null if controls haven't been initialized yet
  if (!password || !confirmPassword) {
    return null;
  }

  if (password.value !== confirmPassword.value) {
    // Set error on the confirmPassword control to display it easily
    confirmPassword.setErrors({ mismatch: true });
    return { mismatch: true };
  } else {
    // If passwords match and the control has a mismatch error, clear it
    if (confirmPassword.hasError('mismatch')) {
      confirmPassword.setErrors(null);
    }
    return null;
  }
}

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css'] // Use .scss for nested styles
})
export class RegisterComponent implements OnInit, OnDestroy {
  registerForm!: FormGroup;
  passwordStrength = { score: 0, feedback: '' };
  private passwordSub!: Subscription;

  errorMessage: string | null = null;
  successMessage: string | null = null;
  isLoading = false;

  constructor(private fb: FormBuilder, private authService: AuthService) { }

  ngOnInit(): void {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      organization: [''],
      password: ['', [Validators.required]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: passwordMatchValidator }); // Apply the custom validator at the form level

    // Subscribe to password changes to update the strength meter
    const passwordControl = this.registerForm.get('password');
    if (passwordControl) {
      this.passwordSub = passwordControl.valueChanges.subscribe(value => {
        this.updatePasswordStrength(value);
      });
    }
  }

  // Getter for easy access to form controls in the template
  get f() {
    return this.registerForm.controls;
  }

  updatePasswordStrength(password: string): void {
    if (!password) {
      this.passwordStrength = { score: 0, feedback: '' };
      return;
    }
    const result = zxcvbn(password);
    this.passwordStrength.score = result.score; // Score from 0 to 4

    const feedbackMessages = [
      'Very weak', 'Weak', 'Fair', 'Strong', 'Very strong'
    ];
    this.passwordStrength.feedback = feedbackMessages[result.score];
  }

  onSubmit(): void {
    this.registerForm.markAllAsTouched(); // Mark all fields to show errors if form is submitted empty
    if (this.registerForm.invalid || this.isLoading) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;
    this.successMessage = null;

    const requestData: RegisterRequest = this.registerForm.value;

    this.authService.register(requestData).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.successMessage = response; // e.g., "Please check your email to verify your account."
        this.registerForm.reset();
      },
      error: (err) => {
        this.isLoading = false;
        // Handle validation errors from the Spring Boot backend
        this.errorMessage = err.error?.message || err.error || 'An unknown error occurred. Please try again.';
      }
    });
  }

  ngOnDestroy(): void {
    // Unsubscribe to prevent memory leaks
    if (this.passwordSub) {
      this.passwordSub.unsubscribe();
    }
  }
}
