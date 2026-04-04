create table public.menu
(
    id_menu        integer primary key not null,
    parent_id_menu INTEGER,
    icon           character varying(50),
    name           character varying(200),
    url            character varying(50),
    status         boolean,
    CONSTRAINT fk_menu_parent
        FOREIGN KEY (parent_id_menu) REFERENCES public.menu (id_menu)
            ON UPDATE CASCADE ON DELETE SET NULL
);

create table public.role
(
    id_role     bigint primary key not null,
    description character varying(255),
    name        character varying(255),
    status      boolean
);

create table public.menu_role
(
    id_menu_role bigint primary key not null,
    id_menu      integer            not null,
    id_role      bigint             not null,
    status       boolean,
    foreign key (id_menu) references public.menu (id_menu)
        match simple on update no action on delete no action,
    foreign key (id_role) references public.role (id_role)
        match simple on update no action on delete no action
);

create table public.params
(
    id_param          bigint primary key     not null,
    code_param        character varying(255) not null,
    creation_date     timestamp without time zone,
    description_param character varying(255) not null,
    status            boolean                not null,
    value_param       text                   not null
);
create unique index uk_jc7lv5dojndte62dqgguxnxb4 on params using btree (code_param);

create table public.users
(
    id_user       bigint primary key     not null,
    status        boolean                not null,
    password      character varying(255) not null,
    username      character varying(255) not null,
    name          character varying(255),
    mail          character varying(255),
    creation_date timestamp without time zone,
    update_date   timestamp without time zone
);
create unique index uk_r43af9ap4edm43mmtq01oddj6 on users using btree (username);

create table public.token
(
    id_token        bigint primary key          not null,
    creation_date   timestamp without time zone,
    expiration_date timestamp without time zone not null,
    status          boolean                     not null,
    token           character varying(255)      not null,
    id_user         bigint,
    foreign key (id_user) references public.users (id_user)
        match simple on update no action on delete no action
);
create unique index uk_pddrhgwxnms2aceeku9s2ewy5 on token using btree (token);

create table public.user_company
(
    id_user_company bigint primary key not null,
    id_company      bigint             not null,
    id_user         bigint,
    creation_date   timestamp without time zone,
    foreign key (id_user) references public.users (id_user)
        match simple on update no action on delete no action
);

CREATE TABLE user_company_role
(
    id_user_company_role bigint not null PRIMARY KEY,
    id_user_company      BIGINT NOT NULL,
    id_role              BIGINT NOT NULL,
    status               BOOLEAN DEFAULT TRUE,

    CONSTRAINT uq_user_company_role UNIQUE (id_user_company, id_role),
    FOREIGN KEY (id_user_company) REFERENCES user_company (id_user_company),
    FOREIGN KEY (id_role) REFERENCES role (id_role)
);