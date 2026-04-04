export interface Menu {
  id: number;
  name: string;
  icon: string;
  url: string;
  featureCode?: string; // 🔧 Opcional: Si el backend lo envía, se usa directamente
  order?: number; // 🔧 Opcional: Orden de visualización (menor = primero)
  subMenus: Menu[];
}
