package com.wallparisoft.integration.sri.test;

import com.wallparisoft.integration.sri.enums.AmbienteEnum;
import com.wallparisoft.integration.sri.enums.FormaPagoEnum;
import com.wallparisoft.integration.sri.enums.MonedaEnum;
import com.wallparisoft.integration.sri.enums.ObligadoContabilidadEnum;
import com.wallparisoft.integration.sri.enums.TipoComprobanteEnum;
import com.wallparisoft.integration.sri.enums.TipoEmisionEnum;
import com.wallparisoft.integration.sri.enums.TipoIdentificacionCompradorEnum;
import com.wallparisoft.integration.sri.model.DetAdicional;
import com.wallparisoft.integration.sri.model.Detalle;
import com.wallparisoft.integration.sri.model.Factura;
import com.wallparisoft.integration.sri.model.Impuesto;
import com.wallparisoft.integration.sri.model.InfoFactura;
import com.wallparisoft.integration.sri.model.InfoTributaria;
import com.wallparisoft.integration.sri.model.Pago;
import com.wallparisoft.integration.sri.model.TotalImpuesto;
import com.wallparisoft.integration.sri.util.MockFirmaXadesBesUtil;
import com.wallparisoft.integration.sri.util.XmlUtil;
import jakarta.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class FacturaTest {

  public void deberiaValidarFacturaFirmadaConEsquema() throws Exception {
    ByteArrayOutputStream baosFirmado = crearFacturaFirmada();
    InputStream is = new ByteArrayInputStream(baosFirmado.toByteArray());
    XmlUtil.validarQueUnXmlCumpleConXSD(is, "src/test/resources/xsd/Factura_V_2_1_0.xsd");
  }

  protected byte[] crearByteArrayFacturaFirmada()
      throws SAXParseException, SAXException, IOException, JAXBException {
    ByteArrayOutputStream baosFirmado = crearFacturaFirmada();
    return baosFirmado.toByteArray();
  }

  private ByteArrayOutputStream crearFacturaFirmada()
      throws SAXParseException, SAXException, IOException, JAXBException {
    ByteArrayOutputStream baosFactura =
        (new XmlUtil()).convertirObjetoAXml(Factura.class, crearFactura());
    // Usar mock en lugar de certificado real para tests unitarios
    MockFirmaXadesBesUtil mockFirma = new MockFirmaXadesBesUtil();
    ByteArrayOutputStream baosFacturaFirmada = new ByteArrayOutputStream();
    mockFirma.firmarDocumento(
        new ByteArrayInputStream(baosFactura.toByteArray()), baosFacturaFirmada);
    return baosFactura;
  }

  public Factura crearFactura() {
    Factura factura = new Factura();
    factura.setVersion("2.1.0");
    factura.setId("comprobante");
    factura.setInfoTributaria(crearInfoTributaria());
    factura.setInfoFactura(crearInfoFactura());
    factura.setDetalles(crearDetalles());

    return factura;
  }

  public String generateAccessKey(InfoTributaria infoTributaria) {
    Calendar cal = Calendar.getInstance();
    String accessKey =
        infoTributaria.generarClaveAcceso(cal.getTime(), TipoComprobanteEnum.FACTURA, "12345678");
    if (accessKey.length() == 49) {
      return accessKey;
    } else {
      return generateAccessKey(infoTributaria);
    }
  }

  private List<Detalle> crearDetalles() {
    List<Detalle> detalles = new ArrayList<Detalle>();
    Detalle detalle = new Detalle();
    detalle.setCodigoPrincipal("831520202");
    detalle.setCodigoAuxiliar("1");
    detalle.setDescripcion("APROVISIONAMIENTO DE SOFTWARE DE GESTION ESTUDIANTIL");
    detalle.setCantidad("1.00");
    detalle.setPrecioUnitario("1.00");
    detalle.setPrecioTotalSinImpuesto("1.00");
    detalle.setDescuento("0.00");
    detalles.add(detalle);

    List<DetAdicional> detallesAdicionales = new ArrayList<DetAdicional>();
    DetAdicional detAdicional = new DetAdicional();
    detallesAdicionales.add(detAdicional);
    detAdicional.setNombre("nombre");
    detAdicional.setValor("valor");
    detalle.setDetallesAdicionales(detallesAdicionales);

    List<Impuesto> impuestos = new ArrayList<Impuesto>();
    Impuesto impuesto = new Impuesto();
    impuestos.add(impuesto);
    impuesto.setCodigo("2");
    impuesto.setCodigoPorcentaje("0");
    impuesto.setTarifa("0");
    impuesto.setBaseImponible("1.00");
    impuesto.setValor("0.14");
    detalle.setImpuestos(impuestos);

    return detalles;
  }

  public InfoFactura crearInfoFactura() {
    InfoFactura infoFactura = new InfoFactura();
    infoFactura.setFechaEmision((new SimpleDateFormat("dd/MM/YYYY")).format(new Date()));
    infoFactura.setDirEstablecimiento("Calle: DUCHICELA Numero: OE8-345 Interseccion: SHIRYS ");
    infoFactura.setObligadoContabilidad(ObligadoContabilidadEnum.NO);
    infoFactura.setTipoIdentificacionComprador(TipoIdentificacionCompradorEnum.CEDULA);
    infoFactura.setRazonSocialComprador("RAZON SOCIAL DEL COMPRADOR");
    infoFactura.setIdentificacionComprador("1803861937");
    infoFactura.setTotalSinImpuestos("1.00");
    infoFactura.setTotalDescuento("0.00");
    infoFactura.setTotalConImpuestos(crearTotalImpuestos());
    infoFactura.setImporteTotal("1.14");
    infoFactura.setMoneda(MonedaEnum.DOLAR);
    infoFactura.setPagos(crearPagos());
    return infoFactura;
  }

  private List<Pago> crearPagos() {
    List<Pago> pagos = new ArrayList<Pago>();
    Pago pago = new Pago();
    pago.setFormaPago(FormaPagoEnum.SIN_UTILIZACION_DEL_SISTEMA_FINANCIERO);
    pago.setTotal("1.14");
    pagos.add(pago);
    return pagos;
  }

  public List<TotalImpuesto> crearTotalImpuestos() {
    List<TotalImpuesto> totalImpuestos = new ArrayList<TotalImpuesto>();
    TotalImpuesto totalImpuesto = new TotalImpuesto();
    totalImpuesto.setCodigo("2");
    totalImpuesto.setCodigoPorcentaje("3");
    totalImpuesto.setTarifa("0");
    totalImpuesto.setBaseImponible("1.00");
    totalImpuesto.setValor("0.14");
    totalImpuestos.add(totalImpuesto);
    return totalImpuestos;
  }

  public InfoTributaria crearInfoTributaria() {
    InfoTributaria infoTributaria = new InfoTributaria();
    infoTributaria.setAmbiente(AmbienteEnum.PRUEBAS);
    infoTributaria.setTipoEmision(TipoEmisionEnum.NORMAL);
    infoTributaria.setRazonSocial("JONATHAN EDDIE GUERRERO HIDALGO");
    infoTributaria.setNombreComercial("JONATHAN EDDIE GUERRERO HIDALGO");
    infoTributaria.setDirMatriz("DIRECCION MATRIZ");
    infoTributaria.setRuc("1722633961001");
    infoTributaria.setCodDoc("01");
    infoTributaria.setEstab("001");
    infoTributaria.setPtoEmi("100");
    Random rn = new Random();
    // Generar un secuencial de 9 dígitos (entre 100000000 y 999999999)
    int min = 100000000;
    int max = 999999999;
    int random = rn.nextInt(max - min + 1) + min;
    infoTributaria.setSecuencial(String.format("%09d", random));
    Calendar cal = Calendar.getInstance();

    //    infoTributaria.generarClaveAcceso(cal.getTime(), TipoComprobanteEnum.FACTURA, "12345678");
    infoTributaria.setClaveAcceso(generateAccessKey(infoTributaria));
    infoTributaria.setDirMatriz("Calle: Dir. Matri Numero: OE8-345 Interseccion: Dir. Matriz");
    return infoTributaria;
  }
}
