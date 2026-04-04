package com.wallparisoft.ebillbilling.mapper;

import static lombok.AccessLevel.PRIVATE;

import com.wallparisoft.ebillbilling.dto.CarrierDto;
import com.wallparisoft.ebillbilling.dto.ClientDto;
import com.wallparisoft.ebillbilling.dto.CompanyDto;
import com.wallparisoft.ebillbilling.dto.GuideDetailDto;
import com.wallparisoft.ebillbilling.dto.GuideDocumentReferenceDto;
import com.wallparisoft.ebillbilling.dto.GuideDto;
import com.wallparisoft.ebillbilling.entity.Guide;
import com.wallparisoft.ebillbilling.entity.GuideDetail;
import com.wallparisoft.ebillbilling.entity.GuideDocumentReference;
import com.wallparisoft.ebillbilling.response.GuideResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE)
public class GuideServiceMapper {

  public Guide toGuide(GuideDto guideDto) {
    // Extract idCompany
    Long idCompany =
        guideDto.getIdCompany() != null
            ? guideDto.getIdCompany()
            : (guideDto.getCompanyDto() != null ? guideDto.getCompanyDto().getIdCompany() : null);

    // Extract idClient
    Long idClient =
        guideDto.getIdClient() != null
            ? guideDto.getIdClient()
            : (guideDto.getClientDto() != null ? guideDto.getClientDto().getIdClient() : null);

    // Extract idCarrier
    Long idCarrier =
        guideDto.getIdCarrier() != null
            ? guideDto.getIdCarrier()
            : (guideDto.getCarrierDto() != null ? guideDto.getCarrierDto().getIdCarrier() : null);

    // Extract idEstablishment
    Long idEstablishment =
        guideDto.getEstablishment() != null
            ? guideDto.getEstablishment().getIdEstablishment()
            : null;

    Guide guide =
        Guide.builder()
            .status(true)
            .idGuide(guideDto.getIdGuide())
            .idCompany(idCompany)
            .idClient(idClient)
            .idIssuancePoint(guideDto.getIdIssuancePoint())
            .idEstablishment(idEstablishment)
            .sequential(guideDto.getSequential())
            .guideNumber(guideDto.getGuideNumber())
            .additionalInformation(guideDto.getAdditionalInformation())
            .environment(guideDto.getEnvironment())
            // Campos específicos de guía
            .idCarrier(idCarrier)
            .transportType(guideDto.getTransportType())
            .transportPlate(guideDto.getTransportPlate())
            .originAddress(guideDto.getOriginAddress())
            .destinationAddress(guideDto.getDestinationAddress())
            .reasonForTransfer(guideDto.getReasonForTransfer())
            .route(guideDto.getRoute())
            // Convertir LocalDate a LocalDateTime
            .issueDate(
                guideDto.getIssueDate() != null
                    ? guideDto.getIssueDate().atStartOfDay()
                    : LocalDateTime.now())
            .startDate(
                guideDto.getStartDate() != null
                    ? guideDto.getStartDate().atStartOfDay()
                    : LocalDateTime.now())
            .endDate(
                guideDto.getEndDate() != null
                    ? guideDto.getEndDate().atStartOfDay()
                    : LocalDateTime.now())
            .build();

    // Mapear detalles
    if (guideDto.getGuideDetails() != null && !guideDto.getGuideDetails().isEmpty()) {
      List<GuideDetail> details =
          guideDto.getGuideDetails().stream()
              .map(
                  detailDto ->
                      GuideDetail.builder()
                          .idGuideDetail(detailDto.getIdGuideDetail())
                          .guide(guide)
                          .idProduct(detailDto.getIdProduct())
                          .description(detailDto.getDescription())
                          .quantity(detailDto.getQuantity())
                          .unit(detailDto.getUnit())
                          .weight(detailDto.getWeight())
                          .additionalInfo(detailDto.getAdditionalInfo())
                          .status(true)
                          .build())
              .collect(Collectors.toList());
      guide.setGuideDetails(details);
    }

    // Mapear referencias de documentos
    if (guideDto.getDocumentReferences() != null && !guideDto.getDocumentReferences().isEmpty()) {
      List<GuideDocumentReference> references =
          guideDto.getDocumentReferences().stream()
              .map(
                  refDto ->
                      GuideDocumentReference.builder()
                          .idGuideDocumentReference(refDto.getIdGuideDocumentReference())
                          .guide(guide)
                          .documentType(refDto.getDocumentType())
                          .documentNumber(refDto.getDocumentNumber())
                          .accessKey(refDto.getAccessKey())
                          .issueDate(
                              refDto.getIssueDate() != null
                                  ? refDto.getIssueDate()
                                  : LocalDateTime.now())
                          .status(true)
                          .build())
              .collect(Collectors.toList());
      guide.setDocumentReferences(references);
    }

    return guide;
  }

