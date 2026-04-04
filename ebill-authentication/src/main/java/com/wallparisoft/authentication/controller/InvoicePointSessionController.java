package com.wallparisoft.authentication.controller;

import com.wallparisoft.authentication.dto.AssignInvoicePointRequestDto;
import com.wallparisoft.authentication.dto.InvoiceIssuancePointDto;
import com.wallparisoft.authentication.service.IUserInvoicePointSessionService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoice-points/session")
@RequiredArgsConstructor
@Slf4j
public class InvoicePointSessionController {

  private final IUserInvoicePointSessionService sessionService;

  @PostMapping
  public ResponseEntity<Void> assignInvoicePoint(
      @RequestBody AssignInvoicePointRequestDto request, Principal principal) {
    String username = principal.getName();
    sessionService.assignInvoiceIssuancePointToUser(username, request.getIdInvoiceIssuancePoint());
    return ResponseEntity.ok().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> releaseInvoicePoint(Principal principal) {
    Long idUser = Long.valueOf(principal.getName());
    sessionService.releaseActiveSession(idUser);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/active")
  public ResponseEntity<List<Long>> getActiveInvoicePoints() {
    List<Long> activePointIds = sessionService.getActiveInvoicePointIds();
    return ResponseEntity.ok(activePointIds);
  }

  @GetMapping("/available")
  public ResponseEntity<List<InvoiceIssuancePointDto>> getAvailableInvoicePoints(
      @RequestParam("idEstablishments") List<Long> idEstablishments) {
    log.info("Buscando puntos disponibles para sucursales: {}", idEstablishments);
    List<InvoiceIssuancePointDto> available =
        sessionService.findAvailablePointsByEstablishments(idEstablishments);
    return ResponseEntity.ok(available);
  }

  @GetMapping("/current-session")
  public ResponseEntity<InvoiceIssuancePointDto> getCurrentSession(@RequestParam String userName) {
    return ResponseEntity.ok(sessionService.getCurrentSession(userName));
  }

  /**
   * Endpoint de heartbeat para mantener la sesión activa.
   *
   * <p>El cliente debe llamar este endpoint periódicamente (ej: cada 2-3 minutos) para indicar que
   * sigue activo y evitar que su sesión sea liberada automáticamente.
   *
   * @param principal Usuario autenticado
   * @return 200 OK si se actualizó, 404 si no hay sesión activa
   */
  @PostMapping("/heartbeat")
  public ResponseEntity<Void> heartbeat(Principal principal) {
    String username = principal.getName();
    boolean updated = sessionService.updateSessionHeartbeat(username);

    if (updated) {
      log.debug("💓 Heartbeat recibido y procesado exitosamente para usuario: {}", username);
      return ResponseEntity.ok().build();
    } else {
      log.warn(
          "⚠️  Heartbeat recibido pero no hay sesión activa para usuario: {}. "
              + "Posibles causas: sesión expirada, limpieza automática, o nunca se asignó punto de emisión.",
          username);
      return ResponseEntity.notFound().build();
    }
  }
}
