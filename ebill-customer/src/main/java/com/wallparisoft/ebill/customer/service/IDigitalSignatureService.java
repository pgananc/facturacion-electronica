package com.wallparisoft.ebill.customer.service;

import com.wallparisoft.ebill.customer.entity.DigitalSignature;
import com.wallparisoft.ebill.customer.response.CertificateInfoResponse;
import com.wallparisoft.ebill.customer.response.DigitalSignatureResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IDigitalSignatureService extends ICRUD<DigitalSignature> {

  DigitalSignatureResponse saveDigitalSignature(DigitalSignature digitalSignature);

  DigitalSignatureResponse updateDigitalSignature(DigitalSignature digitalSignature, Long id);

  DigitalSignatureResponse findByIdCompany(Long idCompany);

  public CertificateInfoResponse getCertificateInfoFromFile(MultipartFile file, String password)
      throws Exception;
}