  public GuideDto toGuideDto(Guide guide) {
    if (guide == null) {
      return null;
    }

    GuideDto.GuideDtoBuilder builder =
        GuideDto.builder()
            .idGuide(guide.getIdGuide())
            .idCompany(guide.getIdCompany())
            .idClient(guide.getIdClient())
            .idEstablishment(guide.getIdEstablishment())
            .idIssuancePoint(guide.getIdIssuancePoint())
            .sequential(guide.getSequential())
            .guideNumber(guide.getGuideNumber())
            .additionalInformation(guide.getAdditionalInformation())
            .environment(guide.getEnvironment())
            .status(guide.getStatus() != null ? guide.getStatus().toString() : null)
            .emailSent(guide.getEmailSent())
            .emailSentDate(guide.getEmailSentDate())
            // Campos específicos de guía
            .idCarrier(guide.getIdCarrier())
            .transportType(guide.getTransportType())
            .transportPlate(guide.getTransportPlate())
            .originAddress(guide.getOriginAddress())
            .destinationAddress(guide.getDestinationAddress())
            .reasonForTransfer(guide.getReasonForTransfer())
            .route(guide.getRoute())
            // Convertir LocalDateTime a LocalDate
            .issueDate(
                guide.getIssueDate() != null ? guide.getIssueDate().toLocalDate() : LocalDate.now())
            .startDate(
                guide.getStartDate() != null ? guide.getStartDate().toLocalDate() : LocalDate.now())
            .endDate(
                guide.getEndDate() != null ? guide.getEndDate().toLocalDate() : LocalDate.now())
            // Initialize @Nonnull fields to avoid NullPointerException
            .companyDto(new CompanyDto())
            .clientDto(new ClientDto())
            .carrierDto(new CarrierDto());

    // Incluir datos de autorización si existen
    if (guide.getAuthorizationSri() != null) {
      builder
          .accessKey(guide.getAuthorizationSri().getAccessKey())
          .authorizationNumber(guide.getAuthorizationSri().getAuthorizationNumber())
          .authorizationDate(guide.getAuthorizationSri().getAuthorizationDate());
    }

    // Mapear detalles
    if (guide.getGuideDetails() != null && !guide.getGuideDetails().isEmpty()) {
      List<GuideDetailDto> details =
          guide.getGuideDetails().stream()
              .map(
                  detail ->
                      GuideDetailDto.builder()
                          .idGuideDetail(detail.getIdGuideDetail())
                          .idGuide(
                              detail.getGuide() != null ? detail.getGuide().getIdGuide() : null)
                          .idProduct(detail.getIdProduct())
                          .description(detail.getDescription())
                          .quantity(detail.getQuantity())
                          .unit(detail.getUnit())
                          .weight(detail.getWeight())
                          .additionalInfo(detail.getAdditionalInfo())
                          .build())
              .collect(Collectors.toList());
      builder.guideDetails(details);
    }

    // Mapear referencias de documentos
    if (guide.getDocumentReferences() != null && !guide.getDocumentReferences().isEmpty()) {
      List<GuideDocumentReferenceDto> references =
          guide.getDocumentReferences().stream()
              .map(
                  ref ->
                      GuideDocumentReferenceDto.builder()
                          .idGuideDocumentReference(ref.getIdGuideDocumentReference())
                          .idGuide(ref.getGuide() != null ? ref.getGuide().getIdGuide() : null)
                          .documentType(ref.getDocumentType())
                          .documentNumber(ref.getDocumentNumber())
                          .accessKey(ref.getAccessKey())
                          .issueDate(ref.getIssueDate())
                          .build())
              .collect(Collectors.toList());
      builder.documentReferences(references);
    }

    return builder.build();
  }

  public GuideResponse toGuideResponse(Guide guide) {
    GuideResponse response =
        GuideResponse.builder()
            .idGuide(guide.getIdGuide())
            .guideNumber(guide.getGuideNumber())
            .build();

    // Incluir datos de autorización si existen
    if (guide.getAuthorizationSri() != null) {
      response.setAccessKey(guide.getAuthorizationSri().getAccessKey());
      response.setAuthorizationNumber(guide.getAuthorizationSri().getAuthorizationNumber());
      response.setAuthorizationDate(guide.getAuthorizationSri().getAuthorizationDate());
      response.setReceptionStatusSri(guide.getAuthorizationSri().getReceptionStatusSri());
      response.setAuthorizationStatusSri(guide.getAuthorizationSri().getAuthorizationStatusSri());
      response.setStoragePath(guide.getAuthorizationSri().getAuthorizationStoragePath());

      GuideDto dto =
          GuideDto.builder()
              .idGuide(guide.getIdGuide())
              .sequential(guide.getSequential())
              .guideNumber(guide.getGuideNumber())
              .accessKey(guide.getAuthorizationSri().getAccessKey())
              .authorizationNumber(guide.getAuthorizationSri().getAuthorizationNumber())
              .companyDto(new CompanyDto())
              .clientDto(new ClientDto())
              .carrierDto(new CarrierDto())
              .originAddress(guide.getOriginAddress() != null ? guide.getOriginAddress() : "S/N")
              .destinationAddress(
                  guide.getDestinationAddress() != null ? guide.getDestinationAddress() : "S/N")
              .build();
      response.setGuideDto(dto);
    }

    return response;
  }
}
