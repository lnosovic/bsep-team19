import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { RegisterComponent } from './register/register.component';
import { LoginComponent } from './login/login.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { AuthGuard } from './guards/auth.guard';


const routes: Routes = [
    { path: 'register', component: RegisterComponent },
    { path: 'login', component:LoginComponent},
     { 
    path: 'dashboard', 
    component: DashboardComponent, 
    canActivate: [AuthGuard]
  },
  { path: '', redirectTo: '/register', pathMatch: 'full' },
  { path: '', redirectTo: '/login', pathMatch: 'full'}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
