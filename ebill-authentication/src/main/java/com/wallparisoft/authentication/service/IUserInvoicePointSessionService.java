package com.wallparisoft.authentication.service;

import com.wallparisoft.authentication.dto.InvoiceIssuancePointDto;
import com.wallparisoft.authentication.entity.UserInvoicePointSession;
import java.util.List;
import java.util.Optional;

public interface IUserInvoicePointSessionService {

  void assignInvoiceIssuancePointToUser(String userName, Long idInvoiceIssuancePoint);

  Optional<UserInvoicePointSession> findActiveSessionByUser(Long idUser);

  void releaseActiveSession(Long idUser);

  List<Long> getActiveInvoicePointIds();

  List<InvoiceIssuancePointDto> findAvailablePointsByEstablishments(List<Long> idEstablishments);

  InvoiceIssuancePointDto getCurrentSession(String userName);

  /**
   * Actualiza la última actividad de la sesión activa del usuario (heartbeat).
   *
   * <p>Este método debe ser llamado periódicamente por el cliente para mantener la sesión viva y
   * evitar que sea liberada automáticamente por inactividad.
   *
   * @param userName Nombre de usuario
   * @return true si se actualizó correctamente, false si no hay sesión activa
   */
  boolean updateSessionHeartbeat(String userName);
}
