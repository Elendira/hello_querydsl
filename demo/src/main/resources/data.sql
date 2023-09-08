INSERT INTO roles2(id, name, created_by) VALUES(1, 'ROLE_GENERAL', 'SYSTEM');
INSERT INTO roles2(id, name, created_by) VALUES(2, 'ROLE_ADMIN', 'SYSTEM');

INSERT INTO login_user(name, created_by) VALUES('MESSI', 'SYSTEM');
INSERT INTO login_user(name, created_by) VALUES('OOTANI', 'SYSTEM');

INSERT INTO user_role(user_id, role_id) VALUES(1, 1);
INSERT INTO user_role(user_id, role_id) VALUES(2, 1);
INSERT INTO user_role(user_id, role_id) VALUES(2, 2);
