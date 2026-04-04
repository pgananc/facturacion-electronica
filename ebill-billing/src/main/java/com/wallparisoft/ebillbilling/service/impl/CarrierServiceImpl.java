package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.CarrierDto;
import com.wallparisoft.ebillbilling.entity.Carrier;
import com.wallparisoft.ebillbilling.repository.ICarrierRepo;
import com.wallparisoft.ebillbilling.service.ICarrierService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarrierServiceImpl implements ICarrierService {

  private final ICarrierRepo carrierRepo;

  @Override
  @Transactional(readOnly = true)
  public List<CarrierDto> getCarriersByCompany(Long idCompany) {
    log.debug("Obteniendo transportistas para empresa: {}", idCompany);
    List<Carrier> carriers = carrierRepo.findByIdCompany(idCompany);
    return carriers.stream().map(this::toDto).collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public List<CarrierDto> getActiveCarriersByCompany(Long idCompany) {
    log.debug("Obteniendo transportistas activos para empresa: {}", idCompany);
    List<Carrier> carriers = carrierRepo.findByIdCompanyAndStatusTrue(idCompany);
    return carriers.stream().map(this::toDto).collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CarrierDto> getCarriersByCompany(Long idCompany, Pageable pageable) {
    log.debug("Obteniendo transportistas paginados para empresa: {}", idCompany);
    Page<Carrier> carriersPage = carrierRepo.findByIdCompany(idCompany, pageable);
    return carriersPage.map(this::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CarrierDto> getActiveCarriersByCompany(Long idCompany, Pageable pageable) {
    log.debug("Obteniendo transportistas activos paginados para empresa: {}", idCompany);
    Page<Carrier> carriersPage = carrierRepo.findByIdCompanyAndStatusTrue(idCompany, pageable);
    return carriersPage.map(this::toDto);
  }

  @Override
  @Transactional(readOnly = true)
  public CarrierDto getCarrierById(Long idCarrier) {
    log.debug("Obteniendo transportista por ID: {}", idCarrier);
    return carrierRepo.findById(idCarrier).map(this::toDto).orElse(null);
  }

  @Override
  @Transactional
  public CarrierDto createCarrier(CarrierDto carrierDto) {
    log.info("Creando nuevo transportista: {}", carrierDto.getName());

    // Validar que no exista otro transportista con la misma identificación en la misma empresa
    if (carrierDto.getIdCompany() != null && StringUtils.hasText(carrierDto.getIdentification())) {
      carrierRepo
          .findByIdCompanyAndIdentification(
              carrierDto.getIdCompany(), carrierDto.getIdentification())
          .ifPresent(
              existing -> {
                throw new IllegalArgumentException(
                    "Ya existe un transportista con la identificación "
                        + carrierDto.getIdentification()
                        + " para esta empresa");
              });
    }

    Carrier carrier = toEntity(carrierDto);
    carrier.setStatus(true);
    carrier.setCreationDate(LocalDateTime.now());
    carrier.setUpdateDate(LocalDateTime.now());

    Carrier saved = carrierRepo.save(carrier);
    log.info("Transportista creado con ID: {}", saved.getIdCarrier());
    return toDto(saved);
  }

  @Override
  @Transactional
  public CarrierDto updateCarrier(Long idCarrier, CarrierDto carrierDto) {
    log.info("Actualizando transportista ID: {}", idCarrier);

    Carrier existing =
        carrierRepo
            .findById(idCarrier)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Transportista no encontrado con ID: " + idCarrier));

    // Validar que si se cambia la identificación, no exista otro con la misma
    if (StringUtils.hasText(carrierDto.getIdentification())
        && !existing.getIdentification().equals(carrierDto.getIdentification())) {
      carrierRepo
          .findByIdCompanyAndIdentification(existing.getIdCompany(), carrierDto.getIdentification())
          .ifPresent(
              duplicate -> {
                if (!duplicate.getIdCarrier().equals(idCarrier)) {
                  throw new IllegalArgumentException(
                      "Ya existe otro transportista con la identificación "
                          + carrierDto.getIdentification()
                          + " para esta empresa");
                }
              });
    }

    // Actualizar campos
    if (StringUtils.hasText(carrierDto.getIdentificationType())) {
      existing.setIdentificationType(carrierDto.getIdentificationType());
    }
    if (StringUtils.hasText(carrierDto.getIdentification())) {
      existing.setIdentification(carrierDto.getIdentification());
    }
    if (StringUtils.hasText(carrierDto.getName())) {
      existing.setName(carrierDto.getName());
    }
    if (carrierDto.getAddress() != null) {
      existing.setAddress(carrierDto.getAddress());
    }
    if (carrierDto.getPhone() != null) {
      existing.setPhone(carrierDto.getPhone());
    }
    if (carrierDto.getEmail() != null) {
      existing.setEmail(carrierDto.getEmail());
    }
    if (carrierDto.getLicenseNumber() != null) {
      existing.setLicenseNumber(carrierDto.getLicenseNumber());
    }
    if (carrierDto.getVehicleType() != null) {
      existing.setVehicleType(carrierDto.getVehicleType());
    }
    if (carrierDto.getStatus() != null) {
      existing.setStatus(carrierDto.getStatus());
    }

    existing.setUpdateDate(LocalDateTime.now());

    Carrier saved = carrierRepo.save(existing);
    log.info("Transportista actualizado: {}", saved.getIdCarrier());
    return toDto(saved);
  }

  @Override
  @Transactional
  public boolean deleteCarrier(Long idCarrier) {
    log.info("Eliminando (desactivando) transportista ID: {}", idCarrier);

    Carrier carrier =
        carrierRepo
            .findById(idCarrier)
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Transportista no encontrado con ID: " + idCarrier));

    // Soft delete: desactivar en lugar de eliminar físicamente
    carrier.setStatus(false);
    carrier.setUpdateDate(LocalDateTime.now());
    carrierRepo.save(carrier);

    log.info("Transportista desactivado: {}", idCarrier);
    return true;
  }

  private CarrierDto toDto(Carrier carrier) {
    return CarrierDto.builder()
        .idCarrier(carrier.getIdCarrier())
        .idCompany(carrier.getIdCompany())
        .identificationType(carrier.getIdentificationType())
        .identification(carrier.getIdentification())
        .name(carrier.getName())
        .address(carrier.getAddress())
        .phone(carrier.getPhone())
        .email(carrier.getEmail())
        .licenseNumber(carrier.getLicenseNumber())
        .vehicleType(carrier.getVehicleType())
        .status(carrier.getStatus())
        .build();
  }

  private Carrier toEntity(CarrierDto dto) {
    return Carrier.builder()
        .idCarrier(dto.getIdCarrier())
        .idCompany(dto.getIdCompany())
        .identificationType(dto.getIdentificationType())
        .identification(dto.getIdentification())
        .name(dto.getName())
        .address(dto.getAddress())
        .phone(dto.getPhone())
        .email(dto.getEmail())
        .licenseNumber(dto.getLicenseNumber())
        .vehicleType(dto.getVehicleType())
        .build();
  }
}
