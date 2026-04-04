create table payment_method
(
    id_payment_method   bigint    not null
        primary key,
    payment_method_type varchar(255),
    payment_method_code varchar(2),
    status              boolean,
    creation_date       timestamp not null,
    update_date         timestamp
);

create table authorization_sri
(
    id_authorization_sri bigint not null
        primary key,
    access_key           character varying(255),
    authorization_date   timestamp,
    authorization_number character varying(255),
    status               boolean,
    error                text,
    xml                  text,
    creation_date        timestamp,
    update_date          timestamp,
    status_sri           character varying(255)
);

create table invoice
(
    id_invoice             bigint    not null
        primary key,
    additional_information varchar(255),
    environment            varchar(255),
    id_client              bigint,
    id_company             bigint,
    issue_date             timestamp,
    invoice_number         varchar(255),
    subtotal_ice           numeric(38, 2),
    subtotal_iva           numeric(38, 2),
    subtotal_zero          numeric(38, 2),
    discount               numeric(38, 2),
    tip                    numeric(38, 2),
    total_amount           numeric(38, 2),
    id_payment_method      bigint
        constraint invoice_payment_method_id_payment_method_fk
            references payment_method,
    id_authorization_sri   bigint
        constraint invoice_authorization_sri_id_authorization_sri_fk
            references authorization_sri,
    status                 boolean,
    creation_date          timestamp not null,
    update_date            timestamp
);

create table invoice_detail
(
    id_invoice_detail bigint    not null
        primary key,
    discount          numeric(38, 2),
    id_product        bigint,
    quantity          numeric,
    subtotal          numeric(38, 2),
    unit_price        numeric(38, 2),
    id_invoice        bigint
        constraint invoice_detail_invoice_id_invoice_fk
            references invoice,
    status            boolean,
    creation_date     timestamp not null,
    update_date       timestamp
);