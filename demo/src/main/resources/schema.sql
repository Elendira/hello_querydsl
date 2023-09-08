DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS login_user;
DROP TABLE IF EXISTS roles2;
CREATE TABLE roles2 (
    id INTEGER PRIMARY KEY NOT NULL AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,
    created_by    character varying(255) NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE login_user(
    id INTEGER auto_increment PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    created_by    character varying(255) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE user_role(
    user_id INTEGER NOT NULL,
    role_id INTEGER NOT NULL,
    begin_date DATE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_user_role PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user_id FOREIGN KEY (user_id) REFERENCES login_user(id),
    CONSTRAINT fk_user_role_role_id FOREIGN KEY (role_id) REFERENCES roles2(id)
);
