INSERT INTO permissions (id, name)
VALUES
    (nextval('permission_seq'), 'catalog:create_category'),
    (nextval('permission_seq'), 'catalog:update_category'),
    (nextval('permission_seq'), 'catalog:delete_category'),

    (nextval('permission_seq'), 'listing:create_listing'),
    (nextval('permission_seq'), 'listing:update_listing'),
    (nextval('permission_seq'), 'listing:delete_listing'),

    (nextval('permission_seq'), 'order:update_shipment_status'),
    (nextval('permission_seq'), 'order:confirm_delivery')
ON CONFLICT (name) DO NOTHING;
