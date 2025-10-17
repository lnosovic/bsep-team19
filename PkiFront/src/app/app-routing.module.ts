import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RegisterComponent } from './register/register.component';
import { LoginComponent } from './login/login.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { AuthGuard } from './guards/auth.guard';
import { CertificateManagementComponent } from './certificate/certificate-management/certificate-management.component';
import { PasswordManagerComponent } from './password-manager/password-manager.component';


const routes: Routes = [
    { path: 'register', component: RegisterComponent },
    { path: 'login', component:LoginComponent},
    { 
    path: 'dashboard', 
    component: DashboardComponent, 
    canActivate: [AuthGuard]
    },
    {
    path: 'passwords',
    component: PasswordManagerComponent,
    canActivate: [AuthGuard] // Samo ulogovani korisnici mogu ovde
  },
    { 
      path: 'certificates', 
      component: CertificateManagementComponent, 
      canActivate: [AuthGuard] // Zaštitite rutu
    },
  { path: '', redirectTo: '/register', pathMatch: 'full' },
  { path: '', redirectTo: '/login', pathMatch: 'full'},
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
