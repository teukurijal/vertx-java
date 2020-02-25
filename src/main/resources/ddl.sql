-- Create the products table if not present
-- CREATE TABLE IF NOT EXISTS products (
--   id        SERIAL PRIMARY KEY,
--   name      VARCHAR(40) NOT NULL,
--   stock     BIGINT
-- );

-- DELETE FROM private.products;

-- INSERT INTO private.products (name, stock) values ('Apple', 10);
-- INSERT INTO private.products (name, stock) values ('Orange', 10);
-- INSERT INTO private.products (name, stock) values ('Pear', 10);


-- ************************************** "category"

-- CREATE TABLE private.category
-- (
--  id          integer NOT NULL,
--  name        varchar(50) NOT NULL,
--  description varchar(50) NOT NULL,
--  CONSTRAINT PK_category PRIMARY KEY ( id )
-- );


-- -- ************************************** "form_request"

-- CREATE TABLE private.form_request
-- (
--  id             integer NOT NULL,
--  subject       varchar(50) NOT NULL,
--  description   varchar(50) NOT NULL,
--  file          text NOT NULL,
--  approval_type varchar(50) NOT NULL,
--  category_id   integer NOT NULL,
--  CONSTRAINT PK_request PRIMARY KEY ( id ),
--  CONSTRAINT FK_103 FOREIGN KEY ( category_id ) REFERENCES private.category ( id )
-- );

-- CREATE INDEX fkIdx_103 ON form_request
-- (
--  category_id
-- );

-- *************** SqlDBM: PostgreSQL ****************;
-- ***************************************************;


-- ************************************** "user_approval"

-- CREATE TABLE private.user_approval
-- (
--  id           integer NOT NULL,
--  full_name    varchar(50) NOT NULL,
--  position     varchar(50) NOT NULL,
--  gender       varchar(50) NOT NULL,
--  email        varchar(50) NOT NULL,
--  phone_number varchar(50) NOT NULL,
--  CONSTRAINT PK_user_approval PRIMARY KEY ( id )
-- );


-- -- ************************************** "juction_table"

-- CREATE TABLE private.log_form
-- (
--  form_id          integer NOT NULL,
--  user_approval_id integer NOT NULL,
--  approval_status  varchar(50) NOT NULL,
--  is_show          boolean NOT NULL,
--  CONSTRAINT FK_122 FOREIGN KEY ( form_id ) REFERENCES private.form_request ( id ),
--  CONSTRAINT FK_127 FOREIGN KEY ( user_approval_id ) REFERENCES private.user_approval ( id )
-- );

-- CREATE INDEX fkIdx_122 ON private.log_form
-- (
--  form_id
-- );

-- CREATE INDEX fkIdx_127 ON private.log_form
-- (
--  user_approval_id
-- );







