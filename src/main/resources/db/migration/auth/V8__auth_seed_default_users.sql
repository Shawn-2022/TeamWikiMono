-- AUTH: seed default users (for local/dev and Swagger testing)
-- Password for all users below: ChangeMe123!

-- bcrypt hash generated with BCrypt (strength 10)
-- Change the password by updating these hashes or creating a new migration.
insert into users (username, password_hash, role)
values
    ('admin1',  '$2b$10$vxF4ht3v4fXVmy3oiaiu7.NgVP9F1.tqpezZkh3hkSyE4Lx7Zcw/i', 'ADMIN'),
    ('editor1', '$2b$10$vxF4ht3v4fXVmy3oiaiu7.NgVP9F1.tqpezZkh3hkSyE4Lx7Zcw/i', 'EDITOR'),
    ('editor2', '$2b$10$vxF4ht3v4fXVmy3oiaiu7.NgVP9F1.tqpezZkh3hkSyE4Lx7Zcw/i', 'EDITOR'),
    ('viewer1', '$2b$10$vxF4ht3v4fXVmy3oiaiu7.NgVP9F1.tqpezZkh3hkSyE4Lx7Zcw/i', 'VIEWER'),
    ('viewer2', '$2b$10$vxF4ht3v4fXVmy3oiaiu7.NgVP9F1.tqpezZkh3hkSyE4Lx7Zcw/i', 'VIEWER'),
    ('viewer3', '$2b$10$vxF4ht3v4fXVmy3oiaiu7.NgVP9F1.tqpezZkh3hkSyE4Lx7Zcw/i', 'VIEWER')
    on conflict (username) do nothing;
