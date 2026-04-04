CREATE SEQUENCE IF NOT EXISTS user_company_establishment_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.user_company_establishment
(
    id_user_company_establishment BIGINT PRIMARY KEY DEFAULT nextval('user_company_establishment_seq'),
    id_user_company BIGINT NOT NULL,
    id_establishment BIGINT NOT NULL,

    CONSTRAINT fk_user_company_establishment_user_company
        FOREIGN KEY (id_user_company)
            REFERENCES public.user_company (id_user_company)
            ON UPDATE CASCADE ON DELETE CASCADE
);


INSERT INTO public.user_company_establishment (id_user_company_establishment, id_user_company, id_establishment)
VALUES (nextval('user_company_establishment_seq'), 1, 1);

