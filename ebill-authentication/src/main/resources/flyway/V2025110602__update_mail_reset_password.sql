update public.params 
set value_param='<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns:th="http://www.thymeleaf.org" xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta name="viewport" content="width=device-width" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>Restablecer contraseña</title>
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
      .btn-primary table {
        width: 100%;
      }
      .btn-primary table td {
        background-color: #003a75;
        border-radius: 8px;
        padding: 0;
      }
      .btn-primary a {
        background-color: #003a75;
        border: 1px solid #003a75;
        border-radius: 8px;
        box-sizing: border-box;
        color: #ffffff;
        cursor: pointer;
        display: block;
        font-size: 15px;
        font-weight: 600;
        padding: 16px 32px;
        text-decoration: none;
        text-transform: none;
        letter-spacing: 0.5px;
        width: 100%;
        text-align: center;
      }
      .btn-primary a:hover {
        background-color: #00508a;
        border-color: #00508a;
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
        .btn-primary a {
          width: 100% !important;
          text-align: center !important;
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
                  <h1>Wallparisoft</h1>
                  <p>Restablecimiento de contraseña</p>
                </td>
              </tr>
              <tr>
                <td class="wrapper">
                  <div class="info-icon">🔒</div>
                  <h2>Hola, <strong>$${user}</strong></h2>
                  <p>Recibimos una solicitud para restablecer tu contraseña. Si no fuiste tú, puedes ignorar este correo.</p>
                  <p>
                    Este enlace estará activo durante
                    <span class="badge">$${duration} minutos</span>.
                  </p>
                  <table role="presentation" border="0" cellpadding="0" cellspacing="0" class="btn btn-primary">
                    <tbody>
                      <tr>
                        <td align="center">
                          <table role="presentation" border="0" cellpadding="0" cellspacing="0" width="100%">
                            <tbody>
                              <tr>
                                <td align="center"><a href="$${resetUrl}" target="_blank">Restablecer contraseña</a></td>
                              </tr>
                            </tbody>
                          </table>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                  <br/>
                  <p>Gracias,<br/>Equipo Wallparisoft</p>
                </td>
              </tr>
            </table>

            <div class="footer">
              <table role="presentation">
                <tr>
                  <td>Quito, Ecuador</td>
                </tr>
                <tr>
                  <td>Copyright © $${year} Wallparisoft. Todos los derechos reservados.</td>
                </tr>
              </table>
            </div>
            <div class="footer-bar">
              Este correo fue enviado automáticamente. No respondas a este mensaje.
            </div>
          </div>
        </td>
        <td></td>
      </tr>
    </table>
  </body>
</html>'
where code_param='MAIL_REST_PASS';

update public.params 
set value_param='http://localhost:4200/reset-password/'
where code_param='URL_REST_PASS'; 