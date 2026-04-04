// package com.wallparisoft.integration.sri.util;
//
//
// import java.io.ByteArrayInputStream;
// import java.io.StringWriter;
// import java.math.BigDecimal;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.Map;
//
// import javax.xml.parsers.DocumentBuilder;
// import javax.xml.parsers.DocumentBuilderFactory;
// import javax.xml.parsers.ParserConfigurationException;
// import javax.xml.transform.Transformer;
// import javax.xml.transform.TransformerException;
// import javax.xml.transform.TransformerFactory;
// import javax.xml.transform.dom.DOMSource;
// import javax.xml.transform.stream.StreamResult;
//
// import org.springframework.cglib.core.Local;
// import org.w3c.dom.Document;
// import org.w3c.dom.Element;
// import org.w3c.dom.NodeList;
//
// import com.wallparisoft.ebillbilling.dto.InvoiceDto;
// import com.wallparisoft.integration.sri.model.CampoAdicional;
// import com.wallparisoft.integration.sri.model.Impuesto;
// import com.wallparisoft.integration.sri.model.InfoTributaria;
// import com.wallparisoft.integration.sri.model.TotalImpuesto;
//
// import ec.com.redepronik.negosys.invfac.entityAux.CantidadFactura;
// import ec.com.redepronik.negosys.rrhh.entity.Persona;
// import
// ec.com.redepronik.negosys.utils.documentosElectronicos.entityDocumentosElectronicosRIDE.DocumentoReporteRIDE;
// import
// ec.com.redepronik.negosys.utils.documentosElectronicos.entityDocumentosElectronicosXML.InfoAdicional;
//
// public class UtilsDocumentos {
//
// private static CampoAdicional campoAdicional(String nombre, String valor) {
// CampoAdicional campoAdicional = new CampoAdicional();
// campoAdicional.setNombre(nombre);
// campoAdicional.setValue(valor);
// return campoAdicional;
// }
//
// public static String tipoIdentificacionComprador(String identificacion) {
// if (identificacion.length() == 13) {
// if (identificacion.compareToIgnoreCase("9999999999999") == 0)
// return "07";
// else
// return "04";
// } else if (identificacion.length() == 10)
// return "05";
// return "";
// }
//
// public static String claveAcceso(Date fechaEmision, String tipoComprobante,
// String serie, String secuencial, InvoiceDto invoiceDto) {
// String tipoAmbien = invoiceDto.getEnvironment();
// String tipoEmi = parametro.getTipoEmision() ? "1" : "0";
// String clave = UtilsDate.fechaFormatoString(fechaEmision, "ddMMyyyy")
// + tipoComprobante + invoiceDto.getCompanyDto().getIdentification() + tipoAmbien +
// invoiceDto.getEstablishment().getCode()
// + secuencial + "12345678" + tipoEmi;
// int sum = 0;
// int val = 2;
// for (int i = clave.length() - 1; i >= 0; i--) {
// sum += Integer.parseInt(String.valueOf(clave.charAt(i))) * val;
// val++;
// if (val > 7)
// val = 2;
// }
// int checksum = 11 - sum % 11;
// if (checksum == 11)
// checksum = 0;
// else if (checksum == 10)
// checksum = 1;
// return clave + checksum;
// }
//
// public static String codDocumento(String claveAcceso) {
// return claveAcceso.substring(8, 10);
// }
//
// public static String convertirDocumentAString(Document doc) {
// try {
// TransformerFactory tf = TransformerFactory.newInstance();
// Transformer transformer = tf.newTransformer();
// // below code to remove XML declaration
// // transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
// // "yes");
// StringWriter writer = new StringWriter();
// transformer.transform(new DOMSource(doc), new StreamResult(writer));
// return writer.getBuffer().toString();
// } catch (TransformerException e) {
// e.printStackTrace();
// }
// return null;
// }
//
//// public static Document convertirStringADocument(String xml) {
//// try {
//// DocumentBuilderFactory factory = DocumentBuilderFactory
//// .newInstance();
//// factory.setNamespaceAware(true);
//// DocumentBuilder builder = factory.newDocumentBuilder();
//// return builder
//// .parse(new ByteArrayInputStream(convertirString(xml)));
//// } catch (Exception e) {
//// e.printStackTrace();
//// }
//// return null;
//// }
//
//// public static String obtenerCedulaCliente(String xml) {
//// Document document = convertirStringADocument(xml);
//// NodeList list = document
//// .getElementsByTagName("identificacionComprador");
//// if (list.getLength() == 0)
//// list = document
//// .getElementsByTagName("identificacionSujetoRetenido");
//// String valor = "";
//// for (int i = 0; i < list.getLength(); i++) {
//// NodeList font = (NodeList) list.item(i);
//// valor = font.item(0).getNodeValue();
//// }
//// return valor;
//// }
//
// public static Map<String, Object> crearDocumentoXML(String nombre) {
// DocumentBuilder builder = null;
// try {
// builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
// } catch (ParserConfigurationException e) {
// e.printStackTrace();
// }
// Document document = builder.getDOMImplementation().createDocument(null,
// nombre, null);
// document.setXmlVersion("1.0");
// document.setXmlStandalone(true);
//
// Element raiz = document.getDocumentElement();
// raiz.setAttribute("id", "comprobante");
// raiz.setAttribute("version", "1.1.0");
// Map<String, Object> map = new HashMap<String, Object>();
// map.put("Document", document);
// map.put("Element", raiz);
// return map;
// }
//
//// public static DocumentoReporteRIDE documentoRIDE(Document doc) {
//// NodeList list = doc.getElementsByTagName("*");
//// String documento = "";
//// String numeroAutorizacion = "";
//// String fechaAutorizacion = "";
//// String estado = "";
//// for (int i = 0; i < list.getLength(); i++) {
//// Element element = (Element) list.item(i);
//// if (element.getNodeName().equals("comprobante"))
//// documento = element.getChildNodes().item(0).getNodeValue();
//// else if (element.getNodeName().equals("estado"))
//// estado = element.getChildNodes().item(0).getNodeValue();
//// else if (element.getNodeName().equals("numeroAutorizacion"))
//// numeroAutorizacion = element.getChildNodes().item(0)
//// .getNodeValue();
//// else if (element.getNodeName().equals("fechaAutorizacion"))
//// fechaAutorizacion = element.getChildNodes().item(0)
//// .getNodeValue();
//// }
//// return new DocumentoReporteRIDE(numeroAutorizacion, fechaAutorizacion,
//// estado, documento);
//// }
//
// public static void hilo() {
// try {
// Thread.currentThread();
// Thread.sleep(300L);
// } catch (InterruptedException e) {
// e.printStackTrace();
// }
// }
//
//// public static Impuesto impuesto(int codigo, short codigoPorcentaje,
//// BigDecimal tarifa, BigDecimal baseImponible, BigDecimal valor) {
//// Impuesto impuesto = new Impuesto();
//// impuesto.setCodigo(String.valueOf(codigo));
//// impuesto.setCodigoPorcentaje(String.valueOf(codigoPorcentaje));
//// if (tarifa != null)
//// impuesto.setTarifa(tarifa.toString());
//// impuesto.setBaseImponible(redondearTotales(baseImponible));
//// impuesto.setValor(redondearTotales(valor));
//// return impuesto;
//// }
////
//// public static InfoAdicional infoAdicional(Persona p) {
//// InfoAdicional infoAdicional = new InfoAdicional();
////
//// infoAdicional.getCampoAdicional().add(
//// campoAdicional("Dirección", p.getDireccion()));
//// if (p.getEmail() != null && p.getEmail().compareToIgnoreCase("") != 0)
//// infoAdicional.getCampoAdicional().add(
//// campoAdicional("Email", p.getEmail()));
////
//// return infoAdicional;
//// }
//
//// public static InfoTributaria infoTributaria(String cAcceso, Local local) {
//// InfoTributaria infoTributaria = new InfoTributaria();
//// infoTributaria.setAmbiente(parametro.getTipoAmbiente() ? "2" : "1");
//// infoTributaria.setTipoEmision(parametro.getTipoEmision() ? "1" : "0");
//// infoTributaria.setRazonSocial(parametro.getRazonSocial());
//// infoTributaria.setNombreComercial(local.getNombre());
//// infoTributaria.setRuc(parametro.getRuc());
//// infoTributaria.setClaveAcceso(cAcceso);
//// infoTributaria.setCodDoc(codDocumento(cAcceso));
//// infoTributaria.setDirMatriz(matriz.getDireccion());
//// return infoTributaria;
//// }
//
// public static Document leerXML(String archivo) {
// try {
// DocumentBuilderFactory fábricaCreadorDocumento = DocumentBuilderFactory
// .newInstance();
// fábricaCreadorDocumento.setNamespaceAware(true);
// DocumentBuilder creadorDocumento = fábricaCreadorDocumento
// .newDocumentBuilder();
// return creadorDocumento.parse(archivo);
// } catch (Exception e) {
// e.printStackTrace();
// }
// return null;
// }
//
//// public static String quitarFirma(String documento) {
//// Document document = convertirStringADocument(documento);
//// NodeList list = document.getElementsByTagName("*");
//// for (int i = 0; i < list.getLength(); i++) {
//// Element element = (Element) list.item(i);
//// if (element.getNodeName().contains("ds:"))
//// element.getParentNode().removeChild(element);
//// }
////
//// return convertirDocumentAString(document);
//// }
//
//// public static CantidadFactura redondearCantidadFactura(CantidadFactura cf) {
//// cf.setStSinImpuesto(redondearTotales(cf.getStSinImpuesto()));
//// cf.setSt12(redondearTotales(cf.getSt12()));
//// cf.setSt0(redondearTotales(cf.getSt0()));
//// cf.setStNoObjetoIva(redondearTotales(cf.getStNoObjetoIva()));
//// cf.setStExentoIva(redondearTotales(cf.getStExentoIva()));
//// cf.settDescuento(redondearTotales(cf.gettDescuento()));
//// cf.setValorIce(redondearTotales(cf.getValorIce()));
//// cf.setValorIRBPNR(redondearTotales(cf.getValorIRBPNR()));
//// cf.setIva12(redondearTotales(cf.getIva12()));
//// cf.setPropina(redondearTotales(cf.getPropina()));
//// cf.setValorTotal(redondearTotales(cf.getValorTotal()));
//// return cf;
//// }
//
// public static String tipoDocumento(String claveAcceso) {
// switch (claveAcceso.substring(8, 10)) {
// case "01":
// return "FACTURA";
// case "04":
// return "NOTA DE CRÉDITO";
// case "05":
// return "NOTA DE DÉBITO";
// case "06":
// return "GUÍA DE REMISIÓN";
// case "07":
// return "COMPROBANTE DE RETENCIÓN";
// default:
// return "";
// }
// }
//
// public static int intTipoDocumento(String claveAcceso) {
// switch (claveAcceso.substring(8, 10)) {
// case "01":
// return "factura".length();
// case "04":
// return "notaCredito".length();
// case "05":
// return "notaDebito".length();
// case "06":
// return "guiaRemision".length();
// case "07":
// return "comprobanteRetencion".length();
// default:
// return 0;
// }
// }
//
// public static String stringTipoDocumento(String claveAcceso) {
// switch (claveAcceso.substring(8, 10)) {
// case "01":
// return "factura";
// case "04":
// return "notaCredito";
// case "05":
// return "notaDebito";
// case "06":
// return "guiaRemision";
// case "07":
// return "comprobanteRetencion";
// default:
// return "";
// }
// }
//
//// public static TotalImpuesto totalImpuesto(String codigo,
//// String codigoPorcentaje, BigDecimal baseImponible,
//// BigDecimal tarifa, BigDecimal valor) {
//// TotalImpuesto totalImpuesto = new TotalImpuesto();
//// totalImpuesto.setCodigo(codigo);
//// totalImpuesto.setCodigoPorcentaje(codigoPorcentaje);
//// totalImpuesto.setBaseImponible(redondearTotales(baseImponible));
//// if (tarifa != null)
//// totalImpuesto.setTarifa(redondearTotales(tarifa));
//// totalImpuesto.setValor(redondearTotales(valor));
//// return totalImpuesto;
//// }
//
// }
