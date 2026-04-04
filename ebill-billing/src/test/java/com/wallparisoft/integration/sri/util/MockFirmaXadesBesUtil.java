package com.wallparisoft.integration.sri.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Mock de FirmaXadesBesUtil para tests unitarios. Simula la firma digital sin requerir certificados
 * reales. Para tests que solo necesitan validar la estructura XML, no la firma real.
 */
public class MockFirmaXadesBesUtil {

  /**
   * Simula la firma de un documento XML copiando el contenido del InputStream al OutputStream. En
   * un test real, esto debería agregar la firma digital, pero para tests unitarios que solo validan
   * estructura XML, copiar el contenido es suficiente.
   *
   * @param is InputStream con el XML a "firmar"
   * @param os OutputStream donde se escribirá el XML "firmado"
   * @throws IOException si hay error al leer/escribir
   */
  public void firmarDocumento(InputStream is, OutputStream os) throws IOException {
    // Simular firma: copiar el contenido del XML original
    // En tests que solo validan estructura XSD, esto es suficiente
    byte[] buffer = new byte[1024];
    int bytesRead;
    while ((bytesRead = is.read(buffer)) != -1) {
      os.write(buffer, 0, bytesRead);
    }
    os.flush();
  }
}
