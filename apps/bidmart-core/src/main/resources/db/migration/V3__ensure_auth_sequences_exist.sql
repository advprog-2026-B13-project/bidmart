-- Ensure explicit auth sequences exist for Hibernate validation/generation

CREATE SEQUENCE IF NOT EXISTS role_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS permission_seq START WITH 1 INCREMENT BY 1;

SELECT setval('role_seq', COALESCE((SELECT MAX(id) FROM roles), 0) + 1, false);
SELECT setval('permission_seq', COALESCE((SELECT MAX(id) FROM permissions), 0) + 1, false);

