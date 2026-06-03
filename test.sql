-- test.sql: 新增三筆 users（ADMIN、VENDOR、ORGANIZER）作為測試資料
-- 使用 dbo.users 表格欄位：role, name, email, password_hash, phone, provider, status
INSERT INTO dbo.users (role, name, email, password_hash, phone, provider, status) VALUES
  ('ADMIN', 'Admin User', 'admin@example.test', 'hash_admin_!@#', '+886912345678', 'LOCAL', 'ACTIVE'),
  ('VENDOR', 'Vendor User', 'vendor@example.test', 'hash_vendor_%^&', '+886912345679', 'LOCAL', 'ACTIVE'),
  ('ORGANIZER', 'Organizer User', 'organizer@example.test', 'hash_organizer_*()_', '+886912345680', 'LOCAL', 'ACTIVE');
