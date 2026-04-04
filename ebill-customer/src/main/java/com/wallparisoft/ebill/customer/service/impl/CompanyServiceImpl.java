package com.wallparisoft.ebill.customer.service.impl;

import com.wallparisoft.ebill.customer.dto.CompanyDetailsDto;
import com.wallparisoft.ebill.customer.dto.CompanyDto;
import com.wallparisoft.ebill.customer.dto.ContactDto;
import com.wallparisoft.ebill.customer.dto.EstablishmentResponseDto;
import com.wallparisoft.ebill.customer.entity.Company;
import com.wallparisoft.ebill.customer.entity.CompanyContact;
import com.wallparisoft.ebill.customer.entity.Contact;
import com.wallparisoft.ebill.customer.entity.Establishment;
import com.wallparisoft.ebill.customer.mapper.CompanyMapper;
import com.wallparisoft.ebill.customer.mapper.ContactMapper;
import com.wallparisoft.ebill.customer.mapper.EstablishmentMapper;
import com.wallparisoft.ebill.customer.repository.ICompanyContactRepo;
import com.wallparisoft.ebill.customer.repository.ICompanyRepo;
import com.wallparisoft.ebill.customer.repository.IContactRepo;
import com.wallparisoft.ebill.customer.repository.IEstablishmentRepo;
import com.wallparisoft.ebill.customer.response.CompanyDetailDtoResponse;
import com.wallparisoft.ebill.customer.service.ICompanyService;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import jakarta.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class CompanyServiceImpl implements ICompanyService {

  final ICompanyRepo companyRepo;
  final IContactRepo contactRepo;
  final ICompanyContactRepo companyContactRepo;
  final IEstablishmentRepo establishmentRepo;
  final CompanyMapper companyMapper;
  final ContactMapper contactMapper;
  final EstablishmentMapper establishmentMapper;

  @Value("${company.logo.base-path:${user.home}/companies}")
  String logoBasePath;

  @Override
  @CacheEvict(
      value = {"companies", "company-detail"},
      allEntries = true)
  public Company save(Company entity) {
    return companyRepo.save(entity);
  }

  @Override
  public Company update(Company entity, Long id) {
    return null;
  }

  @Override
  public List<Company> findAll() {
    return companyRepo.findAll();
  }

  @Override
  public Company findById(Long id) {
    return this.companyRepo
        .findById(id)
        .orElseThrow(() -> new ModelNotFoundException("Company not found for this id :: " + id));
  }

  @Override
  @Transactional
  public void saveCompanyAndContact(CompanyDetailsDto companyDto) {
    /*
     * Company company = companyMapper.convertCompanyDtoToCompany(companyDto);
     * List<Contact>
     * contacts =
     * contactMapper.convertContactDtoListToContactList(companyDto.getContacts());
     * Company companySave = save(company); contacts.forEach( x -> { Contact contact
     * =
     * contactRepo.save(x); CompanyContact companyContact = CompanyContact.builder()
     * .company(companySave) .contact(contact) .status(contact.getStatus())
     * .build();
     * companyContactRepo.save(companyContact); });
     */
  }

  @Override
  @Transactional
  public void updateCompanyAndContact(CompanyDetailsDto companyDetailsDto, Long idCompany) {
    Company companySave = findById(idCompany);
    Company company = companyMapper.convertCompanyDtoToCompany(companyDetailsDto);
    List<Contact> contacts =
        contactMapper.convertContactDtoListToContactList(companyDetailsDto.getContacts());

    company.setCreationDate(companySave.getCreationDate());
    save(company);
    contacts.forEach(
        contact -> {
          Optional<CompanyContact> companyContactOptional =
              companyContactRepo.findByIdCompanyAndIdContact(
                  company.getIdCompany(), contact.getIdContact());
          if (companyContactOptional.isPresent()) {
            CompanyContact companyContact = companyContactOptional.get();
            companyContact.setStatus(contact.getStatus());
            companyContactRepo.save(companyContact);
            contact.setCreationDate(companyContact.getCreationDate());
            contactRepo.save(contact);
          }
        });
  }

  @Override
  @Transactional
  public void updateCompany(CompanyDto companyDto) {
    Company companySave = findById(companyDto.getIdCompany());
    if (companySave == null) {
      throw new ModelNotFoundException(
          "No existe id de compañía consultado: " + companyDto.getIdCompany());
    }
    Company company = companyMapper.convertCompanyDtoToCompany(companyDto);
    company.setCreationDate(companySave.getCreationDate());
    company.setUpdateDate(LocalDateTime.now());
    company.setStatus(companyDto.getStatus());
    save(company);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Company company = findById(id);
    company.setStatus(false);
    companyRepo.save(company);
  }

  @Override
  @Cacheable(value = "companies", key = "'active'", sync = true)
  public List<CompanyDetailsDto> findCompaniesActiveAndContactActive() {
    log.info("🔥 CACHE MISS - Consultando companies activas en BD");
    List<Company> companies =
        this.companyRepo.findByStatusIsTrueOrderByIdentification().orElse(new ArrayList<>());

    // Usar stream() normal en lugar de parallelStream() para evitar problemas de
    // concurrencia
    return companies.stream()
        .map(
            company -> {
              CompanyDetailsDto companyDetailsDto =
                  companyMapper.convertCompanyToCompanyDetailsDto(company);
              List<Contact> contacts =
                  contactRepo.findCompanyContactActiveByIdCompany(company.getIdCompany());
              List<ContactDto> contactsDto =
                  contactMapper.convertContactListToContactDtoList(contacts);
              companyDetailsDto.setContacts(contactsDto);
              return companyDetailsDto;
            })
        .collect(Collectors.toList());
  }

  @Override
  public Page<CompanyDetailsDto> findCompanyByIdentificationOrName(
      String identification,
      String name,
      Boolean status,
      List<Long> companiesIds,
      boolean isSuperAdmin,
      Pageable pageable) {

    // Hacer null-safe los parámetros de búsqueda
    String safeIdentification =
        identification != null && !identification.isEmpty() ? "%" + identification + "%" : "%%";
    String safeName = name != null && !name.isEmpty() ? "%" + name.toUpperCase() + "%" : "%%";

    Page<Company> companies =
        isSuperAdmin
            ? companyRepo.findCompanyByIdentificationOrName(
                safeIdentification, safeName, status, pageable)
            : companyRepo.findCompanyByIdentificationOrName(
                safeIdentification,
                safeName,
                status,
                companiesIds.isEmpty() ? null : companiesIds,
                pageable);

    // Usar stream() normal en lugar de parallelStream() para evitar problemas de
    // concurrencia
    List<CompanyDetailsDto> companyDtoList =
        companies.getContent().stream()
            .map(
                company -> {
                  CompanyDetailsDto companyDto =
                      companyMapper.convertCompanyToCompanyDetailsDto(company);

                  // Agregar contactos activos de la empresa
                  List<Contact> contacts =
                      contactRepo.findCompanyContactActiveByIdCompany(company.getIdCompany());
                  List<ContactDto> contactsDto =
                      contactMapper.convertContactListToContactDtoList(contacts);
                  companyDto.setContacts(contactsDto);

                  // Buscar el establecimiento MATRIZ y obtener su código y tipo
                  List<Establishment> establishments =
                      establishmentRepo.findByCompany_IdCompany(company.getIdCompany());
                  establishments.stream()
                      .filter(est -> "MATRIZ".equals(est.getType()))
                      .findFirst()
                      .ifPresent(
                          matriz -> {
                            companyDto.setBranchOfficeCode(matriz.getCode());
                            companyDto.setType(matriz.getType());
                          });

                  return companyDto;
                })
            .collect(Collectors.toList());

    return new PageImpl<>(companyDtoList, pageable, companies.getTotalElements());
  }

  @Override
  public boolean existsByIdentification(String identification) {
    return companyRepo.existsByIdentification(identification);
  }

  @Override
  @Transactional
  public CompanyDto saveCompany(CompanyDto companyDto) {
    Company companySave = companyMapper.convertCompanyDtoToCompany(companyDto);
    companySave.setStatus(companyDto.getStatus());
    Company savedCompany = companyRepo.save(companySave);
    // Convertir la entidad guardada de vuelta a DTO para incluir el ID generado
    return companyMapper.entityToDto(savedCompany);
  }

  @Override
  public CompanyDetailsDto findCompanyById(Long id) {
    Company company = findById(id);
    CompanyDetailsDto companyDetailsDto = companyMapper.convertCompanyToCompanyDetailsDto(company);

    // Agregar contactos activos de la empresa
    List<Contact> contacts =
        contactRepo.findCompanyContactActiveByIdCompany(company.getIdCompany());

    List<ContactDto> contactsDto = contactMapper.convertContactListToContactDtoList(contacts);
    companyDetailsDto.setContacts(contactsDto);

    return companyDetailsDto;
  }

  @Override
  public CompanyDetailDtoResponse getAllCompanies() {
    List<Company> companies = companyRepo.findAll();
    List<CompanyDetailsDto> companyDetailsDtos = new ArrayList<>();

    companies.forEach(
        company -> {
          CompanyDetailsDto companyDetailsDto =
              companyMapper.convertCompanyToCompanyDetailsDto(company);
          // Agregar contactos activos de la empresa
          List<Contact> contacts =
              contactRepo.findCompanyContactActiveByIdCompany(company.getIdCompany());
          List<ContactDto> contactsDto = contactMapper.convertContactListToContactDtoList(contacts);
          companyDetailsDto.setContacts(contactsDto);
          companyDetailsDtos.add(companyDetailsDto);
        });

    return CompanyDetailDtoResponse.builder().companyDetailsDtos(companyDetailsDtos).build();
  }

  @Override
  @Transactional
  public CompanyDetailDtoResponse getCompaniesByIdCompanies(List<Long> idCompanies) {
    // Validar que la lista no esté vacía
    if (idCompanies == null || idCompanies.isEmpty()) {
      log.warn("⚠️ Lista de IDs de compañías vacía o null");
      return CompanyDetailDtoResponse.builder().companyDetailsDtos(new ArrayList<>()).build();
    }

    try {
      log.debug("🔍 Buscando compañías con IDs: {}", idCompanies);
      List<Company> companies = companyRepo.findByIdCompanyInAndStatus(idCompanies, true);

      if (companies == null || companies.isEmpty()) {
        log.warn("⚠️ No se encontraron compañías activas con IDs: {}", idCompanies);
        return CompanyDetailDtoResponse.builder().companyDetailsDtos(new ArrayList<>()).build();
      }

      log.debug("✅ Se encontraron {} compañías", companies.size());
      List<CompanyDetailsDto> dtos = companyMapper.convertCompaniesToCompanyDetailsDtos(companies);

      // Enriquecer cada DTO con contactos y establecimientos
      for (int i = 0; i < companies.size(); i++) {
        Company company = companies.get(i);
        CompanyDetailsDto dto = dtos.get(i);

        try {
          // Agregar contactos activos de la empresa
          List<Contact> contacts =
              contactRepo.findCompanyContactActiveByIdCompany(company.getIdCompany());
          List<ContactDto> contactsDto = contactMapper.convertContactListToContactDtoList(contacts);
          dto.setContacts(contactsDto != null ? contactsDto : new ArrayList<>());
        } catch (Exception e) {
          log.error(
              "❌ Error al obtener contactos para compañía {}: {}",
              company.getIdCompany(),
              e.getMessage(),
              e);
          dto.setContacts(new ArrayList<>());
        }

        try {
          // Agregar establecimientos
          // Usar toDtoWithoutinvoiceIssuancePoints para evitar cargar colecciones lazy
          // y prevenir ConcurrentModificationException
          List<EstablishmentResponseDto> establishmentDtos =
              establishmentRepo.findByCompany_IdCompany(company.getIdCompany()).stream()
                  .map(establishmentMapper::toDtoWithoutinvoiceIssuancePoints)
                  .toList();
          dto.setEstablishments(establishmentDtos != null ? establishmentDtos : new ArrayList<>());
        } catch (Exception e) {
          log.error(
              "❌ Error al obtener establecimientos para compañía {}: {}",
              company.getIdCompany(),
              e.getMessage(),
              e);
          dto.setEstablishments(new ArrayList<>());
        }
      }

      return CompanyDetailDtoResponse.builder().companyDetailsDtos(dtos).build();
    } catch (Exception e) {
      log.error("❌ Error al obtener compañías por IDs {}: {}", idCompanies, e.getMessage(), e);
      throw new RuntimeException("Error al obtener compañías: " + e.getMessage(), e);
    }
  }

  @Override
  public String uploadLogo(Long idCompany, MultipartFile file) throws IOException {
    // Validar que la compañía existe
    Company company = findById(idCompany);
    if (company == null) {
      throw new ModelNotFoundException("Compañía no encontrada: " + idCompany);
    }

    log.info("📁 Ruta base configurada para logos: {}", logoBasePath);

    // Validar que el archivo no esté vacío
    if (file.isEmpty()) {
      throw new IllegalArgumentException("El archivo está vacío");
    }

    // Validar tipo de archivo (solo imágenes)
    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("El archivo debe ser una imagen");
    }

    // Validar tamaño del archivo (máximo 5MB)
    long maxSize = 5 * 1024 * 1024; // 5MB
    if (file.getSize() > maxSize) {
      throw new IllegalArgumentException("El archivo no debe exceder 5MB");
    }

    // Obtener extensión del archivo
    String originalFilename = file.getOriginalFilename();
    String extension = "";
    if (originalFilename != null && originalFilename.contains(".")) {
      extension = originalFilename.substring(originalFilename.lastIndexOf("."));
    } else {
      // Si no tiene extensión, usar la extensión basada en el content type
      if (contentType != null) {
        if (contentType.equals("image/jpeg") || contentType.equals("image/jpg")) {
          extension = ".jpg";
        } else if (contentType.equals("image/png")) {
          extension = ".png";
        } else if (contentType.equals("image/gif")) {
          extension = ".gif";
        } else if (contentType.equals("image/webp")) {
          extension = ".webp";
        } else {
          extension = ".jpg"; // Por defecto
        }
      }
    }

    // Crear directorio si no existe: {logoBasePath}/{idCompany}
    // Normalizar la ruta para que funcione en Windows y Linux
    String companyDir = logoBasePath + File.separator + idCompany;
    Path companyPath = Paths.get(companyDir);

    log.info("📂 Creando directorio para logo: {}", companyPath.toAbsolutePath());

    if (!Files.exists(companyPath)) {
      Files.createDirectories(companyPath);
      log.info("✅ Directorio creado exitosamente: {}", companyPath.toAbsolutePath());
    } else {
      log.info("ℹ️ Directorio ya existe: {}", companyPath.toAbsolutePath());
    }

    // Eliminar logo anterior si existe (buscar cualquier archivo que empiece con
    // "logo.")
    File dir = companyPath.toFile();
    if (dir.exists() && dir.isDirectory()) {
      File[] existingLogos = dir.listFiles((d, name) -> name.startsWith("logo."));
      if (existingLogos != null) {
        for (File existingLogo : existingLogos) {
          existingLogo.delete();
        }
      }
    }

    // Nombre del archivo: logo.{extension}
    String fileName = "logo" + extension;
    Path filePath = companyPath.resolve(fileName);

    // Guardar el archivo
    log.info("💾 Guardando archivo en: {}", filePath.toAbsolutePath());
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

    // Verificar que el archivo se guardó correctamente
    if (Files.exists(filePath)) {
      long fileSize = Files.size(filePath);
      log.info("✅ Archivo guardado exitosamente. Tamaño: {} bytes", fileSize);
    } else {
      log.error("❌ ERROR: El archivo no se guardó correctamente en: {}", filePath.toAbsolutePath());
      throw new IOException("No se pudo guardar el archivo en: " + filePath.toAbsolutePath());
    }

    // Obtener la ruta absoluta del archivo guardado
    String logoPath = filePath.toAbsolutePath().toString();
    log.info("📝 Ruta completa del logo guardado: {}", logoPath);

    // Actualizar el path del logo en la base de datos
    company.setLogoPath(logoPath);
    companyRepo.save(company);

    log.info("✅ Logo subido exitosamente para compañía {}: {}", idCompany, logoPath);

    // Retornar la ruta absoluta del archivo guardado
    return logoPath;
  }

  @Override
  public String getLogoPath(Long idCompany) {
    // Obtener el path del logo desde la base de datos
    Company company = findById(idCompany);
    if (company != null && company.getLogoPath() != null && !company.getLogoPath().isEmpty()) {
      // Verificar que el archivo existe en el sistema de archivos
      File logoFile = new File(company.getLogoPath());
      if (logoFile.exists()) {
        return company.getLogoPath();
      } else {
        log.warn("Logo path en BD existe pero archivo no encontrado: {}", company.getLogoPath());
        // Si el archivo no existe, limpiar el path en la BD
        company.setLogoPath(null);
        companyRepo.save(company);
      }
    }

    return null;
  }

  @Override
  @Transactional
  @CacheEvict(
      value = {"companies", "company-detail"},
      allEntries = true)
  public void deleteLogo(Long idCompany) {
    Company company = findById(idCompany);
    String logoPath = company.getLogoPath();
    if (logoPath != null && !logoPath.isEmpty()) {
      File logoFile = new File(logoPath);
      if (logoFile.exists()) {
        if (logoFile.delete()) {
          log.info("Archivo de logo eliminado: {}", logoPath);
        } else {
          log.warn("No se pudo eliminar el archivo de logo: {}", logoPath);
        }
      }
      company.setLogoPath(null);
      companyRepo.save(company);
      log.info("Logo eliminado para compañía {}", idCompany);
    }
  }

  @Override
  public List<CompanyDetailsDto> getCompaniesExcludingIds(List<Long> excludeIds) {
    log.info("🔍 Obteniendo empresas activas excluyendo IDs: {}", excludeIds);

    // Obtener todas las empresas activas
    List<Company> companies =
        companyRepo
            .findByStatusIsTrueOrderByIdentification()
            .orElseGet(
                () -> {
                  log.warn("⚠️ No se encontraron empresas activas");
                  return new ArrayList<>();
                });

    // Filtrar excluyendo los IDs proporcionados
    if (excludeIds != null && !excludeIds.isEmpty()) {
      companies =
          companies.stream()
              .filter(company -> !excludeIds.contains(company.getIdCompany()))
              .collect(Collectors.toList());
    }

    log.info("✅ Se encontraron {} empresas sin suscripción activa", companies.size());

    // Mapear a DTO con los contactos
    return companies.stream()
        .map(
            company -> {
              // Obtener contactos de la empresa - método no existe, lista vacía por ahora
              List<ContactDto> contactDtos = List.of();

              // Obtener establecimientos
              List<Establishment> establishments =
                  establishmentRepo.findByCompany_IdCompany(company.getIdCompany());
              List<EstablishmentResponseDto> establishmentDtos =
                  establishments.stream()
                      .map(establishmentMapper::toDto)
                      .collect(Collectors.toList());

              // Crear DTO completo
              return CompanyDetailsDto.builder()
                  .idCompany(company.getIdCompany())
                  .idType(company.getIdType())
                  .identification(company.getIdentification())
                  .name(company.getName())
                  .nameCommercial(company.getNameCommercial())
                  .forcedToAccounting(company.getForcedToAccounting())
                  .specialTaxpayerResolution(company.getSpecialTaxpayerResolution())
                  .rimpeRegisteredTaxpayer(company.getRimpeRegisteredTaxpayer())
                  .smallScaleInformalBusiness(company.getSmallScaleInformalBusiness())
                  .taxWithholdingAgent(company.getTaxWithholdingAgent())
                  .regime(company.getRegime())
                  .logoPath(company.getLogoPath())
                  .status(company.getStatus())
                  .contacts(contactDtos)
                  .establishments(establishmentDtos)
                  .build();
            })
        .collect(Collectors.toList());
  }
}
