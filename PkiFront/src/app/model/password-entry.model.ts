export interface PasswordEntry {
  id?: number;
  siteName: string;
  username: string;
  password?: string; // Opciono jer ga ne dobijamo u listi
}