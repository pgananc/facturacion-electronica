package com.wallparisoft.ebill.customer.service.impl;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.wallparisoft.ebill.customer.dto.EstablishmentRequestDto;
import com.wallparisoft.ebill.customer.dto.EstablishmentResponseDto;
import com.wallparisoft.ebill.customer.entity.Establishment;
import com.wallparisoft.ebill.customer.entity.InvoiceIssuancePoint;
import com.wallparisoft.ebill.customer.mapper.EstablishmentMapper;
import com.wallparisoft.ebill.customer.repository.IEstablishmentRepo;
import com.wallparisoft.ebill.customer.repository.IInvoiceIssuancePointRepo;
import com.wallparisoft.ebill.customer.service.IEstablishmentService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EstablishmentServiceImpl implements IEstablishmentService {

  private final IEstablishmentRepo establishmentRepo;
  private final IInvoiceIssuancePointRepo invoiceIssuancePointRepo;
  private final EstablishmentMapper establishmentMapper;

  /**
   * Valida que no exista otra MATRIZ activa antes de crear/actualizar.
   *
   * @param dto DTO con los datos del establecimiento
   * @param currentEstablishmentId ID del establecimiento actual (null si es creación)
   * @throws ResponseStatusException si ya existe otra MATRIZ activa
   */
  private void validateUniqueMatriz(EstablishmentRequestDto dto, Long currentEstablishmentId) {
    // Solo validar si el tipo es MATRIZ
    if (!"MATRIZ".equals(dto.getType())) {
      return;
    }

    // Buscar si ya existe una MATRIZ activa para esta compañía
    Optional<Establishment> existingMatriz =
        establishmentRepo.findByCompany_IdCompanyAndTypeAndStatusIsTrue(
            dto.getCompanyId(), "MATRIZ");

    // Si existe y NO es el mismo registro que estamos actualizando
    if (existingMatriz.isPresent()
        && !existingMatriz.get().getIdEstablishment().equals(currentEstablishmentId)) {
      throw new ResponseStatusException(
          BAD_REQUEST,
          "Ya existe una MATRIZ activa para esta compañía. "
              + "Solo puede haber una MATRIZ activa por compañía. "
              + "MATRIZ actual: '"
              + existingMatriz.get().getName()
              + "' (código: "
              + existingMatriz.get().getCode()
              + ")");
    }
  }

  @Override
  public Page<EstablishmentResponseDto> getAllEstablishmentsByCompanyId(
      Long companyId, Pageable pageable) {
    return establishmentRepo
        .findEstablishmentsByCompany_IdCompany(companyId, pageable)
        .map(
            establishmentMapper
                ::toDtoWithoutinvoiceIssuancePoints); // Usa toDtoWithoutinvoiceIssuancePoints si no
    // necesitas puntos de emisión
  }

  @Override
  public List<EstablishmentResponseDto> getAllEstablishmentsByCompanyId(Long companyId) {
    return establishmentRepo.findByCompany_IdCompany(companyId).stream()
        .map(establishmentMapper::toDtoWithoutinvoiceIssuancePoints)
        .toList();
  }

  @Override
  public Page<EstablishmentResponseDto> getActiveEstablishmentsByCompanyId(
      Long companyId, Pageable pageable) {
    return establishmentRepo
        .findEstablishmentsByCompany_IdCompanyAndStatusIsTrue(companyId, pageable)
        .map(establishmentMapper::toDtoWithoutinvoiceIssuancePoints);
  }

  @Override
  public EstablishmentResponseDto findEstablishmentByIdEstablishmentAndIdCompany(
      Long idEstablishment, Long idCompany) {
    return establishmentRepo
        .findEstablishmentByIdEstablishmentAndCompany_IdCompany(idEstablishment, idCompany)
        .map(
            establishmentMapper
                ::toDto) // o toDtoWithoutinvoiceIssuancePoints si no necesitas puntos de emisión
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Establishment not found"));
  }

  @Override
  public EstablishmentResponseDto createEstablishment(EstablishmentRequestDto dto) {
    // Validar código único por compañía
    if (establishmentRepo.existsByCodeAndCompany_IdCompany(dto.getCode(), dto.getCompanyId())) {
      throw new ResponseStatusException(
          BAD_REQUEST,
          "Ya existe una sucursal con el código " + dto.getCode() + " para esta compañía");
    }

    // Validar MATRIZ única
    validateUniqueMatriz(dto, null);

    Establishment entity = establishmentMapper.toEntity(dto);
    entity.setStatus(true); // Alta lógica por defecto
    Establishment saved = establishmentRepo.save(entity);
    return establishmentMapper.toDto(saved);
  }

  @Override
  public EstablishmentResponseDto updateEstablishment(
      Long id, Long companyId, EstablishmentRequestDto dto) {
    Establishment existing =
        establishmentRepo
            .findEstablishmentByIdEstablishmentAndCompany_IdCompany(id, companyId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Establishment not found"));

    // Validar código único si cambió
    if (!existing.getCode().equals(dto.getCode())) {
      if (establishmentRepo
          .findByCodeAndCompany_IdCompanyAndIdEstablishmentNot(dto.getCode(), companyId, id)
          .isPresent()) {
        throw new ResponseStatusException(
            BAD_REQUEST,
            "Ya existe otra sucursal con el código " + dto.getCode() + " para esta compañía");
      }
    }

    // Validar MATRIZ única (pasamos el ID actual para excluirlo)
    validateUniqueMatriz(dto, id);

    existing.setName(dto.getName());
    existing.setCode(dto.getCode());
    existing.setAddress(dto.getAddress());
    existing.setType(dto.getType());
    existing.setStatus(dto.getStatus());
    // No permitir cambiar de compañía (podría ser peligroso)
    // existing.getCompany().setIdCompany(dto.getCompanyId());

    return establishmentMapper.toDto(establishmentRepo.save(existing));
  }

  @Override
  public void deactivateEstablishment(Long id, Long companyId) {
    deactivateEstablishment(id, companyId, false);
  }

  @Override
  @Transactional
  public void deactivateEstablishment(Long id, Long companyId, boolean cascade) {
    Establishment existing =
        establishmentRepo
            .findEstablishmentByIdEstablishmentAndCompany_IdCompany(id, companyId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Establishment not found"));

    if (cascade) {
      // Desactivar todos los puntos de emisión del establecimiento
      List<InvoiceIssuancePoint> points =
          invoiceIssuancePointRepo.findByEstablishment_IdEstablishment(id);
      for (InvoiceIssuancePoint point : points) {
        point.setStatus(false);
        invoiceIssuancePointRepo.save(point);
      }
    } else {
      // Sin cascade: validar que no tenga puntos de emisión activos
      long activePoints =
          invoiceIssuancePointRepo.countByEstablishment_IdEstablishmentAndStatusIsTrue(id);
      if (activePoints > 0) {
        throw new ResponseStatusException(
            BAD_REQUEST,
            "No se puede eliminar la sucursal porque tiene "
                + activePoints
                + " punto(s) de emisión activo(s)");
      }
    }

    existing.setStatus(false);
    establishmentRepo.save(existing);
  }

  @Override
  public boolean existsByCodeAndCompany(Long companyId, String code) {
    return establishmentRepo.existsByCodeAndCompany_IdCompany(code, companyId);
  }
}
