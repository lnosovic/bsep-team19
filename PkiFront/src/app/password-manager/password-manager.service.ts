import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PasswordEntry } from '../model/password-entry.model';

@Injectable({
  providedIn: 'root'
})
export class PasswordManagerService {
  private apiUrl = 'https://localhost:8443/api/passwords';

  constructor(private http: HttpClient) { }

  getAllEntries(): Observable<PasswordEntry[]> {
    return this.http.get<PasswordEntry[]>(this.apiUrl);
  }

  getPassword(id: number): Observable<PasswordEntry> {
    return this.http.get<PasswordEntry>(`${this.apiUrl}/${id}`);
  }

  saveEntry(entry: PasswordEntry): Observable<PasswordEntry> {
    return this.http.post<PasswordEntry>(this.apiUrl, entry);
  }
}