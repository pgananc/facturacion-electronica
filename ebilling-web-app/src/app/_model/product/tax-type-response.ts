import { BasicEntity } from '../basic-entity';
import { TaxTypeDto } from './tax-type-dto';

/**
 * Respuesta del backend para el endpoint de tax-type
 * Sigue el patrón estándar de respuestas del sistema
 * ⚠️ IMPORTANTE: El backend usa "taxTypes" NO "taxTypeDtos"
 */
export interface TaxTypeResponse extends BasicEntity {
  taxTypes: TaxTypeDto[]; // ✅ Debe coincidir con el backend (sin "Dtos")
}
