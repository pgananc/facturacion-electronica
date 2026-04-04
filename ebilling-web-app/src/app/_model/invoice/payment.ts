export interface Payment {
  idPaymentMethod?: number; // ✅ ID de la forma de pago en la BD
  paymentType: string;
  paymentMethodCode?: string; // Código SRI (ej: "01", "16", "17", etc.)
  value: number;
  term?: number;
  time?: string;
}
