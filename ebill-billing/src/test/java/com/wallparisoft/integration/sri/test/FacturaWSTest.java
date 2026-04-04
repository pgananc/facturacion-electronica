package com.wallparisoft.integration.sri.test;

import com.wallparisoft.integration.sri.model.Factura;
import com.wallparisoft.integration.sri.util.MockFirmaXadesBesUtil;
import com.wallparisoft.integration.sri.util.XmlUtil;
import com.wallparisoft.integration.sri.ws.autorizacion.AutorizacionComprobantes;
import com.wallparisoft.integration.sri.ws.autorizacion.AutorizacionComprobantesService;
import com.wallparisoft.integration.sri.ws.autorizacion.RespuestaComprobante;
import com.wallparisoft.integration.sri.ws.recepcion.RecepcionComprobantes;
import com.wallparisoft.integration.sri.ws.recepcion.RecepcionComprobantesService;
import com.wallparisoft.integration.sri.ws.recepcion.RespuestaSolicitud;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class FacturaWSTest extends FacturaTest {

  @Test
  public void deberiaReceptarFactura()
      throws SAXParseException, SAXException, IOException, JAXBException, InterruptedException {
    FacturaTest facturaTest = new FacturaTest();
    Factura factura = facturaTest.crearFactura();
    ByteArrayOutputStream baosFactura = (new XmlUtil()).convertirObjetoAXml(Factura.class, factura);
    // Usar mock en lugar de certificado real para tests unitarios
    // NOTA: Este test requiere conexión real al SRI, por lo que puede fallar sin certificado real
    // Para tests de integración completos, se recomienda usar certificados de prueba del SRI
    MockFirmaXadesBesUtil mockFirma = new MockFirmaXadesBesUtil();
    ByteArrayOutputStream baosFacturaFirmada = new ByteArrayOutputStream();
    mockFirma.firmarDocumento(
        new ByteArrayInputStream(baosFactura.toByteArray()), baosFacturaFirmada);

    RecepcionComprobantesService webServiceRecepcion = new RecepcionComprobantesService();
    RecepcionComprobantes port1 = webServiceRecepcion.getRecepcionComprobantesPort();

    RespuestaSolicitud respuestaSolicitud =
        port1.validarComprobante(baosFacturaFirmada.toByteArray());

    if (!respuestaSolicitud.getComprobantes().getComprobante().isEmpty()) {
      for (com.wallparisoft.integration.sri.ws.recepcion.Mensaje mensaje :
          respuestaSolicitud
              .getComprobantes()
              .getComprobante()
              .get(0)
              .getMensajes()
              .getMensaje()) {}
    }

    Thread.sleep(4500);

    AutorizacionComprobantesService webServiceAutorizacion = new AutorizacionComprobantesService();

    AutorizacionComprobantes port2 = webServiceAutorizacion.getAutorizacionComprobantesPort();
    RespuestaComprobante respuestaComprobante =
        port2.autorizacionComprobante(factura.getInfoTributaria().getClaveAcceso());
    if (!respuestaComprobante.getAutorizaciones().getAutorizacion().isEmpty()) {
      for (com.wallparisoft.integration.sri.ws.autorizacion.Mensaje mensaje :
          respuestaComprobante
              .getAutorizaciones()
              .getAutorizacion()
              .get(0)
              .getMensajes()
              .getMensaje()) {}
    }
  }
}
