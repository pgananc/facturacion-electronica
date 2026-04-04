package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.auth.util.UtilsRedis;
import com.wallparisoft.ebill.customer.dto.EstablishmentWithIssuancePointDto;
import com.wallparisoft.ebill.customer.dto.InvoiceIssuancePointRequestDto;
import com.wallparisoft.ebill.customer.dto.InvoiceIssuancePointResponseDto;
import com.wallparisoft.ebill.customer.dto.InvoiceIssuancePointWithEstablishmentDto;
import com.wallparisoft.ebill.customer.dto.IssuancePointDto;
import com.wallparisoft.ebill.customer.entity.Establishment;
import com.wallparisoft.ebill.customer.entity.InvoiceIssuancePoint;
import com.wallparisoft.ebill.customer.mapper.InvoiceIssuancePointMapper;
import com.wallparisoft.ebill.customer.repository.IEstablishmentRepo;
import com.wallparisoft.ebill.customer.repository.IInvoiceIssuancePointRepo;
import com.wallparisoft.ebill.customer.service.IInvoiceIssuancePointService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InvoiceIssuancePointServiceImpl implements IInvoiceIssuancePointService {

  private final IInvoiceIssuancePointRepo invoiceIssuancePointRepo;
  private final IEstablishmentRepo establishmentRepo;
  private final InvoiceIssuancePointMapper invoiceIssuancePointMapper;
  private final UtilsRedis utilsRedis;

  @Override
  public Optional<InvoiceIssuancePointResponseDto> findByIdAndEstablishment(
      Long idInvoiceIssuancePoint, Long idEstablishment) {
    return invoiceIssuancePointRepo
        .findInvoiceIssuancePointByIdInvoiceIssuancePointAndEstablishment_IdEstablishment(
            idInvoiceIssuancePoint, idEstablishment)
        .map(invoiceIssuancePointMapper::toDto);
  }

  @Override
  public Page<InvoiceIssuancePointResponseDto> findAllByEstablishment(
      Long idEstablishment, Pageable pageable) {
    return invoiceIssuancePointRepo
        .findInvoiceIssuancePointsByEstablishment_IdEstablishment(idEstablishment, pageable)
        .map(invoiceIssuancePointMapper::toDto);
  }

  @Override
  public Page<InvoiceIssuancePointResponseDto> findAllActiveByEstablishment(
      Long idEstablishment, Pageable pageable) {
    return invoiceIssuancePointRepo
        .findInvoiceIssuancePointsByEstablishment_IdEstablishmentAndStatusIsTrue(
            idEstablishment, pageable)
        .map(invoiceIssuancePointMapper::toDto);
  }

  @Override
  public InvoiceIssuancePointResponseDto create(InvoiceIssuancePointRequestDto dto) {
    // Validar que el establecimiento existe y está activo
    Establishment establishment =
        establishmentRepo
            .findById(dto.getEstablishmentId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada"));

    if (!establishment.getStatus()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "No se puede crear un punto de emisión en una sucursal inactiva");
    }

    // Validar código único por sucursal
    if (invoiceIssuancePointRepo.existsByCodeAndEstablishment_IdEstablishment(
        dto.getCode(), dto.getEstablishmentId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Ya existe un punto de emisión con el código " + dto.getCode() + " para esta sucursal");
    }

    InvoiceIssuancePoint entity = invoiceIssuancePointMapper.toEntity(dto);
    entity.setStatus(true); // alta lógica por defecto
    if (dto.getSequentialInvoice() != null) {
      entity.setSequentialInvoice(dto.getSequentialInvoice());
    } else {
      entity.setSequentialInvoice(1L); // Default 1
    }
    return invoiceIssuancePointMapper.toDto(invoiceIssuancePointRepo.save(entity));
  }

  @Override
  public InvoiceIssuancePointResponseDto update(
      Long id, Long establishmentId, InvoiceIssuancePointRequestDto dto) {
    // Normalizar: Convertir 0 o negativos en null (frontend puede enviar 0 cuando
    // no quiere actualizar)
    if (dto.getSequentialInvoice() != null && dto.getSequentialInvoice() <= 0) {
      dto.setSequentialInvoice(null);
    }

    InvoiceIssuancePoint entity =
        invoiceIssuancePointRepo
            .findInvoiceIssuancePointByIdInvoiceIssuancePointAndEstablishment_IdEstablishment(
                id, establishmentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Issuance Point not found"));

    // Validar código único si cambió
    if (!entity.getCode().equals(dto.getCode())) {
      if (invoiceIssuancePointRepo
          .findByCodeAndEstablishment_IdEstablishmentAndIdInvoiceIssuancePointNot(
              dto.getCode(), establishmentId, id)
          .isPresent()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Ya existe otro punto de emisión con el código "
                + dto.getCode()
                + " para esta sucursal");
      }
    }

    // Validar que sequential no disminuya
    if (dto.getSequentialInvoice() != null
        && dto.getSequentialInvoice() < entity.getSequentialInvoice()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "El secuencial no puede ser menor al actual (" + entity.getSequentialInvoice() + ")");
    }

    entity.setCode(dto.getCode());
    if (dto.getSequentialInvoice() != null) {
      entity.setSequentialInvoice(dto.getSequentialInvoice());
    }
    if (dto.getStatus() != null) {
      entity.setStatus(dto.getStatus());
    }
    // No permitir cambiar de establecimiento (sería peligroso)
    // entity.getEstablishment().setIdEstablishment(dto.getEstablishmentId());

    return invoiceIssuancePointMapper.toDto(invoiceIssuancePointRepo.save(entity));
  }

  @Override
  public void deactivate(Long id, Long establishmentId) {
    InvoiceIssuancePoint entity =
        invoiceIssuancePointRepo
            .findInvoiceIssuancePointByIdInvoiceIssuancePointAndEstablishment_IdEstablishment(
                id, establishmentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Issuance Point not found"));

    entity.setStatus(false);
    invoiceIssuancePointRepo.save(entity);
  }

  @Override
  public List<InvoiceIssuancePointResponseDto> findAvailableByEstablishments(
      List<Long> establishmentIds) {
    List<InvoiceIssuancePoint> points =
        invoiceIssuancePointRepo.findAllActiveByEstablishments(establishmentIds);
    return points.stream().map(invoiceIssuancePointMapper::toDto).toList();
  }

  @Override
  public InvoiceIssuancePointResponseDto getCurrentSession(String userName) {

    String pointIdStr = utilsRedis.getPointSession(userName);
    Long pointId = pointIdStr != null ? Long.valueOf(pointIdStr) : null;

    if (pointId == null) {
      return null; // El frontend debe interpretar esto como "sin punto asignado"
    }

    InvoiceIssuancePoint point =
        invoiceIssuancePointRepo
            .findById(pointId)
            .orElseThrow(() -> new EntityNotFoundException("Punto no encontrado"));

    return invoiceIssuancePointMapper.toDto(point);
  }

  @Override
  public InvoiceIssuancePointResponseDto updateSequentialByCode(
      String code, Long establishmentId, Long newSequential) {
    InvoiceIssuancePoint entity =
        invoiceIssuancePointRepo
            .findByCodeAndEstablishment_IdEstablishmentAndStatusIsTrue(code, establishmentId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Punto de emisión no encontrado con código: "
                            + code
                            + " en establecimiento: "
                            + establishmentId));

    entity.setSequentialInvoice(newSequential);

    return invoiceIssuancePointMapper.toDto(invoiceIssuancePointRepo.save(entity));
  }

  @Override
  public List<InvoiceIssuancePointResponseDto> findAllByEstablishment(Long idEstablishment) {
    return invoiceIssuancePointRepo.findByEstablishment_IdEstablishment(idEstablishment).stream()
        .map(invoiceIssuancePointMapper::toDto)
        .toList();
  }

  @Override
  public List<InvoiceIssuancePointWithEstablishmentDto> findAllByCompany(Long idCompany) {
    List<InvoiceIssuancePoint> points = invoiceIssuancePointRepo.findAllByCompanyId(idCompany);

    return points.stream()
        .map(
            point ->
                InvoiceIssuancePointWithEstablishmentDto.builder()
                    .idInvoiceIssuancePoint(point.getIdInvoiceIssuancePoint())
                    .code(point.getCode())
                    .establishmentId(point.getEstablishment().getIdEstablishment())
                    .establishmentCode(point.getEstablishment().getCode())
                    .establishmentName(point.getEstablishment().getName())
                    .sequentialInvoice(point.getSequentialInvoice())
                    .status(point.getStatus())
                    .build())
        .toList();
  }

  @Override
  public boolean existsByCodeAndEstablishment(Long establishmentId, String code) {
    return invoiceIssuancePointRepo.existsByCodeAndEstablishment_IdEstablishment(
        code, establishmentId);
  }

  @Override
  public EstablishmentWithIssuancePointDto getEstablishmentWithIssuancePointById(
      Long idInvoiceIssuancePoint) {
    InvoiceIssuancePoint point =
        invoiceIssuancePointRepo
            .findById(idInvoiceIssuancePoint)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Punto de emisión no encontrado con ID: " + idInvoiceIssuancePoint));

    Establishment establishment = point.getEstablishment();

    // Si el establecimiento es SUCURSAL, obtener la dirección del establecimiento MATRIZ
    String parentAddress = null;
    if ("SUCURSAL".equals(establishment.getType())) {
      Optional<Establishment> matriz =
          establishmentRepo.findByCompany_IdCompanyAndTypeAndStatusIsTrue(
              establishment.getCompany().getIdCompany(), "MATRIZ");
      if (matriz.isPresent()) {
        parentAddress = matriz.get().getAddress();
      }
    }

    // Construir IssuancePointDto
    IssuancePointDto issuancePointDto =
        IssuancePointDto.builder()
            .idIssuancePoint(point.getIdInvoiceIssuancePoint())
            .code(point.getCode())
            .sequentialInvoice(
                point.getSequentialInvoice() != null
                    ? String.format("%09d", point.getSequentialInvoice())
                    : null)
            .build();

    // Construir EstablishmentWithIssuancePointDto
    return EstablishmentWithIssuancePointDto.builder()
        .idEstablishment(establishment.getIdEstablishment())
        .name(establishment.getName())
        .code(establishment.getCode())
        .address(establishment.getAddress())
        .type(establishment.getType())
        .parentAddress(parentAddress)
        .issuancePoint(issuancePointDto)
        .build();
  }
}
