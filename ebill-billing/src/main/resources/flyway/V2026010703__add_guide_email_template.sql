-- Agregar parámetro para la plantilla de email de guías de remisión
INSERT INTO params (
    id_param,
    code_param,
    value_param,
    description_param,
    status,
    creation_date
)
VALUES (
    (
        SELECT COALESCE(MAX(id_param), 0) + 1
        FROM params
    ),
    'GUIDE_EMAIL_TEMPLATE',
    $$<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html lang="es" xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width" />
  <title>Guía de remisión electrónica autorizada</title>
  <style>
    body {
      background-color: #f6f6f6;
      font-family: Arial, sans-serif;
      font-size: 14px;
      line-height: 1.6;
      margin: 0;
      padding: 0;
      color: #343a40;
    }
    .body {
      background-color: #f0f3f7;
      width: 100%;
    }
    table {
      border-collapse: separate;
      width: 100%;
    }
    table td {
      font-family: inherit;
      font-size: inherit;
      vertical-align: top;
    }
    .container {
      display: block;
      margin: 0 auto !important;
      max-width: 600px;
      padding: 16px;
      width: 600px;
    }
    .content {
      box-sizing: border-box;
      display: block;
      margin: 0 auto;
      max-width: 600px;
      padding: 0;
    }
    .main {
      background: #ffffff;
      border-radius: 12px;
      border-top: 4px solid #003a75;
      box-shadow: 0 6px 18px rgba(0, 0, 0, 0.08);
      overflow: hidden;
    }
    .wrapper {
      box-sizing: border-box;
      padding: 32px 32px 24px 32px;
    }
    .header-brand {
      background: linear-gradient(135deg, #003a75 0%, #00508a 100%);
      padding: 24px 32px;
      text-align: center;
      color: #ffffff;
    }
    .header-brand h1 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      letter-spacing: 0.4px;
      text-transform: uppercase;
      color: #ffffff;
    }
    .header-brand p {
      margin: 12px 0 0;
      font-size: 15px;
      color: #ffffff;
      font-weight: 500;
      opacity: 1;
    }
    .info-icon {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 42px;
      height: 42px;
      border-radius: 50%;
      background: #e8f1fa;
      color: #003a75;
      margin-bottom: 12px;
      font-size: 20px;
    }
    h2 {
      font-size: 18px;
      font-weight: 600;
      color: #003a75;
      margin: 16px 0 12px;
    }
    p {
      margin: 0 0 16px 0;
      color: #495057;
    }
    p strong {
      color: #003a75;
    }
    .details {
      background: #f8fbff;
      border: 1px solid #d8e3f2;
      border-radius: 10px;
      padding: 16px 20px;
      margin-bottom: 20px;
    }
    .details p {
      margin: 8px 0;
    }
    .badge {
      display: inline-block;
      padding: 6px 12px;
      border-radius: 999px;
      background: #e8f1fa;
      color: #003a75;
      font-weight: 600;
      font-size: 12px;
      letter-spacing: 0.3px;
    }
    ul {
      padding-left: 20px;
      color: #495057;
      margin: 0 0 16px 0;
    }
    ul li {
      margin-bottom: 8px;
    }
    .footer {
      text-align: center;
      width: 100%;
      margin-top: 16px;
    }
    .footer td {
      color: #6c757d;
      font-size: 12px;
    }
    .footer a {
      color: #003a75;
      text-decoration: none;
    }
    .footer-bar {
      background: #003a75;
      color: #ffffff;
      padding: 12px;
      font-size: 12px;
      text-align: center;
    }
    @media only screen and (max-width: 620px) {
      .container,
      .content {
        width: 100% !important;
        padding: 0 12px !important;
      }
      .wrapper {
        padding: 24px 20px 20px 20px !important;
      }
      h2 {
        font-size: 17px !important;
      }
      p {
        font-size: 15px !important;
      }
    }
  </style>
</head>
<body>
  <table role="presentation" border="0" cellpadding="0" cellspacing="0" class="body">
    <tr>
      <td></td>
      <td class="container">
        <div class="content">
          <table role="presentation" class="main">
            <tr>
              <td class="header-brand">
                <h1>{{companyName}}</h1>
                <p>Guía de remisión electrónica autorizada</p>
              </td>
            </tr>
            <tr>
              <td class="wrapper">
                <div class="info-icon">🚚</div>
                <h2>Estimado/a <strong>{{buyerName}}</strong>,</h2>
                <p>
                  Adjuntamos su guía de remisión electrónica número <strong>{{guideNumber}}</strong>,
                  autorizada por el SRI.
                </p>
                <div class="details">
                  <p><strong>Fecha de autorización:</strong> {{authorizationDate}}</p>
                  <p><strong>Número de autorización:</strong> {{authorizationNumber}}</p>
                  <p><strong>Clave de acceso:</strong> {{accessKey}}</p>
                  <p><strong>Fecha de emisión:</strong> {{issueDate}}</p>
                  <p><strong>Fecha inicio traslado:</strong> {{startDate}}</p>
                  <p><strong>Fecha fin traslado:</strong> {{endDate}}</p>
                  <p><strong>Transportista:</strong> {{carrierName}}</p>
                  <p><strong>Placa:</strong> {{transportPlate}}</p>
                  <p><strong>Origen:</strong> {{originAddress}}</p>
                  <p><strong>Destino:</strong> {{destinationAddress}}</p>
                  <p><strong>Motivo traslado:</strong> {{reasonForTransfer}}</p>
                </div>
                <p>Le enviamos dos archivos adjuntos:</p>
                <ul>
                  <li>Guía de remisión en formato PDF (RIDE).</li>
                  <li>XML autorizado por el SRI.</li>
                </ul>
                <p>
                  Este documento ya está disponible para su visualización y descarga visitando 
                  <a href="{{consultationUrl}}" style="color: #003a75; text-decoration: underline;">aquí</a>.
                </p>
                <p>
                  Gracias por confiar en nosotros. Si requiere información adicional,
                  por favor contacte a nuestro equipo de soporte.
                </p>
                <p>Saludos cordiales<br /></p>
              </td>
            </tr>
          </table>
          <div class="footer">
            <table role="presentation">
              <tr>
                <td>Quito, Ecuador</td>
              </tr>
              <tr>
                <td>&copy; Wallparisoft. Todos los derechos reservados.</td>
              </tr>
            </table>
          </div>
          <div class="footer-bar">
            Este correo fue enviado automáticamente. Por favor, no respondas a este mensaje.
          </div>
        </div>
      </td>
      <td></td>
    </tr>
  </table>
</body>
</html>$$,
    'Plantilla HTML para el correo de notificación de guía de remisión autorizada',
    TRUE,
    CURRENT_TIMESTAMP
) ON CONFLICT (code_param) DO NOTHING;




