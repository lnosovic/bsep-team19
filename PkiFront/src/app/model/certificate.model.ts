export interface CertificateDTO {
  id: number;
  serialNumber: string;
  subjectName: string;
  issuerName: string;
  validFrom: string;
  validTo: string;
  ca: boolean;
}

export interface NewCertificateDTO {
  commonName: string;
  organization: string;
  organizationalUnit: string;
  country: string;
  email: string;
  validFrom: string;
  validTo: string;
  certificateType: 'ROOT_CA' | 'INTERMEDIATE_CA' | 'END_ENTITY';
  issuerSerialNumber?: string;
}