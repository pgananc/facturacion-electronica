package com.wallparisoft.ebill.customer.controller;

import static com.wallparisoft.ebill.utils.LogUtil.setLogs;
import static com.wallparisoft.ebill.utils.log.EventType.REQUEST;
import static com.wallparisoft.ebill.utils.log.Level.LEVEL_001;
import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebill.customer.entity.Company;
import com.wallparisoft.ebill.customer.entity.DigitalSignature;
import com.wallparisoft.ebill.customer.response.CertificateInfoResponse;
import com.wallparisoft.ebill.customer.response.DigitalSignatureResponse;
import com.wallparisoft.ebill.customer.service.IDigitalSignatureService;
import com.wallparisoft.ebill.utils.log.EventLog;
import com.wallparisoft.ebill.utils.log.EventType;
import com.wallparisoft.ebill.utils.log.Level;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/digital-signature")
@FieldDefaults(level = PRIVATE)
@Slf4j
public class DigitalSignatureController {

  final IDigitalSignatureService digitalSignatureService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DigitalSignatureResponse save(
      @RequestParam("signFile") MultipartFile signFile,
      @RequestParam("password") String password,
      @RequestParam("idCompany") Long idCompany) {

    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    DigitalSignatureResponse response = null;
    try {
      DigitalSignature digitalSignature = new DigitalSignature();
      digitalSignature.setSignFile(signFile.getBytes());
      digitalSignature.setPassword(password);
      Company company = new Company();
      company.setIdCompany(idCompany);
      digitalSignature.setCompany(company);
      digitalSignature.setStatus(true);
      digitalSignature.setFileName(signFile.getOriginalFilename());

      response = digitalSignatureService.saveDigitalSignature(digitalSignature);
      setLogs(
          com.wallparisoft.ebill.utils.log.EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(response.getDigitalSignatureDto()),
          EventType.RESPONSE,
          Level.LEVEL_001);
    } catch (Exception e) {
      log.error("Error al cargar firma digital", e);
      response = DigitalSignatureResponse.builder().build();
      response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.name());
    }
    return response;
  }

  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DigitalSignatureResponse updateDigitalSignature(
      @PathVariable Long id,
      @RequestParam(value = "signFile", required = false) MultipartFile signFile,
      @RequestParam("password") String password,
      @RequestParam("idCompany") Long idCompany) {

    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    DigitalSignatureResponse response = null;
    try {
      DigitalSignature digitalSignature = new DigitalSignature();
      if (signFile != null && !signFile.isEmpty()) {
        digitalSignature.setSignFile(signFile.getBytes());
        digitalSignature.setFileName(signFile.getOriginalFilename());
      }
      digitalSignature.setPassword(password);
      digitalSignature.setStatus(true);
      Company company = new Company();
      company.setIdCompany(idCompany);
      digitalSignature.setCompany(company);

      response = digitalSignatureService.updateDigitalSignature(digitalSignature, id);
      setLogs(
          com.wallparisoft.ebill.utils.log.EventLog.builder()
              .service(traceElement.getClassName())
              .method(traceElement.getMethodName())
              .information(response.getDigitalSignatureDto()),
          EventType.RESPONSE,
          Level.LEVEL_001);
    } catch (Exception e) {
      response = DigitalSignatureResponse.builder().build();
      response.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
      response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.name());
    }
    return response;
  }

  @GetMapping("/company/{idCompany}")
  public DigitalSignatureResponse findByCompaniesDigitalSignature(
      @PathVariable("idCompany") Long idCompany) {

    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    DigitalSignatureResponse response = null;

    response = digitalSignatureService.findByIdCompany(idCompany);
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getDigitalSignatureDto()),
        EventType.RESPONSE,
        Level.LEVEL_001);

    return response;
  }

  @PostMapping("/upload")
  public CertificateInfoResponse uploadCertificate(
      @RequestParam("file") MultipartFile file, @RequestParam("password") String password) {
    StackTraceElement traceElement = Thread.currentThread().getStackTrace()[1];
    setLogs(
        EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName()),
        REQUEST,
        LEVEL_001);
    CertificateInfoResponse response = null;
    if (file.isEmpty()) {
      return CertificateInfoResponse.builder()
          .status(HttpStatus.BAD_REQUEST.name())
          .code(HttpStatus.BAD_REQUEST.value())
          .build();
    }

    try {
      response = digitalSignatureService.getCertificateInfoFromFile(file, password);
    } catch (Exception e) {
      return CertificateInfoResponse.builder()
          .status(HttpStatus.BAD_REQUEST.name())
          .code(HttpStatus.BAD_REQUEST.value())
          .build();
    }
    setLogs(
        com.wallparisoft.ebill.utils.log.EventLog.builder()
            .service(traceElement.getClassName())
            .method(traceElement.getMethodName())
            .information(response.getCertificateInfoDto()),
        EventType.RESPONSE,
        Level.LEVEL_001);
    return response;
  }
}
