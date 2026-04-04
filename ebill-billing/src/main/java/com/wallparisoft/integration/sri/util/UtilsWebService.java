package com.wallparisoft.integration.sri.util;

import com.wallparisoft.integration.sri.ws.autorizacion.AutorizacionComprobantes;
import com.wallparisoft.integration.sri.ws.autorizacion.AutorizacionComprobantesService;
import com.wallparisoft.integration.sri.ws.autorizacion.RespuestaComprobante;
import com.wallparisoft.integration.sri.ws.autorizacion.RespuestaLote;
import com.wallparisoft.integration.sri.ws.recepcion.RecepcionComprobantes;
import com.wallparisoft.integration.sri.ws.recepcion.RecepcionComprobantesService;
import com.wallparisoft.integration.sri.ws.recepcion.RespuestaSolicitud;
import jakarta.xml.ws.BindingProvider;
import java.net.URL;
import javax.xml.namespace.QName;

public class UtilsWebService {

  private static String produccion = "https://cel.sri.gob.ec";
  private static String prueba = "https://celcer.sri.gob.ec";
  private static String recepcion = "/comprobantes-electronicos-ws/RecepcionComprobantes?wsdl";
  private static String recepcionPrueba =
      "/comprobantes-electronicos-ws/RecepcionComprobantesOffline?wsdl";
  private static String autorizacion =
      "/comprobantes-electronicos-ws/AutorizacionComprobantes?wsdl";
  private static String autorizacionPrueba =
      "/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
  private static boolean ambiente = false;

  public static RespuestaComprobante autorizacionComprobante(String claveAccesoComprobante) {
    try {
      //			certificadoSeguridadSRI();
      AutorizacionComprobantesService service =
          new AutorizacionComprobantesService(
              new URL((ambiente ? produccion : prueba) + autorizacion),
              new QName("http://ec.gob.sri.ws.autorizacion", "AutorizacionComprobantesService"));
      AutorizacionComprobantes port = service.getAutorizacionComprobantesPort();
      ((BindingProvider) port)
          .getRequestContext()
          .put("com.sun.xml.internal.ws.connect.timeout", 9999);
      ((BindingProvider) port)
          .getRequestContext()
          .put("com.sun.xml.internal.ws.request.timeout", 9999);
      return port.autorizacionComprobante(claveAccesoComprobante);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static RespuestaLote autorizacionComprobanteLote(String claveAccesoLote) {
    try {
      //			certificadoSeguridadSRI();
      AutorizacionComprobantesService service =
          new AutorizacionComprobantesService(
              new URL((ambiente ? produccion : prueba) + autorizacion),
              new QName("http://ec.gob.sri.ws.autorizacion", "AutorizacionComprobantesService"));
      AutorizacionComprobantes port = service.getAutorizacionComprobantesPort();
      ((BindingProvider) port)
          .getRequestContext()
          .put("com.sun.xml.internal.ws.connect.timeout", 9999);
      ((BindingProvider) port)
          .getRequestContext()
          .put("com.sun.xml.internal.ws.request.timeout", 9999);
      return port.autorizacionComprobanteLote(claveAccesoLote);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static RespuestaSolicitud validarComprobante(byte[] xml) {
    try {
      RecepcionComprobantesService service =
          new RecepcionComprobantesService(
              new URL((ambiente ? produccion : prueba) + (ambiente ? recepcion : recepcion)),
              new QName("http://ec.gob.sri.ws.recepcion", "RecepcionComprobantesService"));
      RecepcionComprobantes port = service.getRecepcionComprobantesPort();
      ((BindingProvider) port)
          .getRequestContext()
          .put("com.sun.xml.internal.ws.connect.timeout", 9999);
      ((BindingProvider) port)
          .getRequestContext()
          .put("com.sun.xml.internal.ws.request.timeout", 9999);
      return port.validarComprobante(xml);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }
}
