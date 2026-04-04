package com.wallparisoft.ebillbilling.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Deserializador flexible para LocalDateTime que acepta múltiples formatos:
 *
 * <ul>
 *   <li>yyyy-MM-dd'T'HH:mm:ss (fecha y hora)
 *   <li>yyyy-MM-dd'T'HH:mm:ss.SSS (fecha y hora con milisegundos)
 *   <li>yyyy-MM-dd (solo fecha, se convierte a LocalDateTime con hora 00:00:00)
 *   <li>Formato ISO estándar
 * </ul>
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final DateTimeFormatter DATE_TIME_MILLIS_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  @Override
  public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String value = p.getText();
    if (value == null || value.trim().isEmpty()) {
      return null;
    }

    // Intentar parsear como LocalDateTime con milisegundos
    try {
      return LocalDateTime.parse(value, DATE_TIME_MILLIS_FORMATTER);
    } catch (DateTimeParseException e1) {
      // Intentar parsear como LocalDateTime sin milisegundos
      try {
        return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
      } catch (DateTimeParseException e2) {
        // Intentar parsear como LocalDate (solo fecha)
        try {
          LocalDate date = LocalDate.parse(value, DATE_FORMATTER);
          return date.atStartOfDay(); // Convertir a LocalDateTime con hora 00:00:00
        } catch (DateTimeParseException e3) {
          // Si también falla, intentar con el formato ISO estándar de LocalDateTime
          try {
            return LocalDateTime.parse(value);
          } catch (DateTimeParseException e4) {
            throw new IOException(
                "No se pudo parsear la fecha: "
                    + value
                    + ". Formatos aceptados: yyyy-MM-dd'T'HH:mm:ss, yyyy-MM-dd'T'HH:mm:ss.SSS, o yyyy-MM-dd",
                e4);
          }
        }
      }
    }
  }
}
