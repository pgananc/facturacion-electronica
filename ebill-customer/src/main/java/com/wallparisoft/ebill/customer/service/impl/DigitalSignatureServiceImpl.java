package com.wallparisoft.ebill.customer.service.impl;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.bill.aes.util.AESUtil;
import com.wallparisoft.ebill.customer.dto.CertificateInfoDto;
import com.wallparisoft.ebill.customer.entity.DigitalSignature;
import com.wallparisoft.ebill.customer.mapper.DigitalSignatureServiceMapper;
import com.wallparisoft.ebill.customer.repository.IDigitalSignatureRepo;
import com.wallparisoft.ebill.customer.response.CertificateInfoResponse;
import com.wallparisoft.ebill.customer.response.DigitalSignatureResponse;
import com.wallparisoft.ebill.customer.service.IDigitalSignatureService;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = PRIVATE)
@Slf4j
public class DigitalSignatureServiceImpl implements IDigitalSignatureService {

  @Value("${crypto.master-password}")
  private String masterPassword;

  final IDigitalSignatureRepo digitalSignatureRepo;
  final DigitalSignatureServiceMapper digitalSignatureService;

  @Override
  public DigitalSignature save(DigitalSignature entity) {
    return digitalSignatureRepo.save(entity);
  }

  @Override
  public DigitalSignature update(DigitalSignature digitalSignatureDetails, Long id) {
    return digitalSignatureRepo
        .findById(id)
        .map(
            existingSignature -> {
              existingSignature.setStatus(Boolean.FALSE);
              existingSignature.setUpdateDate(LocalDateTime.now());
              return digitalSignatureRepo.save(existingSignature);
            })
        .orElseThrow(() -> new RuntimeException("DigitalSignature not found with id " + id));
  }

  @Override
  public List<DigitalSignature> findAll() {
    return digitalSignatureRepo.findAll();
  }

  @Override
  public DigitalSignature findById(Long id) {
    return this.digitalSignatureRepo
        .findById(id)
        .orElseThrow(() -> new ModelNotFoundException("Contact not found for this id :: " + id));
  }

  @Override
  public DigitalSignatureResponse saveDigitalSignature(DigitalSignature digitalSignature) {
    try {
      String password =
          AESUtil.encrypt(digitalSignature.getPassword(), masterPassword.toCharArray());
      digitalSignature.setPassword(password);
      save(digitalSignature);
    } catch (Exception e) {
      log.error("Error al carga llaves de cifrado", e);
      DigitalSignatureResponse response = DigitalSignatureResponse.builder().build();
      response.setCode(HttpStatus.BAD_REQUEST.value());
      response.setStatus(HttpStatus.BAD_GATEWAY.name());
      response.setMessage("Error al carga llaves de cifrado");
      return response;
    }

    DigitalSignatureResponse response = DigitalSignatureResponse.builder().build();
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    return response;
  }

  @Override
  public DigitalSignatureResponse updateDigitalSignature(
      DigitalSignature digitalSignature, Long id) {

    try {
      update(digitalSignature, id);
      String password;
      password = AESUtil.encrypt(digitalSignature.getPassword(), masterPassword.toCharArray());
      digitalSignature.setPassword(password);
      save(digitalSignature);
    } catch (Exception e) {
      log.error("Error al carga llaves de cifrado", e);
      DigitalSignatureResponse response = DigitalSignatureResponse.builder().build();
      response.setCode(HttpStatus.BAD_REQUEST.value());
      response.setStatus(HttpStatus.BAD_GATEWAY.name());
      response.setMessage("Error al carga llaves de cifrado");
    }

    DigitalSignatureResponse response = DigitalSignatureResponse.builder().build();
    response.setCode(HttpStatus.OK.value());
    response.setStatus(HttpStatus.OK.name());
    return response;
  }

  @Override
  public DigitalSignatureResponse findByIdCompany(Long idCompany) {
    Optional<DigitalSignature> digitalSignatures =
        digitalSignatureRepo.findByCompany_idCompanyAndStatus(idCompany, true);
    if (digitalSignatures == null) {
      throw new ModelNotFoundException("No existe id de compañía consultado: " + idCompany);
    }
    DigitalSignatureResponse response = null;

    CertificateInfoDto certificateInfoDto = null;
    if (digitalSignatures.isPresent()) {
      try (InputStream is =
          new java.io.ByteArrayInputStream(digitalSignatures.get().getSignFile())) {
        String password =
            AESUtil.decrypt(digitalSignatures.get().getPassword(), masterPassword.toCharArray());
        certificateInfoDto = getCertificateInfo(is, password);
        digitalSignatures.get().setPassword(password);
      } catch (Exception e) {
        return DigitalSignatureResponse.builder()
            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
            .build();
      }
      response =
          DigitalSignatureResponse.builder()
              .digitalSignatureDto(
                  digitalSignatureService.toDigitalSignatureDto(digitalSignatures.get()))
              .build();
      response.getDigitalSignatureDto().setCertificateInfo(certificateInfoDto);
      response.setCode(HttpStatus.OK.value());
      response.setStatus(HttpStatus.OK.name());
    } else {
      response = DigitalSignatureResponse.builder().build();
    }

    return response;
  }

  public CertificateInfoResponse getCertificateInfoFromFile(MultipartFile file, String password)
      throws Exception {

    CertificateInfoDto certificateInfoDto = getCertificateInfo(file.getInputStream(), password);
    if (certificateInfoDto == null) {
      return CertificateInfoResponse.builder()
          .code(HttpStatus.NOT_FOUND.value())
          .status(HttpStatus.NOT_FOUND.name())
          .message("Password incorrecto.")
          .certificateInfoDto(certificateInfoDto)
          .build();
    }
    return CertificateInfoResponse.builder()
        .code(HttpStatus.OK.value())
        .status(HttpStatus.OK.name())
        .certificateInfoDto(certificateInfoDto)
        .build();
  }

  private CertificateInfoDto getCertificateInfo(InputStream inputStream, String password)
      throws Exception {
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    try {
      try (InputStream is = inputStream) {
        keyStore.load(is, password.toCharArray());
      }
    } catch (IOException e) {
      return null;
    }
    Enumeration<String> aliases = keyStore.aliases();
    String alias = null;
    if (aliases.hasMoreElements()) {
      alias = aliases.nextElement();
    } else {
      throw new Exception("No se encontraron entradas en el archivo P12.");
    }

    X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

    // Obtener fecha de vigencia (fecha de expiración)
    LocalDate fechaVigencia =
        certificate.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

    // Obtener propietario (Subject DN)
    String propietario = certificate.getSubjectX500Principal().getName();

    return CertificateInfoDto.builder().effectiveDate(fechaVigencia).owner(propietario).build();
  }
}
