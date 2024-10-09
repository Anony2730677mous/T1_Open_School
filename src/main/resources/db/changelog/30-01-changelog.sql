-- liquibase formatted sql

-- changeset e_cha:1727702549313-1
CREATE SEQUENCE IF NOT EXISTS role_seq START WITH 1 INCREMENT BY 50;

-- changeset e_cha:1727702549313-2
CREATE SEQUENCE IF NOT EXISTS transaction_seq START WITH 1 INCREMENT BY 50;

-- changeset e_cha:1727702549313-3
CREATE SEQUENCE IF NOT EXISTS users_seq START WITH 1 INCREMENT BY 50;

-- changeset e_cha:1727702549313-4
CREATE TABLE role
(
    id   BIGINT NOT NULL,
    name VARCHAR(20),
    CONSTRAINT pk_role PRIMARY KEY (id)
);

-- changeset e_cha:1727702549313-5
CREATE TABLE transaction
(
    id        BIGINT NOT NULL,
    amount    DECIMAL(19, 2),
    client_id BIGINT,
    CONSTRAINT pk_transaction PRIMARY KEY (id)
);

-- changeset e_cha:1727702549313-6
CREATE TABLE user_roles
(
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (role_id, user_id)
);

-- changeset e_cha:1727702549313-7
CREATE TABLE users
(
    id       BIGINT NOT NULL,
    login    VARCHAR(20),
    email    VARCHAR(50),
    password VARCHAR(120),
    CONSTRAINT pk_users PRIMARY KEY (id)
);

-- changeset e_cha:1727702549313-8
ALTER TABLE users
    ADD CONSTRAINT uc_74165e195b2f7b25de690d14a UNIQUE (email);

-- changeset e_cha:1727702549313-9
ALTER TABLE users
    ADD CONSTRAINT uc_f8d2576e807e2b20b506bf6a3 UNIQUE (login);

-- changeset e_cha:1727702549313-10
ALTER TABLE user_roles
    ADD CONSTRAINT fk_userol_on_role FOREIGN KEY (role_id) REFERENCES role (id);

-- changeset e_cha:1727702549313-11
ALTER TABLE user_roles
    ADD CONSTRAINT fk_userol_on_user FOREIGN KEY (user_id) REFERENCES users (id);

-- changeset 12
CREATE TABLE client
(
    id       BIGINT NOT NULL,
    first_name   VARCHAR(50),
    last_name    VARCHAR(50),
    middle_name  VARCHAR(50),
    CONSTRAINT pk_client PRIMARY KEY (id)
);

-- changeset 13
CREATE TABLE client_account(
    id          BIGINT NOT NULL,
    client_id   BIGINT NOT NULL,
    account_type    VARCHAR(50) NOT NULL,
    balance    DECIMAL(50, 2) NOT NULL,
    CONSTRAINT pk_client_account PRIMARY KEY (id),
    CONSTRAINT fk_client_account_client FOREIGN KEY (client_id) REFERENCES client(id)
);


-- changeset 14
CREATE TABLE data_source_error_log
(
    id            BIGINT NOT NULL,
    stack_trace      VARCHAR(256) NOT NULL,
    message          VARCHAR(256) NOT NULL,
    method_signature VARCHAR(256) NOT NULL,
    CONSTRAINT pk_data_source_error_log PRIMARY KEY (id)
);

-- changeset 15
CREATE TABLE time_limit_exceed_log
(
    id            BIGINT NOT NULL,
    method_signature VARCHAR(256) NOT NULL,
    execution_time BIGINT NOT NULL,
    CONSTRAINT pk_time_limit_exceed_log PRIMARY KEY (id)
);