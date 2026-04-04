CREATE SEQUENCE IF NOT EXISTS user_invoice_point_session_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.user_invoice_point_session
(
    id_user_invoice_point_session BIGINT PRIMARY KEY DEFAULT nextval('user_invoice_point_session_seq'),
    id_user                       BIGINT    NOT NULL,
    id_invoice_issuance_point     BIGINT    NOT NULL,
    start_time                    TIMESTAMP NOT NULL DEFAULT now(),
    end_time                      TIMESTAMP,
    active                        BOOLEAN            DEFAULT TRUE,

    CONSTRAINT fk_user FOREIGN KEY (id_user)
        REFERENCES public.users (id_user)
        ON UPDATE CASCADE ON DELETE CASCADE
);