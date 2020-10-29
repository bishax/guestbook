CREATE TABLE users
(login text PRIMARYbKEY,
 password text not null,
 created_at TIMESTAMP not null DEFAULT now());
