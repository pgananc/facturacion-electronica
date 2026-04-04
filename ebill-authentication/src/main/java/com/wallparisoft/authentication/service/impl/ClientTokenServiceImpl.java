package com.wallparisoft.authentication.service.impl;

import com.wallparisoft.authentication.client.IInformationClient;
import com.wallparisoft.authentication.dto.ClientDto;
import com.wallparisoft.authentication.dto.ClientSearchRequestDto;
import com.wallparisoft.authentication.entity.ClientToken;
import com.wallparisoft.authentication.entity.Params;
import com.wallparisoft.authentication.exception.RateLimitExceededException;
import com.wallparisoft.authentication.repository.IClientTokenRepository;
import com.wallparisoft.authentication.repository.IParamsRepository;
import com.wallparisoft.authentication.response.TokenResponse;
import com.wallparisoft.authentication.service.ClientTokenRateLimitService;
import com.wallparisoft.authentication.service.IClientTokenService;
import com.wallparisoft.authentication.service.IGenericRateLimitService;
import com.wallparisoft.authentication.service.IMailService;
import com.wallparisoft.ebill.auth.util.model.TokenInformation;
import com.wallparisoft.ebill.auth.util.model.TokenSession;
import com.wallparisoft.ebill.auth.util.service.IAuthService;
import com.wallparisoft.ebill.utils.TokenGenerator;
import com.wallparisoft.ebill.utils.enums.ParmsAuthEnum;
import com.wallparisoft.ebill.utils.exception.ModelNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ClientTokenServiceImpl implements IClientTokenService {

  private final IClientTokenRepository clientTokenRepository;
  private final IParamsRepository paramsRepository;
  private final IMailService mailService;
  private final IInformationClient iInformationClient;
  private final ClientTokenRateLimitService rateLimitService;
  private final IGenericRateLimitService genericRateLimitService;
  private final IAuthService authService;

  public ClientTokenServiceImpl(
      IClientTokenRepository clientTokenRepository,
      IParamsRepository paramsRepository,
      IMailService mailService,
      IInformationClient iInformationClient,
      ClientTokenRateLimitService rateLimitService,
      IGenericRateLimitService genericRateLimitService,
      IAuthService authService) {
    this.clientTokenRepository = clientTokenRepository;
    this.paramsRepository = paramsRepository;
    this.mailService = mailService;
    this.iInformationClient = iInformationClient;
    this.rateLimitService = rateLimitService;
    this.genericRateLimitService = genericRateLimitService;
    this.authService = authService;
  }

  @Override
  public ClientToken save(ClientToken entity) {
    return clientTokenRepository.save(entity);
  }

  @Override
  public List<ClientToken> findAll() {
    return clientTokenRepository.findAll();
  }

  @Override
  public ClientToken findById(Long id) {
    return clientTokenRepository.findById(id).orElse(null);
  }

  @Override
  public ClientToken saveClientToken(String identification, String clientEmail, String clientIp) {
    // 1. Verificar si el cliente tiene tokens activos
    boolean hasActiveTokens = clientTokenRepository.hasActiveTokens(identification);

    // 2. Si NO tiene tokens activos, limpiar rate limiting y permitir solicitud
    if (!hasActiveTokens) {
      rateLimitService.clearAttempts(clientEmail, identification, true);
    }

    // 3. Validar rate limiting por email (usando servicio genérico)
    if (!genericRateLimitService.canMakeRequest(
        "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.EMAIL, clientEmail)) {
      long secondsLeft =
          genericRateLimitService.getTimeUntilNextAttempt(
              "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.EMAIL, clientEmail);
      long minutesLeft = secondsLeft / 60;
      int currentAttempts =
          genericRateLimitService.getCurrentAttempts(
              "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.EMAIL, clientEmail);
      int maxAttempts =
          genericRateLimitService.getMaxRequests(
              "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.EMAIL);
      int windowMinutes =
          genericRateLimitService.getWindowMinutes(
              "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.EMAIL);
      String message =
          String.format(
              "Máximo de solicitudes alcanzado por email (%d/%d). Intente nuevamente en %d minutos",
              currentAttempts, maxAttempts, minutesLeft);
      throw new RateLimitExceededException(
          message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
    }

    // 4. Validar rate limiting por IP (usando servicio genérico)
    if (clientIp != null
        && !genericRateLimitService.canMakeRequest(
            "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.IP, clientIp)) {
      long secondsLeft =
          genericRateLimitService.getTimeUntilNextAttempt(
              "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
      long minutesLeft = secondsLeft / 60;
      int currentAttempts =
          genericRateLimitService.getCurrentAttempts(
              "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
      int maxAttempts =
          genericRateLimitService.getMaxRequests(
              "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.IP);
      int windowMinutes =
          genericRateLimitService.getWindowMinutes(
              "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.IP);
      String message =
          String.format(
              "Máximo de solicitudes alcanzado por IP (%d/%d). Intente nuevamente en %d minutos",
              currentAttempts, maxAttempts, minutesLeft);
      throw new RateLimitExceededException(
          message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
    }

    // 5. Validar cliente existe
    ClientSearchRequestDto client = new ClientSearchRequestDto();
    client.setEmail(clientEmail);
    client.setIdentification(identification);

    ClientDto clientDto;
    try {
      clientDto = iInformationClient.findByIdentificationAndEmail(client);
      if (clientDto == null) {
        // Incrementar contador por intento fallido para tokens activos
        rateLimitService.incrementAttempts(clientEmail, identification, true);
        throw new ModelNotFoundException("No existe información para los datos ingresados");
      }
    } catch (Exception e) {
      log.error("error", e);
      // Incrementar contador por intento fallido para tokens activos
      rateLimitService.incrementAttempts(clientEmail, identification, true);

      // Si es un error 404 del servicio de información, extraer mensaje del JSON
      if (e.getMessage() != null && e.getMessage().contains("404")) {
        throw new ModelNotFoundException("No existe información para los datos ingresados");
      }
      // Para otros errores, usar mensaje genérico
      throw new ModelNotFoundException("Error al validar los datos ingresados.");
    }

    ClientToken clientToken =
        ClientToken.builder()
            .clientNumber(identification)
            .clientEmail(clientEmail)
            .creationDate(LocalDateTime.now())
            .status(true)
            .build();

    Params paramsTime =
        paramsRepository.findParamsByCodeParam(
            ParmsAuthEnum.COD005_TIME_DURATION_TOKEN_CLIENT.getCode());
    Params lengthToken =
        paramsRepository.findParamsByCodeParam(ParmsAuthEnum.COD006_LENGTH_TOKEN.getCode());
    clientToken.setExpirationDate(Integer.parseInt(paramsTime.getValueParam()));
    String token =
        TokenGenerator.generateLetterToken(Integer.parseInt(lengthToken.getValueParam()));
    clientToken.setToken(token);
    ClientToken cliToken = clientTokenRepository.save(clientToken);
    mailService.sendMailCodeClient(
        clientDto.getName(), clientEmail, paramsTime.getValueParam(), token);

    // 6. Incrementar contadores por intento exitoso
    genericRateLimitService.incrementRequest(
        "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.EMAIL, clientEmail);
    if (clientIp != null) {
      genericRateLimitService.incrementRequest(
          "TOKEN_CLIENT", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
    }

    return cliToken;
  }

  @Override
  public ClientToken resendClientToken(String clientNumber, String clientEmail, String clientIp) {
    // 1. Verificar si el cliente tiene tokens activos
    boolean hasActiveTokens = clientTokenRepository.hasActiveTokens(clientNumber);

    // 2. Si NO tiene tokens activos, limpiar rate limiting y permitir solicitud
    if (!hasActiveTokens) {
      rateLimitService.clearAttempts(clientEmail, clientNumber, true);
    }

    // 3. Validar rate limiting por email para reenvío (usando servicio genérico)
    if (!genericRateLimitService.canMakeRequest(
        "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.EMAIL, clientEmail)) {
      long secondsLeft =
          genericRateLimitService.getTimeUntilNextAttempt(
              "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.EMAIL, clientEmail);
      long minutesLeft = secondsLeft / 60;
      int currentAttempts =
          genericRateLimitService.getCurrentAttempts(
              "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.EMAIL, clientEmail);
      int maxAttempts =
          genericRateLimitService.getMaxRequests(
              "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.EMAIL);
      int windowMinutes =
          genericRateLimitService.getWindowMinutes(
              "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.EMAIL);
      String message =
          String.format(
              "Máximo de reenvíos alcanzado por email (%d/%d). Intente nuevamente en %d minutos",
              currentAttempts, maxAttempts, minutesLeft);
      throw new RateLimitExceededException(
          message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
    }

    // 4. Validar rate limiting por IP para reenvío
    if (clientIp != null
        && !genericRateLimitService.canMakeRequest(
            "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.IP, clientIp)) {
      long secondsLeft =
          genericRateLimitService.getTimeUntilNextAttempt(
              "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
      long minutesLeft = secondsLeft / 60;
      int currentAttempts =
          genericRateLimitService.getCurrentAttempts(
              "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
      int maxAttempts =
          genericRateLimitService.getMaxRequests(
              "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.IP);
      int windowMinutes =
          genericRateLimitService.getWindowMinutes(
              "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.IP);
      String message =
          String.format(
              "Máximo de reenvíos alcanzado por IP (%d/%d). Intente nuevamente en %d minutos",
              currentAttempts, maxAttempts, minutesLeft);
      throw new RateLimitExceededException(
          message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
    }

    // 5. Validar cliente existe
    ClientSearchRequestDto client = new ClientSearchRequestDto();
    client.setEmail(clientEmail);
    client.setIdentification(clientNumber);

    ClientDto clientDto;
    try {
      clientDto = iInformationClient.findByIdentificationAndEmail(client);
      if (clientDto == null) {
        // Incrementar contador por intento fallido para tokens activos
        rateLimitService.incrementAttempts(clientEmail, clientNumber, true);
        throw new ModelNotFoundException("No existe información para los datos ingresados");
      }
    } catch (Exception e) {
      // Incrementar contador por intento fallido para tokens activos
      rateLimitService.incrementAttempts(clientEmail, clientNumber, true);

      // Si es un error 404 del servicio de información
      if (e.getMessage() != null && e.getMessage().contains("404")) {
        throw new ModelNotFoundException("No existe información para los datos ingresados");
      }

      // Para otros errores, usar mensaje genérico
      throw new ModelNotFoundException("Error al validar los datos ingresados.");
    }

    // 3. Desactivar todos los tokens activos del cliente
    List<ClientToken> activeTokens =
        clientTokenRepository.findActiveTokensByClientNumber(clientNumber);
    for (ClientToken token : activeTokens) {
      token.setStatus(false);
      token.setUsedDate(LocalDateTime.now());
      clientTokenRepository.save(token);
    }

    // 4. Crear nuevo token
    ClientToken clientToken =
        ClientToken.builder()
            .clientNumber(clientNumber)
            .clientEmail(clientEmail)
            .creationDate(LocalDateTime.now())
            .status(true)
            .build();

    Params paramsTime =
        paramsRepository.findParamsByCodeParam(
            ParmsAuthEnum.COD005_TIME_DURATION_TOKEN_CLIENT.getCode());
    Params lengthToken =
        paramsRepository.findParamsByCodeParam(ParmsAuthEnum.COD006_LENGTH_TOKEN.getCode());
    clientToken.setExpirationDate(Integer.parseInt(paramsTime.getValueParam()));
    String token =
        TokenGenerator.generateLetterToken(Integer.parseInt(lengthToken.getValueParam()));
    clientToken.setToken(token);

    ClientToken newToken = clientTokenRepository.save(clientToken);

    // 6. Enviar email con el nuevo token
    mailService.sendMailCodeClient(
        clientDto.getName(), clientEmail, paramsTime.getValueParam(), token);

    // 7. Incrementar contadores por intento exitoso
    genericRateLimitService.incrementRequest(
        "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.EMAIL, clientEmail);
    if (clientIp != null) {
      genericRateLimitService.incrementRequest(
          "TOKEN_RESEND", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
    }

    return newToken;
  }

  @Override
  public TokenResponse validateTokenAndGenerateAccessToken(
      String clientNumber, String token, String clientIp) {
    // Validar rate limiting por IP
    if (clientIp != null
        && !genericRateLimitService.canMakeRequest(
            "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.IP, clientIp)) {
      long secondsLeft =
          genericRateLimitService.getTimeUntilNextAttempt(
              "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
      long minutesLeft = secondsLeft / 60;
      int currentAttempts =
          genericRateLimitService.getCurrentAttempts(
              "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
      int maxAttempts =
          genericRateLimitService.getMaxRequests(
              "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.IP);
      int windowMinutes =
          genericRateLimitService.getWindowMinutes(
              "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.IP);
      String message =
          String.format(
              "Máximo de intentos de validación alcanzado por IP (%d/%d). Intente nuevamente en %d minutos",
              currentAttempts, maxAttempts, minutesLeft);
      throw new RateLimitExceededException(
          message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
    }

    // Validar rate limiting por cliente
    if (!genericRateLimitService.canMakeRequest(
        "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.CLIENT, clientNumber)) {
      long secondsLeft =
          genericRateLimitService.getTimeUntilNextAttempt(
              "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.CLIENT, clientNumber);
      long minutesLeft = secondsLeft / 60;
      int currentAttempts =
          genericRateLimitService.getCurrentAttempts(
              "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.CLIENT, clientNumber);
      int maxAttempts =
          genericRateLimitService.getMaxRequests(
              "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.CLIENT);
      int windowMinutes =
          genericRateLimitService.getWindowMinutes(
              "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.CLIENT);
      String message =
          String.format(
              "Máximo de intentos de validación alcanzado por cliente (%d/%d). Intente nuevamente en %d minutos",
              currentAttempts, maxAttempts, minutesLeft);
      throw new RateLimitExceededException(
          message, minutesLeft, currentAttempts, maxAttempts, windowMinutes);
    }

    Optional<ClientToken> tokenOpt =
        clientTokenRepository.findValidTokenByClientNumberAndToken(clientNumber, token);

    if (tokenOpt.isPresent()) {
      ClientToken clientToken = tokenOpt.get();

      // Verificar si el token no ha expirado
      if (clientToken.isExpired()) {
        // Si el token es válido, marcarlo como usado automáticamente
        clientToken.markAsUsed(); // Esto cambia status a false y establece usedDate
        clientTokenRepository.save(clientToken);

        // DECREMENTAR contador de rate limiting ya que se consumió un intento exitoso
        rateLimitService.decrementAttempts(clientToken.getClientEmail(), clientNumber, true);

        // GENERAR TOKEN DE ACCESO JWT
        TokenSession accessToken = generateClientAccessToken(clientToken);

        return TokenResponse.builder()
            .status(HttpStatus.OK.getReasonPhrase())
            .code(1)
            .token(accessToken)
            .build();
      }
    }

    // Si llegamos aquí, el token no es válido - incrementar contadores de rate limiting
    if (clientIp != null) {
      genericRateLimitService.incrementRequest(
          "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.IP, clientIp);
    }
    genericRateLimitService.incrementRequest(
        "TOKEN_VALIDATE", IGenericRateLimitService.RateLimitDimension.CLIENT, clientNumber);

    return TokenResponse.builder()
        .status(HttpStatus.OK.getReasonPhrase())
        .token(null)
        .code(0)
        .build();
  }

  /** Genera un token de acceso JWT para el cliente */
  private TokenSession generateClientAccessToken(ClientToken clientToken) {
    // Crear TokenInformation para el cliente
    TokenInformation informationEntity = new TokenInformation();
    informationEntity.setUserName(
        "CLIENT_" + clientToken.getClientNumber()); // Usuario virtual para cliente
    informationEntity.setRoles(List.of("CLIENT")); // Rol específico para clientes
    informationEntity.setCompanyEstablishments(
        new ArrayList<>()); // Sin establecimientos para clientes

    // Generar token JWT usando el mismo servicio que el login
    return authService.generateToken(informationEntity, Boolean.FALSE);
  }
}
