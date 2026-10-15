USE community_mall;
ALTER TABLE orders ADD COLUMN refund_reason VARCHAR(500);
ALTER TABLE sys_user ADD COLUMN phone VARCHAR(30);
