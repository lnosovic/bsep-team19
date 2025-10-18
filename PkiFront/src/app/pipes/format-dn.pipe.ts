import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'formatDn'
})
export class FormatDnPipe implements PipeTransform {

  // Mapa poznatih OID-ova i njihovih skraćenica
  private static readonly OID_MAP: { [key: string]: string } = {
    '1.2.840.113549.1.9.1': 'E' // Email Address
    // Ovde možete dodati i druge OID-ove ako se pojave
  };

  transform(value: string | null | undefined): string {
    if (!value) {
      return '';
    }

    // Razdvajamo string na pojedinačne atribute
    const parts = value.split(',');
    
    const formattedParts = parts.map(part => {
      const trimmedPart = part.trim();
      
      // Proveravamo da li deo počinje sa poznatim OID-om
      for (const oid in FormatDnPipe.OID_MAP) {
        if (trimmedPart.startsWith(oid)) {
          // Ako počinje, izvlačimo heksadecimalnu vrednost
          // Primer: 1.2.840.113549.1.9.1=#16106361...
          const hexValue = trimmedPart.split('#')[1]?.substring(4); // Uklanjamo #1610 prefix
          if (hexValue) {
            // Konvertujemo heksadecimalnu vrednost nazad u string (email)
            const email = this.hexToString(hexValue);
            return `${FormatDnPipe.OID_MAP[oid]}=${email}`;
          }
        }
      }
      // Ako nije OID, vraćamo originalni deo
      return trimmedPart;
    });

    // Spajamo formatirane delove nazad u jedan string
    return formattedParts.join(', ');
  }

  private hexToString(hex: string): string {
    let str = '';
    for (let i = 0; i < hex.length; i += 2) {
      str += String.fromCharCode(parseInt(hex.substr(i, 2), 16));
    }
    return str;
  }
}