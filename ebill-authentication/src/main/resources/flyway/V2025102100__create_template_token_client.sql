CREATE TABLE public.client_token (
    -- Clave primaria: ID único para cada registro de token
    id_client_token SERIAL PRIMARY KEY,

    -- Cédula o identificador del cliente.
    client_number VARCHAR(20) NOT NULL,

    -- Correo electrónico del cliente
    client_email VARCHAR(255) NOT NULL,

    -- El token temporal generado para el acceso.
    token VARCHAR(100) NOT NULL UNIQUE,

    -- Fecha y hora de creación del token.
    creation_date timestamp without time zone,
    update_date   timestamp without time zone,

    -- Fecha y hora de caducidad del token (TTL - Time To Live).
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Estado del token: true (ACTIVO) o false (USADO/INVALIDADO/EXPIRADO).
    status BOOLEAN NOT NULL DEFAULT TRUE,

    -- Registro de la fecha y hora en que fue usado (para auditoría).
    used_date TIMESTAMP WITH TIME ZONE NULL
);

-- ----------------------------------------------------------------------
-- Índices para optimizar la búsqueda y la validación rápida
-- ----------------------------------------------------------------------

-- Índice único en el valor del token para búsquedas rápidas al validar
CREATE UNIQUE INDEX idx_ct_token ON public.client_token (token);

-- Índice para buscar rápidamente tokens activos por el identificador del cliente
CREATE INDEX idx_ct_client_active_search ON public.client_token (client_number) WHERE status = TRUE;

-- ----------------------------------------------------------------------
-- Comentarios (Buena práctica)
-- ----------------------------------------------------------------------
COMMENT ON TABLE public.client_token IS 'Almacena tokens temporales para el acceso de clientes a recursos restringidos (ej. facturas).';
COMMENT ON COLUMN public.client_token.token IS 'El token único usado por el cliente para acceder al portal.';
COMMENT ON COLUMN public.client_token.status IS 'Indica si el token está activo (TRUE) o ya fue usado/invalidado (FALSE).';

INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES ((SELECT MAX(id_param) + 1 FROM public.params), 'TIME_DURATION_TOKEN_CLIENT', NOW(), 'Tiempo duracion de token del cliente para visualizar facturas',true, '5');
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES ((SELECT MAX(id_param) + 1 FROM public.params), 'LENGTH_TOKEN', NOW(), 'Longitud de token', true,
        '5');
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES ((SELECT MAX(id_param) + 1 FROM public.params), 'URL_VIEW_INVOICES', NOW(), 'URL para restaurar password', true,
        'http://localhost:4200/#/viewing-invoices/');
        
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES ((SELECT MAX(id_param) + 1 FROM public.params), 'MAIL_TOKEN_CLIENT', '2022-11-24 14:25:01.307430', 'Template restore password', true, '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns:th="http://www.thymeleaf.org" xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <meta name="viewport" content="width=device-width" />
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <title>Simple Transactional Email</title>
    <style>
      /* -------------------------------------
          GLOBAL RESETS
      ------------------------------------- */
      img {
        border: none;
        -ms-interpolation-mode: bicubic;
        max-width: 100%; }
      body {
        background-color: #f6f6f6;
        font-family: sans-serif;
        -webkit-font-smoothing: antialiased;
        font-size: 14px;
        line-height: 1.4;
        margin: 0;
        padding: 0;
        -ms-text-size-adjust: 100%;
        -webkit-text-size-adjust: 100%; }
      table {
        border-collapse: separate;
        mso-table-lspace: 0pt;
        mso-table-rspace: 0pt;
        width: 100%; }
        table td {
          font-family: sans-serif;
          font-size: 14px;
          vertical-align: top; }
      /* -------------------------------------
          BODY CONTAINER
      ------------------------------------- */
      .body {
        background-color: #f6f6f6;
        width: 100%; }
      /* Set a max-width, and make it display as block so it will automatically stretch to that width, but will also shrink down on a phone or something */
      .container {
        display: block;
        Margin: 0 auto !important;
        /* makes it centered */
        max-width: 580px;
        padding: 10px;
        width: 580px; }
      /* This should also be a block element, so that it will fill 100% of the .container */
      .content {
        box-sizing: border-box;
        display: block;
        Margin: 0 auto;
        max-width: 580px;
        padding: 10px; }
      /* -------------------------------------
          HEADER, FOOTER, MAIN
      ------------------------------------- */
      .main {
        background: #ffffff;
        border-radius: 3px;
        width: 100%; }
      .wrapper {
        box-sizing: border-box;
        padding: 20px; }
      .content-block {
        padding-bottom: 10px;
        padding-top: 10px;
      }
      .footer {
        clear: both;
        Margin-top: 10px;
        text-align: center;
        width: 100%; }
        .footer td,
        .footer p,
        .footer span,
        .footer a {
          color: #999999;
          font-size: 12px;
          text-align: center; }
      /* -------------------------------------
          TYPOGRAPHY
      ------------------------------------- */
      h1,
      h2,
      h3,
      h4 {
        color: #000000;
        font-family: sans-serif;
        font-weight: 400;
        line-height: 1.4;
        margin: 0;
        Margin-bottom: 30px; }
      h1 {
        font-size: 35px;
        font-weight: 300;
        text-align: center;
        text-transform: capitalize; }
      p,
      ul,
      ol {
        font-family: sans-serif;
        font-size: 14px;
        font-weight: normal;
        margin: 0;
        Margin-bottom: 15px; }
        p li,
        ul li,
        ol li {
          list-style-position: inside;
          margin-left: 5px; }
      a {
        color: #3498db;
        text-decoration: underline; }
        /* Estilo para el código de acceso */
        .code-box {
            text-align: center;
            margin: 30px 0;
        }
        .code-box p {
            font-size: 18px;
            margin-bottom: 10px;
        }
         .code {
            display: inline-block;
            background-color: #e6f2ff; /* Fondo claro para destacar */
            color: #004d99;
            font-size: 32px;
            font-weight: bold;
            padding: 10px 20px;
            border-radius: 5px;
            border: 1px dashed #004d99;
            letter-spacing: 5px;
        }
      /* -------------------------------------
          BUTTONS
      ------------------------------------- */
      .btn {
        box-sizing: border-box;
        width: 100%; }
        .btn > tbody > tr > td {
          padding-bottom: 15px; }
        .btn table {
          width: auto; }
        .btn table td {
          background-color: #ffffff;
          border-radius: 5px;
          text-align: center; }
        .btn a {
          background-color: #ffffff;
          border: solid 1px #3498db;
          border-radius: 5px;
          box-sizing: border-box;
          color: #3498db;
          cursor: pointer;
          display: inline-block;
          font-size: 14px;
          font-weight: bold;
          margin: 0;
          padding: 12px 25px;
          text-decoration: none;
          text-transform: capitalize; }
      .btn-primary table td {
        background-color: #3498db; }
      .btn-primary a {
        background-color: #3498db;
        border-color: #3498db;
        color: #ffffff; }
      /* -------------------------------------
          OTHER STYLES THAT MIGHT BE USEFUL
      ------------------------------------- */
      .last {
        margin-bottom: 0; }
      .first {
        margin-top: 0; }
      .align-center {
        text-align: center; }
      .align-right {
        text-align: right; }
      .align-left {
        text-align: left; }
      .clear {
        clear: both; }
      .mt0 {
        margin-top: 0; }
      .mb0 {
        margin-bottom: 0; }
      .preheader {
        color: transparent;
        display: none;
        height: 0;
        max-height: 0;
        max-width: 0;
        opacity: 0;
        overflow: hidden;
        mso-hide: all;
        visibility: hidden;
        width: 0; }
      .powered-by a {
        text-decoration: none; }
      hr {
        border: 0;
        border-bottom: 1px solid #f6f6f6;
        Margin: 20px 0; }
      /* -------------------------------------
          RESPONSIVE AND MOBILE FRIENDLY STYLES
      ------------------------------------- */
      @media only screen and (max-width: 620px) {
        table[class=body] h1 {
          font-size: 28px !important;
          margin-bottom: 10px !important; }
        table[class=body] p,
        table[class=body] ul,
        table[class=body] ol,
        table[class=body] td,
        table[class=body] span,
        table[class=body] a {
          font-size: 16px !important; }
        table[class=body] .wrapper,
        table[class=body] .article {
          padding: 10px !important; }
        table[class=body] .content {
          padding: 0 !important; }
        table[class=body] .container {
          padding: 0 !important;
          width: 100% !important; }
        table[class=body] .main {
          border-left-width: 0 !important;
          border-radius: 0 !important;
          border-right-width: 0 !important; }
        table[class=body] .btn table {
          width: 100% !important; }
        table[class=body] .btn a {
          width: 100% !important; }
        table[class=body] .img-responsive {
          height: auto !important;
          max-width: 100% !important;
          width: auto !important; }}
      /* -------------------------------------
          PRESERVE THESE STYLES IN THE HEAD
      ------------------------------------- */
      @media all {
        .ExternalClass {
          width: 100%; }
        .ExternalClass,
        .ExternalClass p,
        .ExternalClass span,
        .ExternalClass font,
        .ExternalClass td,
        .ExternalClass div {
          line-height: 100%; }
        .apple-link a {
          color: inherit !important;
          font-family: inherit !important;
          font-size: inherit !important;
          font-weight: inherit !important;
          line-height: inherit !important;
          text-decoration: none !important; }
        .btn-primary table td:hover {
          background-color: #34495e !important; }
        .btn-primary a:hover {
          background-color: #34495e !important;
          border-color: #34495e !important; } }
    </style>
  </head>
  <body class="">
    <table border="0" cellpadding="0" cellspacing="0" class="body">
      <tr>
        <td></td>
        <td class="container">
          <div class="content">

            <!-- START CENTERED WHITE CONTAINER -->
            <span class="preheader">Acceso a su Portal de Facturas Electrónicas - WALLPARISOFT</span>
            <table class="main">

             
              <tr>
                <td class="wrapper">
                  <table border="0" cellpadding="0" cellspacing="0">
                    <tr>
                      <td>
                        Estimado(a) Cliente: $${user}
                        <p>Hemos generado un código de acceso temporal para que pueda consultar y 
                        descargar las facturas electrónicas emitidas a su nombre. 
                        Este código le permitirá ingresar a nuestro portal de facturación de manera segura.</p>
                          <div class="code-box">
                <p>Su Código de Acceso Temporal es:</p>
                <span class="code">
                    $${token}
                </span>
            </div>
                        <table border="0" cellpadding="0" cellspacing="0" class="btn btn-primary">
                          <tbody>
                            <tr>
                              <td align="left">
                                <table border="0" cellpadding="0" cellspacing="0">
                                  <tbody>
                                   
                                  </tbody>
                                </table>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        
                         <p style="margin-top: 30px;"><b>Vigencia del Código:</b> Por seguridad, este código es válido por $${tokenDuration} minutos.
                          Si el código expira, deberá solicitar uno nuevo en la página de acceso.</p>
            
            <p>Gracias por su preferencia.</p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>

            <!-- END MAIN CONTENT AREA -->
            </table>


            <!-- START FOOTER -->
            <div class="footer">
              <table border="0" cellpadding="0" cellspacing="0">
                <tr>
                  <td class="content-block">
                    <span class="apple-link">Quito, Ecuador</span>
                  </td>
                </tr>
                <tr>
                  <td class="content-block powered-by">
                    <a href="http://htmlemail.io">Copyright © $S{year} Wallparisoft</a>.
                  </td>
                </tr>
              </table>
            </div>
            <!-- END FOOTER -->

          <!-- END CENTERED WHITE CONTAINER -->
          </div>
        </td>
        <td></td>
      </tr>
    </table>
  </body>
</html>');