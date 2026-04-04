package com.wallparisoft.ebill.product.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.product.response.TaxTypeResponse;
import com.wallparisoft.ebill.product.service.ITaxTypeService;
import com.wallparisoft.ebill.utils.log.EventType;
import com.wallparisoft.ebill.utils.log.Level;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/tax-type")
@FieldDefaults(level = PRIVATE)
@RequiredArgsConstructor
@Slf4j
public class TaxTypeController {

  final ITaxTypeService taxTypeService;

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public TaxTypeResponse getTaxTypesByStatus(
      @RequestParam(value = "status", required = false, defaultValue = "true") boolean status,
      @RequestParam(value = "type", required = false) String type) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];

    // Log REQUEST
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information("Get TaxTypes by status: " + status + ", type: " + type),
        EventType.REQUEST,
        Level.LEVEL_001);

    TaxTypeResponse response;
    if (type != null && !type.isBlank()) {
      response = taxTypeService.getTaxTypesByStatusAndType(status, type.toUpperCase());
    } else {
      response = taxTypeService.getTaxTypesByStatus(status);
    }

    // Log RESPONSE
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response),
        EventType.RESPONSE,
        Level.LEVEL_001);

    return response;
  }
}
