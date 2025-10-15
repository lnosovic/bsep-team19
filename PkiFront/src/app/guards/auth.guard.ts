import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
@Injectable({
  providedIn: 'root'
})

export class AuthGuard implements CanActivate {

  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean {
    if (this.authService.isLoggedIn()) {
      return true; // Korisnik je ulogovan, dozvoli pristup
    } else {
      // Korisnik nije ulogovan, preusmeri ga na login stranicu
      this.router.navigate(['/login']);
      return false;
    }
  }
}