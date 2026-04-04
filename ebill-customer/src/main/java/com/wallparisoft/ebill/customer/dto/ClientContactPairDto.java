package com.wallparisoft.ebill.customer.dto;

import com.wallparisoft.ebill.customer.entity.Contact;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO auxiliar para batch loading de contactos.
 *
 * <p><b>PROPÓSITO:</b> Evitar N+1 queries al cargar contactos de múltiples clientes.
 *
 * <p><b>PROBLEMA SIN ESTE DTO:</b>
 *
 * <pre>
 * // Por cada cliente, hacer una query
 * clients.forEach(client -> {
 *     List<Contact> contacts = repo.findByClientId(client.getId()); // N queries
 * });
 * </pre>
 *
 * <p><b>SOLUCIÓN CON ESTE DTO:</b>
 *
 * <pre>
 * // UNA query para TODOS los clientes
 * &#64;Query("SELECT new ClientContactPairDto(cl.idClient, c) " +
 *         "FROM ClientContact cc " +
 *         "WHERE cl.idClient IN :clientIds")
 * List<ClientContactPairDto> findClientContactsActiveByClientIds(List<Long> clientIds);
 * </pre>
 *
 * <p>Este DTO permite a JPQL devolver pares (clientId, contact) que luego se agrupan en memoria
 * para asignar contactos a cada cliente sin queries adicionales.
 *
 * @see com.wallparisoft.ebill.customer.repository.IContactRepo#findClientContactsActiveByClientIds
 * @see
 *     com.wallparisoft.ebill.customer.service.impl.ClientServiceImpl#findClientByIdentificationOrNameOrType
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientContactPairDto {
  /** ID del cliente al que pertenece el contacto */
  private Long clientId;

  /** Entidad Contact asociada */
  private Contact contact;
}
