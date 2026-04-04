import { Client } from './client';

/**
 * DTO for paginated client search results
 * Returned from searchPageable endpoint
 */
export interface ClientPageResponse {
  content: Client[];
  totalElements: number;
  totalPages?: number;
  size?: number;
  number?: number;
}
