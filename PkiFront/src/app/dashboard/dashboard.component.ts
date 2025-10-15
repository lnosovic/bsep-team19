import { Component, OnInit } from '@angular/core';
import { AuthService, DecodedToken } from '../auth/auth.service';
import { CommonModule, DatePipe } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {

  currentUser: DecodedToken | null = null;
  
  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.currentUser = this.authService.getUserDetails();
    console.log(this.currentUser);
  }
}