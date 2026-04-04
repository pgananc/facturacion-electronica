package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.entity.Params;
import com.wallparisoft.ebillbilling.event.CronConfigUpdatedEvent;
import com.wallparisoft.ebillbilling.repository.IParamsRepo;
import com.wallparisoft.ebillbilling.service.ParamsService;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParamsServiceImpl implements ParamsService {

  private static final String DEFAULT_BASE_DIR = "/home/invoices";
  private static final String DEFAULT_CREDIT_NOTE_BASE_DIR = "/opt/ebilling/credit_notes";
  private static final String DEFAULT_DEBIT_NOTE_BASE_DIR = "/opt/ebilling/debit_notes";
  private static final String DEFAULT_GUIDE_BASE_DIR = "/opt/ebilling/guides";
  private static final String PARAM_STORAGE_BASE_DIR = "INVOICE_STORAGE_BASE_DIR";
  private static final String PARAM_CREDIT_NOTE_STORAGE_BASE_DIR = "CREDIT_NOTE_STORAGE_BASE_DIR";
  private static final String PARAM_DEBIT_NOTE_STORAGE_BASE_DIR = "DEBIT_NOTE_STORAGE_BASE_DIR";
  private static final String PARAM_GUIDE_STORAGE_BASE_DIR = "GUIDE_STORAGE_BASE_DIR";
  private static final String CRON_TASK_EXPRESSION_PARAM = "CRON_TASK_EXPRESSION";
  private static final String CRON_TASK_ENABLED_PARAM = "CRON_TASK_ENABLED";
  private static final String CACHE_PREFIX = "params_cache:";
  private static final Duration CACHE_TTL = Duration.ofSeconds(30);

  private final IParamsRepo paramsRepo;
  private final ApplicationEventPublisher eventPublisher;
  private final StringRedisTemplate redisTemplate;

  @Override
  public Optional<String> getParamValue(String codeParam) {
    // Intentar desde cache primero
    try {
      String cacheKey = CACHE_PREFIX + codeParam;
      String cached = redisTemplate.opsForValue().get(cacheKey);
      if (cached != null && !cached.isBlank()) {
        return Optional.of(cached);
      }
    } catch (Exception e) {
      log.debug("Error al obtener parámetro desde cache: {}", e.getMessage());
      // Continuar con consulta a BD si cache falla
    }

    // Si no está en cache, consultar BD
    Optional<String> value =
        paramsRepo
            .findByCodeParamAndStatusTrue(codeParam)
            .map(Params::getValueParam)
            .filter(v -> v != null && !v.isBlank());

    // Guardar en cache si se encontró valor
    value.ifPresent(
        v -> {
          try {
            String cacheKey = CACHE_PREFIX + codeParam;
            redisTemplate.opsForValue().set(cacheKey, v, CACHE_TTL);
          } catch (Exception e) {
            log.debug("Error al guardar parámetro en cache: {}", e.getMessage());
            // No fallar si cache no está disponible
          }
        });

    return value;
  }

  @Override
  public String getParamValueOrDefault(String codeParam, String defaultValue) {
    return getParamValue(codeParam).orElse(defaultValue);
  }

  @Override
  @Transactional
  public void updateParamValue(String codeParam, String value) {
    paramsRepo
        .findByCodeParam(codeParam)
        .ifPresentOrElse(
            param -> {
              param.setValueParam(value);
              paramsRepo.save(param);
              log.info("Parámetro {} actualizado", codeParam);

              // Invalidar cache
              try {
                String cacheKey = CACHE_PREFIX + codeParam;
                redisTemplate.delete(cacheKey);
              } catch (Exception e) {
                log.debug("Error al invalidar cache: {}", e.getMessage());
              }

              // Publicar evento si se actualiza un parámetro relacionado con el cron
              if (CRON_TASK_EXPRESSION_PARAM.equals(codeParam)
                  || CRON_TASK_ENABLED_PARAM.equals(codeParam)) {
                eventPublisher.publishEvent(new CronConfigUpdatedEvent(this, codeParam));
                log.info(
                    "Evento de actualización de configuración cron publicado para parámetro: {}",
                    codeParam);
              }
            },
            () -> log.warn("Parámetro {} no encontrado para actualizar", codeParam));
  }

  @Override
  public String getBaseDir() {
    // Siempre consultar BD en tiempo de ejecución para permitir cambios dinámicos
    String baseDir = getParamValueOrDefault(PARAM_STORAGE_BASE_DIR, DEFAULT_BASE_DIR);
    if (baseDir == null || baseDir.isBlank()) {
      log.warn(
          "Parámetro {} no configurado en BD, usando valor por defecto: {}",
          PARAM_STORAGE_BASE_DIR,
          DEFAULT_BASE_DIR);
      return DEFAULT_BASE_DIR;
    }
    return baseDir;
  }

  @Override
  public String getCreditNoteBaseDir() {
    // Siempre consultar BD en tiempo de ejecución para permitir cambios dinámicos
    String baseDir =
        getParamValueOrDefault(PARAM_CREDIT_NOTE_STORAGE_BASE_DIR, DEFAULT_CREDIT_NOTE_BASE_DIR);
    if (baseDir == null || baseDir.isBlank()) {
      log.warn(
          "Parámetro {} no configurado en BD, usando valor por defecto: {}",
          PARAM_CREDIT_NOTE_STORAGE_BASE_DIR,
          DEFAULT_CREDIT_NOTE_BASE_DIR);
      return DEFAULT_CREDIT_NOTE_BASE_DIR;
    }
    return baseDir;
  }

  @Override
  public String getDebitNoteBaseDir() {
    // Siempre consultar BD en tiempo de ejecución para permitir cambios dinámicos
    String baseDir =
        getParamValueOrDefault(PARAM_DEBIT_NOTE_STORAGE_BASE_DIR, DEFAULT_DEBIT_NOTE_BASE_DIR);
    if (baseDir == null || baseDir.isBlank()) {
      log.warn(
          "Parámetro {} no configurado en BD, usando valor por defecto: {}",
          PARAM_DEBIT_NOTE_STORAGE_BASE_DIR,
          DEFAULT_DEBIT_NOTE_BASE_DIR);
      return DEFAULT_DEBIT_NOTE_BASE_DIR;
    }
    return baseDir;
  }

  @Override
  public String getGuideBaseDir() {
    // Siempre consultar BD en tiempo de ejecución para permitir cambios dinámicos
    String baseDir = getParamValueOrDefault(PARAM_GUIDE_STORAGE_BASE_DIR, DEFAULT_GUIDE_BASE_DIR);
    if (baseDir == null || baseDir.isBlank()) {
      log.warn(
          "Parámetro {} no configurado en BD, usando valor por defecto: {}",
          PARAM_GUIDE_STORAGE_BASE_DIR,
          DEFAULT_GUIDE_BASE_DIR);
      return DEFAULT_GUIDE_BASE_DIR;
    }
    return baseDir;
  }
}
