-- Seed ADMIN role and map admin permission

INSERT INTO roles (id, name)
VALUES (nextval('role_seq'), 'ADMIN')
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'admin:all'
WHERE r.name = 'ADMIN'
ON CONFLICT (role_id, permission_id) DO NOTHING;

