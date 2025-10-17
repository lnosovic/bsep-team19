import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CertificateDTO, NewCertificateDTO } from '../model/certificate.model';

@Injectable({
  providedIn: 'root'
})
export class CertificateService {
  private apiUrl = 'https://localhost:8443/api/certificates'; // Putanja do vašeg API-ja

  constructor(private http: HttpClient) { }

  // Dohvatanje svih sertifikata
  getAllCertificates(): Observable<CertificateDTO[]> {
    return this.http.get<CertificateDTO[]>(this.apiUrl);
  }

  // Kreiranje novog sertifikata
  createCertificate(data: NewCertificateDTO): Observable<string> {
    return this.http.post(`${this.apiUrl}/create`, data, { responseType: 'text' });
  }
  downloadCertificate(serialNumber: string): Observable<Blob> {
    // Korišćenje 'blob' kao responseType je ključno!
    return this.http.get(`${this.apiUrl}/download/${serialNumber}`, {
      responseType: 'blob' 
    });
  }
}