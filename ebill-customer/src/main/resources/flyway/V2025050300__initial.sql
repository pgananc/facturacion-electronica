create table client
(
    id_client      bigint      not null
        primary key,
    client_type    integer,
    id_type        integer     not null,
    identification varchar(13) not null,
    name           varchar(255),
    status         boolean,
    creation_date  timestamp,
    update_date    timestamp
);

create table contact
(
    id_contact    bigint     not null
        primary key,
    contact_type  varchar(3) not null,
    value         varchar(255),
    status        boolean    not null,
    creation_date timestamp,
    update_date   timestamp
);



create table company
(
    id_company           bigint      not null
        primary key,
    id_type              integer     not null,
    identification       varchar(13) not null,
    name                 varchar(255),
    name_commercial      varchar(255),
    forced_to_accounting varchar(255),
    special_taxpayer     bigint,
    status               boolean,
    creation_date        timestamp,
    update_date          timestamp
);

create table digital_signature
(
    id_digital_signature bigint not null
        constraint digital_signature_pk
            primary key,
    sign_file            BYTEA,
    password             varchar(255),
    id_company         bigint
        constraint digital_signature_company_id_company_fk
            references company,
    effective_date       timestamp,
    status               boolean,
    creation_date        timestamp,
    update_date          timestamp
);

create table client_contact
(
    id_client_contact bigint not null
        primary key,
    id_client         bigint
        constraint client_contact_client_id_client_fk
            references client,
    id_contact        bigint
        constraint client_contact_contact_id_contact_fk
            references contact,
    status            boolean,
    creation_date     timestamp,
    update_date       timestamp
);

create table company_client
(
    id_company_client      bigint not null
        primary key,
    company_identification varchar(255),
    id_client              bigint
        constraint company_client_client_id_client_fk
            references client,
    creation_date          timestamp
);

create table company_contact
(
    id_company_contact bigint not null
        primary key,
    id_company         bigint
        constraint company_contact_company_id_company_fk
            references company,
    id_contact         bigint
        constraint company_contact_contact_id_contact_fk
            references contact,
    status             boolean,
    creation_date      timestamp,
    update_date        timestamp
);

create table establishment
(
    id_establishment bigint not null
        constraint establishment_pk
            primary key,
    name             varchar(100),
    code             varchar(50),
    address          varchar(255),
    id_company       bigint
        constraint establishment_company_id_company_fk
            references company,
    type             varchar(8),
    status           boolean,
    creation_date    timestamp,
    update_date      timestamp
);

comment on column establishment.type is 'SUCURSAL, MATRIZ';

create table invoice_issuance_point
(
    id_invoice_issuance_point bigint not null
        constraint invoice_issuance_point_pk
            primary key,
    code                      varchar(100),
    sequential_invoice        bigint,
    id_establishment          integer
        constraint invoice_issuance_point_establishment_id_establishment_fk
            references establishment,
    status                    boolean,
    creation_date             timestamp,
    update_date               timestamp
);