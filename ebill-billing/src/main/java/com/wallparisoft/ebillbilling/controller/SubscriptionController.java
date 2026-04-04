package com.wallparisoft.ebillbilling.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wallparisoft.ebill.utils.log.EventType;
import com.wallparisoft.ebill.utils.log.Level;
import com.wallparisoft.ebillbilling.dto.subscription.CanEmitRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.CanEmitResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.SubscriptionResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.UpgradeRequestDto;
import com.wallparisoft.ebillbilling.dto.subscription.UpgradeResponseDto;
import com.wallparisoft.ebillbilling.dto.subscription.UsageResponseDto;
import com.wallparisoft.ebillbilling.service.ISubscriptionService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscription")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class SubscriptionController {

  final ISubscriptionService subscriptionService;
  final ObjectMapper objectMapper;

  public SubscriptionController(
      ISubscriptionService subscriptionService, ObjectMapper objectMapper) {
    this.subscriptionService = subscriptionService;
    this.objectMapper = objectMapper;
    // Asegurar que el módulo JSR310 esté registrado para LocalDateTime
    this.objectMapper.registerModule(new JavaTimeModule());
  }

  /**
   * Obtiene la suscripción actual de una compañía.
   *
   * @param idCompany ID de la compañía
   * @return Datos de la suscripción actual con plan, fechas, uso y límites
   */
  @GetMapping("/{idCompany}/current")
  public ResponseEntity<?> getCurrentSubscription(@PathVariable("idCompany") Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Obteniendo suscripción actual de company: " + idCompany),
        EventType.REQUEST,
        Level.LEVEL_001);

    try {
      SubscriptionResponseDto response = subscriptionService.getCurrentSubscription(idCompany);

      // Log de respuesta completa en JSON
      try {
        String responseJson =
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        log.info(
            "📦 [RESPONSE COMPLETA] GET /api/subscription/{}/current\n{}", idCompany, responseJson);
      } catch (Exception e) {
        log.warn("⚠️ No se pudo serializar la respuesta a JSON: {}", e.getMessage());
      }

      setLogs(
          com.wallparisoft.ebill.utils.log.EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Suscripción obtenida: " + response.getPlan().getName()),
          EventType.RESPONSE,
          Level.LEVEL_001);

      return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (IllegalArgumentException e) {
      log.error("❌ Error obteniendo suscripción: {}", e.getMessage());

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 404);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());

      return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      log.error("❌ Error inesperado obteniendo suscripción: {}", e.getMessage(), e);

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 500);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", "Error interno al obtener la suscripción");

      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Obtiene el uso mensual y anual de documentos de una compañía.
   *
   * @param idCompany ID de la compañía
   * @return Datos de uso mensual, anual e histórico
   */
  @GetMapping("/{idCompany}/usage")
  public ResponseEntity<?> getUsage(@PathVariable("idCompany") Long idCompany) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Obteniendo uso de company: " + idCompany),
        EventType.REQUEST,
        Level.LEVEL_001);

    try {
      UsageResponseDto response = subscriptionService.getUsage(idCompany);

      setLogs(
          com.wallparisoft.ebill.utils.log.EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Uso obtenido - Total anual: " + response.getCurrentYear().getTotal()),
          EventType.RESPONSE,
          Level.LEVEL_001);

      return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (IllegalArgumentException e) {
      log.error("❌ Error obteniendo uso: {}", e.getMessage());

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 404);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());

      return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      log.error("❌ Error inesperado obteniendo uso: {}", e.getMessage(), e);

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 500);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", "Error interno al obtener el uso");

      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Verifica si una compañía puede emitir un documento específico. Valida: - Si el tipo de
   * documento está disponible en el plan - Si la suscripción no está expirada - Si no se ha
   * alcanzado el límite de documentos
   *
   * @param idCompany ID de la compañía
   * @param request Tipo de documento a validar
   * @return Resultado de la validación con detalles
   */
  @PostMapping("/{idCompany}/can-emit")
  public ResponseEntity<?> canEmit(
      @PathVariable("idCompany") Long idCompany, @RequestBody CanEmitRequestDto request) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(
                "Verificando si company "
                    + idCompany
                    + " puede emitir "
                    + request.getDocumentType()),
        EventType.REQUEST,
        Level.LEVEL_001);

    try {
      CanEmitResponseDto response = subscriptionService.canEmitDocument(idCompany, request);

      setLogs(
          com.wallparisoft.ebill.utils.log.EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Verificación completada: " + response.getMessage()),
          EventType.RESPONSE,
          Level.LEVEL_001);

      return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (IllegalArgumentException e) {
      log.error("❌ Error verificando emisión: {}", e.getMessage());

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 404);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());

      return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    } catch (Exception e) {
      log.error("❌ Error inesperado verificando emisión: {}", e.getMessage(), e);

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 500);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", "Error interno al verificar la emisión");

      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Actualiza el plan de suscripción de una compañía. Requiere rol ADMIN o SUPER_ADMIN. Actualiza:
   * - Plan de suscripción - Fecha de expiración (+1 año) - Información de pago
   *
   * @param idCompany ID de la compañía
   * @param request Datos del nuevo plan y pago
   * @return Resultado de la actualización
   */
  @PostMapping("/{idCompany}/upgrade")
  public ResponseEntity<?> upgradePlan(
      @PathVariable("idCompany") Long idCompany, @Valid @RequestBody UpgradeRequestDto request) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(
                "Actualizando plan de company " + idCompany + " a " + request.getNewPlanCode()),
        EventType.REQUEST,
        Level.LEVEL_001);

    try {
      UpgradeResponseDto response = subscriptionService.upgradePlan(idCompany, request);

      if (!response.getSuccess()) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("code", 403);
        errorResponse.put("status", "ERROR");
        errorResponse.put("message", response.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
      }

      setLogs(
          com.wallparisoft.ebill.utils.log.EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information("Plan actualizado exitosamente"),
          EventType.RESPONSE,
          Level.LEVEL_001);

      return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (IllegalArgumentException e) {
      log.error("❌ Error actualizando plan: {}", e.getMessage());

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 400);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", e.getMessage());

      return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    } catch (Exception e) {
      log.error("❌ Error inesperado actualizando plan: {}", e.getMessage(), e);

      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("code", 500);
      errorResponse.put("status", "ERROR");
      errorResponse.put("message", "Error interno al actualizar el plan");

      return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
