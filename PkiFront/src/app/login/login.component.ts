import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { environment } from '../enviropment';
import { AuthService, LoginRequest } from '../auth/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  siteKey: string;
  isLoading = false;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.siteKey = environment.recaptcha.siteKey;
  }

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
      recaptcha: ['', [Validators.required]]
    });
  }

  get f() {
    return this.loginForm.controls;
  }

  onSubmit(): void {
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid || this.isLoading) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = null;

    const formValue = this.loginForm.value;
    const requestData: LoginRequest = {
      email: formValue.email,
      password: formValue.password,
      recaptchaToken: formValue.recaptcha
    };

    this.authService.login(requestData).subscribe({
      next: (response) => {
        this.isLoading = false;

        console.log('Login successful!', response.token);
         this.router.navigate(['/dashboard']); 
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || err.error || 'Login failed. Please check your credentials and try again.';
        this.loginForm.get('recaptcha')?.reset();
      }
    });
  }
}