import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PasswordManagerService } from './password-manager.service';
import { PasswordEntry } from '../model/password-entry.model';

@Component({
  selector: 'app-password-manager',
  templateUrl: './password-manager.component.html',
  styleUrls: ['./password-manager.component.css']
})
export class PasswordManagerComponent implements OnInit {
  passwordForm!: FormGroup;
  entries: PasswordEntry[] = [];
  isLoading = false;
  errorMessage: string | null = null;

  constructor(
    private fb: FormBuilder,
    private pmService: PasswordManagerService
  ) {}

  ngOnInit(): void {
    this.passwordForm = this.fb.group({
      siteName: ['', Validators.required],
      username: ['', Validators.required],
      password: ['', Validators.required],
    });
    this.loadEntries();
  }

  loadEntries(): void {
    this.isLoading = true;
    this.pmService.getAllEntries().subscribe({
      next: (data) => {
        this.entries = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load entries. Are you logged in?';
        this.isLoading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.passwordForm.invalid) return;

    this.pmService.saveEntry(this.passwordForm.value).subscribe({
      next: () => {
        this.loadEntries();
        this.passwordForm.reset();
      },
      error: (err) => {
        this.errorMessage = 'Failed to save the entry.';
      }
    });
  }

  viewPassword(id: number | undefined): void {
    if (!id) return;
    this.pmService.getPassword(id).subscribe({
      next: (entry) => {
        alert(`Password for ${entry.siteName} is: ${entry.password}`);
      },
      error: (err) => {
        alert('Could not retrieve password.');
      }
    });
  }
}