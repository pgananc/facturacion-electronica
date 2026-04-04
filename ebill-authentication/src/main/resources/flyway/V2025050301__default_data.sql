INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
VALUES (1, NULL, 'person', 'Cliente', NULL, true);

INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
VALUES (2, NULL, 'inventory', 'Producto', null, true);

INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
VALUES (3, NULL, 'store', 'Compañía', null, true);

INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
VALUES (4, NULL, 'account_circle', 'Usuario', null, true);

INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
VALUES (5, NULL, 'store', 'Facturación Electrónica', NULL, true);

INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
VALUES (6, 5, 'store', 'Facturas', '/billing', true);

INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
VALUES (7, 4, 'store', 'Gestionar usuarios por compañía', '/company-user', true);

INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
VALUES (8, 1, 'person', 'Gestionar clientes', '/client', true);

INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
VALUES (9, 2, 'inventory', 'Gestionar productos', '/product', true);

INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
VALUES (10, 3, 'store', 'Gestionar compañía', '/company', true);

INSERT INTO public.menu (id_menu, parent_id_menu, icon, name, url, status)
VALUES (11, 4, 'account_circle', 'Gestionar usuario', '/user', true);

INSERT INTO public.role (id_role, description, name, status)
VALUES (1, 'Super Admin', 'super_admin', true);
INSERT INTO public.role (id_role, description, name, status)
VALUES (2, 'Miembro', 'member', true);
INSERT INTO public.role (id_role, description, name, status)
VALUES (3, 'Administrador', 'admin', true);

INSERT INTO public.menu_role (id_menu, id_role, id_menu_role, status)
VALUES (1, 1, 1, true),
       (1, 2, 2, true),
       (2, 1, 3, true),
       (2, 2, 4, true),
       (3, 1, 5, true),
       (3, 2, 6, true),
       (3, 3, 7, true),
       (4, 1, 8, true),
       (5, 1, 9, true);

INSERT INTO public.menu_role (id_menu, id_role, id_menu_role, status)
VALUES (6, 1, 10, true),   -- Facturas para Super Admin
       (6, 2, 11, true),  -- Facturas para Miembro

       (7, 1, 12, true),  -- Gestionar usuarios x compañía
       (8, 1, 13, true),  -- Gestionar clientes
       (8, 2, 14, true),  -- Gestionar clientes

       (9, 1, 15, true),  -- Gestionar productos
       (9, 2, 16, true),  -- Gestionar productos

       (10, 1, 17, true), -- Gestionar compañía
       (10, 2, 18, true), -- Gestionar compañía

       (11, 1, 19, true), -- Gestionar usuario
       (11, 3, 20, true); -- Gestionar usuario


INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (1, 'TIME_DURATION_TOKEN', '2022-11-24 13:21:06.015195', 'Tiempo duracion de token', true, '10');
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (4, 'MAIL_REST_PASS', '2022-11-24 14:25:01.307430', 'Template restore password', true, e'<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
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
            <span class="preheader">RESTABLECER CONTRASEÑA - WALLPARISOFT</span>
            <table class="main">

              <!-- START MAIN CONTRASENA -->
              <tr>
                <td class="wrapper">
                  <table border="0" cellpadding="0" cellspacing="0">
                    <tr>
                      <td>
                        Estimado <p>$${user}</p>
                        <p>Debes realizar un restablecimiento de contraseña.</p>
                          <p>El tiempo de duración de link es de $${duration} minutos.</p>
                        <table border="0" cellpadding="0" cellspacing="0" class="btn btn-primary">
                          <tbody>
                            <tr>
                              <td align="left">
                                <table border="0" cellpadding="0" cellspacing="0">
                                  <tbody>
                                    <tr>
                                      <td> <a href="$${resetUrl}">Restablecer Contraseña</a> </td>
                                    </tr>
                                  </tbody>
                                </table>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                        <p>Gracias.</p>
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
                    <a href="http://htmlemail.io">Copyright © #$${year} Wallparisoft</a>.
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
INSERT INTO public.params (id_param, code_param, creation_date, description_param, status, value_param)
VALUES (7, 'URL_REST_PASS', '2022-11-24 16:12:41.206275', 'URL para restaurar password', true,
        'http://localhost:4200/#/reset-password/');

INSERT INTO public.users (id_user, status, password, username, name, mail, creation_date, update_date)
VALUES (1, true, '$2a$10$I/eTiijAKpnudcdCaRPdOem5bPcozedujKtcebfJ78M0cRylqDRsO', '1722633961', 'Jonathan Guerrero', 'jguerreroh@outlook.com', '2022-12-22 08:08:14.490429',
        null);
INSERT INTO public.users (id_user, status, password, username, name, mail, creation_date, update_date)
VALUES (2, true, '$2a$10$I/eTiijAKpnudcdCaRPdOem5bPcozedujKtcebfJ78M0cRylqDRsO', '1803861937', 'Pablo Ganan Culqui', 'pablos.ganan@gmail.com', null,
        '2022-12-19 21:25:15.950219');
INSERT INTO public.users (id_user, status, password, username, name, mail, creation_date, update_date)
VALUES (3, true, '$2a$10$I/eTiijAKpnudcdCaRPdOem5bPcozedujKtcebfJ78M0cRylqDRsO', '1707030901', 'Gustavo Guerrero', 'mail@mail.com', '2022-12-18 23:28:38.570314',
        '2022-12-19 10:12:50.640741');
INSERT INTO public.users (id_user, status, password, username, name, mail, creation_date, update_date)
VALUES (4, true, '$2a$10$I/eTiijAKpnudcdCaRPdOem5bPcozedujKtcebfJ78M0cRylqDRsO', '1803861937001', 'Pepito Perez', 'pablos.ganan1@gmail.com', '2022-12-19 09:54:13.569718',
        '2022-12-19 10:33:55.538441');
INSERT INTO public.users (id_user, status, password, username, name, mail, creation_date, update_date)
VALUES (5, true, '$2a$10$I/eTiijAKpnudcdCaRPdOem5bPcozedujKtcebfJ78M0cRylqDRsO', '1718529934', 'Ana Ceron', 'anny.ceron@gmail.com', '2022-12-19 11:07:42.063563',
        '2022-12-19 11:20:34.903211');
INSERT INTO public.users (id_user, status, password, username, name, mail, creation_date, update_date)
VALUES (6, true, '$2a$10$I/eTiijAKpnudcdCaRPdOem5bPcozedujKtcebfJ78M0cRylqDRsO', '1722633971', 'Priscila Jijon', 'pjijon@mail.com', '2022-12-22 08:08:57.726062', null);

INSERT INTO public.user_company (id_user_company, id_company, id_user, creation_date)
VALUES (1, 1, 1, '2022-12-19 11:13:22.780561');
INSERT INTO public.user_company (id_user_company, id_company, id_user, creation_date)
VALUES (2, 2, 1, '2022-12-16 15:50:02.000000');
INSERT INTO public.user_company (id_user_company, id_company, id_user, creation_date)
VALUES (3, 2, 2, '2022-12-22 08:08:27.683696');
INSERT INTO public.user_company (id_user_company, id_company, id_user, creation_date)
VALUES (4, 2, 3, '2024-09-04 17:46:50.687341');
INSERT INTO public.user_company (id_user_company, id_company, id_user, creation_date)
VALUES (5, 2, 4, '2024-09-04 17:46:56.522336');
INSERT INTO public.user_company (id_user_company, id_company, id_user, creation_date)
VALUES (6, 2, 5, '2024-09-04 17:46:57.289790');
INSERT INTO public.user_company (id_user_company, id_company, id_user, creation_date)
VALUES (7, 2, 6, '2024-09-04 17:46:57.289790');

INSERT INTO public.user_company_role (id_user_company_role, id_user_company, id_role, status)
VALUES
    (1, 1, 1, true), -- Jonathan Guerrero (Super Admin)
    (2, 3, 1, true), -- Pablo Ganan (Super Admin)
    (3, 4, 2, true), -- Gustavo Guerrero (Miembro)
    (4, 5, 2, true), -- Pepito Perez (Miembro)
    (5, 6, 3, true), -- Ana Ceron (Admin)
    (6, 7, 3, true); -- Priscila Jijon (Admin)
