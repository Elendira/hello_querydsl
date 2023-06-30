DROP TABLE IF EXISTS login_user;
CREATE TABLE login_user(
    id INTEGER auto_increment PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    created_by    character varying(255) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
