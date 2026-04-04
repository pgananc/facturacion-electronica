create table tax_type
(
    id_tax_type   bigint    not null
        primary key,
    type          varchar(3),
    name          varchar(255),
    value         numeric(38, 2),
    status        boolean   not null,
    creation_date timestamp not null,
    update_date   timestamp
);

comment on column tax_type.type is 'IVA,ICE';

create table product
(
    id_product      bigint  not null
        primary key,
    id_company      bigint  not null,
    main_code       varchar(255),
    name            varchar(255),
    product_type    integer,
    unit_price      double precision,
    description     varchar(255),
    has_ice         boolean,
    has_tourism_iva boolean,
    status          boolean not null,
    creation_date   timestamp,
    update_date     timestamp
);