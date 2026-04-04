package com.wallparisoft.ebillbilling.service.impl;

import com.wallparisoft.ebillbilling.dto.DigitalSignatureDto;
import com.wallparisoft.ebillbilling.dto.GuideDto;
import com.wallparisoft.ebillbilling.entity.AuthorizationSri;
import com.wallparisoft.ebillbilling.repository.IAuthorizationRepo;
import com.wallparisoft.ebillbilling.service.ISendGuideService;
import com.wallparisoft.ebillbilling.service.SriXmlStorageService;
import com.wallparisoft.integration.sri.model.GuiaRemision;
import com.wallparisoft.integration.sri.util.FirmaXadesBesUtil;
import com.wallparisoft.integration.sri.util.UtilsDate;
import com.wallparisoft.integration.sri.util.XmlUtil;
import com.wallparisoft.integration.sri.ws.autorizacion.Autorizacion;
import com.wallparisoft.integration.sri.ws.autorizacion.AutorizacionComprobantes;
import com.wallparisoft.integration.sri.ws.autorizacion.AutorizacionComprobantesService;
import com.wallparisoft.integration.sri.ws.autorizacion.Mensaje;
import com.wallparisoft.integration.sri.ws.autorizacion.RespuestaComprobante;
import com.wallparisoft.integration.sri.ws.recepcion.Comprobante;
import com.wallparisoft.integration.sri.ws.recepcion.RecepcionComprobantes;
import com.wallparisoft.integration.sri.ws.recepcion.RecepcionComprobantesService;
import com.wallparisoft.integration.sri.ws.recepcion.RespuestaSolicitud;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.BindingProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.time.LocalDateTime;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendGuideImpl implements ISendGuideService {

  private final GenerateGuideSri generateGuideSri;
  private final IAuthorizationRepo authorizationRepo;
  private final SriXmlStorageService sriXmlStorageService;

  @Override
  public AuthorizationSri sendGuideSri(
      GuideDto guideDto,
      DigitalSignatureDto digitalSignatureDto,
      AuthorizationSri existingAuthorizationSri)
      throws SAXParseException,
          CertificateException,
          SAXException,
          IOException,
          JAXBException,
          InterruptedException {
    GuiaRemision guiaRemision = generateGuideSri.createGuideSri(guideDto, existingAuthorizationSri);
    ByteArrayOutputStream baosGuiaRemision =
        (new XmlUtil()).convertirObjetoAXml(GuiaRemision.class, guiaRemision);

    FirmaXadesBesUtil firmaXadesBesUtil =
        new FirmaXadesBesUtil(digitalSignatureDto.getSignFile(), digitalSignatureDto.getPassword());
    ByteArrayOutputStream baosGuiaRemisionFirmada = new ByteArrayOutputStream();
    firmaXadesBesUtil.firmarDocumento(
        new ByteArrayInputStream(baosGuiaRemision.toByteArray()), baosGuiaRemisionFirmada);

    return sendToAuthorizeBySri(guiaRemision, baosGuiaRemisionFirmada, existingAuthorizationSri);
  }

  private AuthorizationSri sendToAuthorizeBySri(
      GuiaRemision guiaRemision,
      ByteArrayOutputStream baosGuiaRemisionFirmada,
      AuthorizationSri existingAuthorizationSri) {
    RespuestaSolicitud respuestaSolicitud = null;
    for (int i = 0; i < 5; i++) {
      RecepcionComprobantesService webServiceRecepcion = new RecepcionComprobantesService();
      RecepcionComprobantes port1 = webServiceRecepcion.getRecepcionComprobantesPort();
      ((BindingProvider) port1)
          .getRequestContext()
          .put("com.sun.xml.internal.ws.connect.timeout", 9999);
      ((BindingProvider) port1)
          .getRequestContext()
          .put("com.sun.xml.internal.ws.request.timeout", 9999);

      respuestaSolicitud = port1.validarComprobante(baosGuiaRemisionFirmada.toByteArray());
      if (respuestaSolicitud != null
          && respuestaSolicitud.getEstado().compareToIgnoreCase("") != 0) {
        break;
      }
      hilo();
    }

    String accessKey = guiaRemision.getInfoTributaria().getClaveAcceso();

    if (respuestaSolicitud == null) {
      AuthorizationSri authorization =
          getOrCreateAuthorization(
              existingAuthorizationSri,
              accessKey,
              null,
              null,
              "ERROR: El SRI no retornó respuesta después de 5 intentos. Posible problema de conectividad o servicio no disponible.");
      return authorizationRepo.save(authorization);
    }

    String estado = respuestaSolicitud.getEstado();
    String receptionXml = obtieneComprobantes(respuestaSolicitud);

    if (estado.compareToIgnoreCase("") != 0) {
      if (estado.compareToIgnoreCase("RECIBIDA") == 0) {
        AuthorizationSri authorization =
            getOrCreateAuthorization(
                existingAuthorizationSri, accessKey, estado, receptionXml, null);
        authorizationRepo.save(authorization);
        return authorizeXMLBySri(guiaRemision, authorization);
      } else if (estado.compareToIgnoreCase("DEVUELTA") == 0) {
        boolean isClaveRegistrada =
            receptionXml != null
                && (receptionXml.contains("CLAVE ACCESO REGISTRADA")
                    || receptionXml.contains("<identificador>43</identificador>"));

        if (isClaveRegistrada) {
          log.info(
              "🔄 [GR] Clave de acceso ya registrada en SRI, consultando autorización existente...");
          AuthorizationSri authorization =
              getOrCreateAuthorization(
                  existingAuthorizationSri, accessKey, "RECIBIDA", receptionXml, null);
          authorizationRepo.save(authorization);
          return authorizeXMLBySri(guiaRemision, authorization);
        }

        AuthorizationSri authorization =
            getOrCreateAuthorization(
                existingAuthorizationSri,
                accessKey,
                estado,
                receptionXml,
                "EL XML HA SIDO DEVUELTO");
        return authorizationRepo.save(authorization);
      }
    } else if (estado.compareToIgnoreCase("") == 0) {
      AuthorizationSri authorization =
          getOrCreateAuthorization(
              existingAuthorizationSri, accessKey, estado, receptionXml, "ERROR DE TRANSMICION");
      return authorizationRepo.save(authorization);
    }

    return existingAuthorizationSri;
  }

  private AuthorizationSri authorizeXMLBySri(
      GuiaRemision guiaRemision, AuthorizationSri authorizationSri) {
    RespuestaComprobante respuestaComprobante = null;
    for (int i = 0; i < 5; i++) {
      AutorizacionComprobantesService webServiceAutorizacion =
          new AutorizacionComprobantesService();

      AutorizacionComprobantes port = webServiceAutorizacion.getAutorizacionComprobantesPort();
      ((BindingProvider) port)
          .getRequestContext()
          .put("com.sun.xml.internal.ws.connect.timeout", 9999);
      ((BindingProvider) port)
          .getRequestContext()
          .put("com.sun.xml.internal.ws.request.timeout", 9999);

      respuestaComprobante =
          port.autorizacionComprobante(guiaRemision.getInfoTributaria().getClaveAcceso());
      if (respuestaComprobante != null
          && !respuestaComprobante.getAutorizaciones().getAutorizacion().isEmpty()) {
        break;
      }
      hilo();
    }
    if (!respuestaComprobante.getAutorizaciones().getAutorizacion().isEmpty()) {
      StringBuilder xml = new StringBuilder();
      xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<autorizacion>");
      for (Autorizacion aut : respuestaComprobante.getAutorizaciones().getAutorizacion()) {
        if (aut.getEstado().equals("AUTORIZADO")) {
          xml.append("<estado>" + aut.getEstado() + "</estado>");
          xml.append(
              "<numeroAutorizacion>" + aut.getNumeroAutorizacion() + "</numeroAutorizacion>");
          xml.append(
              "<fechaAutorizacion class=\"fechaAutorizacion\">"
                  + aut.getFechaAutorizacion()
                  + "</fechaAutorizacion>");
          xml.append("<comprobante><![CDATA[" + aut.getComprobante() + "]]></comprobante>");
          xml.append(obtenerMensajesAutorizacion(aut));
          xml.append("\n</autorizacion>");
          LocalDateTime authorizationDate = UtilsDate.toLocalDateTime(aut.getFechaAutorizacion());
          authorizationSri.setAuthorizationDate(authorizationDate);
          String formattedXml = formatXml(xml.toString());
          authorizationSri.setAuthorizationXml(formattedXml);
          // Obtener identificación del destinatario desde la sección destinatarios
          String identificacionDestinatario = "DESCONOCIDO";
          if (guiaRemision.getDestinatarios() != null
              && !guiaRemision.getDestinatarios().isEmpty()
              && guiaRemision.getDestinatarios().get(0).getIdentificacionDestinatario() != null) {
            identificacionDestinatario =
                guiaRemision.getDestinatarios().get(0).getIdentificacionDestinatario();
          }
          Path xmlPath =
              sriXmlStorageService.storeGuideXml(
                  formattedXml,
                  identificacionDestinatario,
                  authorizationDate,
                  guiaRemision.getInfoTributaria().getClaveAcceso());
          authorizationSri.setAuthorizationStoragePath(resolveStoragePath(xmlPath));
          authorizationSri.setUpdateDate(LocalDateTime.now());
          authorizationSri.setAuthorizationNumber(aut.getNumeroAutorizacion());
          authorizationSri.setAccessKey(guiaRemision.getInfoTributaria().getClaveAcceso());
          authorizationSri.setAuthorizationStatusSri(aut.getEstado());
          authorizationSri.setAuthorizationError(getMessagesError(aut));
          return authorizationRepo.save(authorizationSri);
        } else if (aut.getEstado().equals("NO AUTORIZADO")) {
          xml.append("<estado>" + aut.getEstado() + "</estado>");
          xml.append("<fechaAutorizacion>" + aut.getFechaAutorizacion() + "</fechaAutorizacion>");
          xml.append("\n<comprobante><![CDATA[" + aut.getComprobante() + "]]></comprobante>");
          xml.append(obtenerMensajesAutorizacion(aut));
          xml.append("\n</autorizacion>");
          LocalDateTime authorizationDate = UtilsDate.toLocalDateTime(aut.getFechaAutorizacion());
          authorizationSri.setAuthorizationDate(authorizationDate);
          String formattedXml = formatXml(xml.toString());
          authorizationSri.setAuthorizationXml(formattedXml);
          // Obtener identificación del destinatario desde la sección destinatarios
          String identificacionDestinatario = "DESCONOCIDO";
          if (guiaRemision.getDestinatarios() != null
              && !guiaRemision.getDestinatarios().isEmpty()
              && guiaRemision.getDestinatarios().get(0).getIdentificacionDestinatario() != null) {
            identificacionDestinatario =
                guiaRemision.getDestinatarios().get(0).getIdentificacionDestinatario();
          }
          Path xmlPath =
              sriXmlStorageService.storeGuideXml(
                  formattedXml,
                  identificacionDestinatario,
                  authorizationDate,
                  guiaRemision.getInfoTributaria().getClaveAcceso());
          authorizationSri.setAuthorizationStoragePath(resolveStoragePath(xmlPath));
          authorizationSri.setUpdateDate(LocalDateTime.now());
          authorizationSri.setAuthorizationNumber(aut.getNumeroAutorizacion());
          authorizationSri.setAccessKey(guiaRemision.getInfoTributaria().getClaveAcceso());
          authorizationSri.setAuthorizationStatusSri(aut.getEstado());
          authorizationSri.setAuthorizationError(getMessagesError(aut));
          return authorizationRepo.save(authorizationSri);
        }
      }
    } else if (respuestaComprobante.getAutorizaciones().getAutorizacion().isEmpty()) {
      authorizationSri.setAuthorizationXml("EL XML SE ENVIO PERO NO RECIBIO NINGUNA RESPUESTA");
      authorizationSri.setUpdateDate(LocalDateTime.now());
      authorizationSri.setAccessKey(guiaRemision.getInfoTributaria().getClaveAcceso());
      return authorizationRepo.save(authorizationSri);
    }
    return authorizationSri;
  }

  private String resolveStoragePath(Path xmlPath) {
    if (xmlPath == null) {
      return null;
    }
    Path parent = xmlPath.getParent();
    return (parent != null ? parent : xmlPath).toString();
  }

  private String obtenerMensajesAutorizacion(Autorizacion aut) {
    StringBuilder mensaje = new StringBuilder();
    mensaje.append("\n<mensajes>\n<mensaje>");
    for (Mensaje m : aut.getMensajes().getMensaje()) {
      mensaje.append("\n<mensaje>");
      mensaje.append("\n<identificador>" + m.getIdentificador() + "</identificador>");
      mensaje.append("\n<mensaje>" + m.getMensaje() + "</mensaje>");
      if (m.getInformacionAdicional() != null) {
        mensaje.append(
            "\n<informacionAdicional>" + m.getInformacionAdicional() + "</informacionAdicional>");
      }
      mensaje.append("\n<tipo>" + m.getTipo() + "</tipo>");
      mensaje.append("\n</mensaje>");
    }
    mensaje.append("\n</mensaje>\n</mensajes>");
    return mensaje.toString();
  }

  private String getMessagesError(Autorizacion aut) {
    StringBuilder mensaje = new StringBuilder();
    for (Mensaje m : aut.getMensajes().getMensaje()) {
      mensaje.append("\n" + m.getMensaje());
      if (m.getInformacionAdicional() != null) {
        mensaje.append("\n" + m.getInformacionAdicional());
      }
      mensaje.append("\n" + m.getTipo());
    }
    return mensaje.toString();
  }

  private String obtieneComprobantes(RespuestaSolicitud rs) {
    StringBuilder mensaje = new StringBuilder();
    mensaje.append("<ns2:respuestaSolicitud xmlns:ns2=\"http://ec.gob.sri.ws.recepcion\">");
    mensaje.append("\n<estado>" + rs.getEstado() + "</estado>");
    mensaje.append("\n<comprobantes>");
    for (Comprobante c : rs.getComprobantes().getComprobante()) {
      mensaje.append("\n<comprobante>");
      mensaje.append("\n<claveAcceso>" + c.getClaveAcceso() + "</claveAcceso>");
      mensaje.append("\n<mensajes>");
      mensaje.append(obtieneMensajesComprobante(c));
      mensaje.append("\n</mensajes>");
      mensaje.append("\n</comprobante>");
    }
    mensaje.append("\n</comprobantes>");
    mensaje.append("\n</ns2:respuestaSolicitud>");
    return mensaje.toString();
  }

  private String obtieneMensajesComprobante(Comprobante c) {
    StringBuilder mensaje = new StringBuilder();
    for (com.wallparisoft.integration.sri.ws.recepcion.Mensaje m : c.getMensajes().getMensaje()) {
      mensaje.append("\n<mensaje>");
      mensaje.append("\n<identificador>" + m.getIdentificador() + "</identificador>");
      mensaje.append("\n<mensaje>" + m.getMensaje() + "</mensaje>");
      mensaje.append(
          "\n<informacionAdicional>" + m.getInformacionAdicional() + "</informacionAdicional>");
      mensaje.append("\n<tipo>" + m.getTipo() + "</tipo>");
      mensaje.append("\n</mensaje>");
    }
    return mensaje.toString();
  }

  private String formatXml(String xmlString) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document document = builder.parse(new InputSource(new StringReader(xmlString)));

      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(document), new StreamResult(writer));
      return writer.toString();
    } catch (Exception e) {
      return xmlString;
    }
  }

  private AuthorizationSri getOrCreateAuthorization(
      AuthorizationSri existingAuthorizationSri,
      String accessKey,
      String receptionStatusSri,
      String receptionXml,
      String receptionError) {
    if (existingAuthorizationSri == null) {
      return AuthorizationSri.builder()
          .status(true)
          .accessKey(accessKey)
          .receptionStatusSri(receptionStatusSri)
          .receptionXml(receptionXml)
          .receptionError(receptionError)
          .build();
    }

    existingAuthorizationSri.setStatus(true);
    existingAuthorizationSri.setAccessKey(accessKey);
    existingAuthorizationSri.setReceptionStatusSri(receptionStatusSri);
    existingAuthorizationSri.setReceptionXml(receptionXml);
    if (receptionError != null) {
      existingAuthorizationSri.setReceptionError(receptionError);
    }
    existingAuthorizationSri.setUpdateDate(LocalDateTime.now());
    return existingAuthorizationSri;
  }

  public static void hilo() {
    try {
      Thread.currentThread();
      Thread.sleep(300L);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
