import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { RegisterComponent } from './register/register.component';
import { NgxCaptchaModule } from 'ngx-captcha';
import { LoginComponent } from './login/login.component';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { CommonModule, DatePipe } from '@angular/common';
import { CertificateManagementComponent } from './certificate/certificate-management/certificate-management.component';
import { NavbarComponent } from './navbar/navbar.component';
import { PasswordManagerComponent } from './password-manager/password-manager.component';
import { FormatDnPipe } from './pipes/format-dn.pipe';

@NgModule({
  declarations: [
    AppComponent,
    RegisterComponent,
    LoginComponent,
    CertificateManagementComponent,
    NavbarComponent,
    PasswordManagerComponent,
    FormatDnPipe
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    HttpClientModule,
    NgxCaptchaModule,
    CommonModule,
    DatePipe
  ],
  providers: [

    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
