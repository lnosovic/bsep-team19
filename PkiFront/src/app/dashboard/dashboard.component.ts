import { Component, OnInit } from '@angular/core';
import { AuthService, DecodedToken } from '../auth/auth.service';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, DatePipe, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  currentUser: DecodedToken | null = null;
  canManageCertificates = false;
  
  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.currentUser = this.authService.getUserDetails();
    if (this.currentUser) {
      this.canManageCertificates = this.currentUser.role === 'ADMIN' || this.currentUser.role === 'CA_USER';
    }
  }
}