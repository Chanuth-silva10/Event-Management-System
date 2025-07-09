INSERT INTO users (id, name, email, password, role) VALUES 
('550e8400-e29b-41d4-a716-446655440001', 'Admin User', 'admin@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBaLIgC/LT.cA2', 'ADMIN');

INSERT INTO users (id, name, email, password, role) VALUES 
('550e8400-e29b-41d4-a716-446655440002', 'Chanuth Silva', 'chanuth@gmail.com', '$2a$10$r8p9W9n3T/UMzJJ4FVBz7um2b2y.3J0i7T4b5h8Q2y2P8h0V6c2w2', 'USER'),
('550e8400-e29b-41d4-a716-446655440003', 'Rikaz Nawzer', 'rikaz@gmail.com', '$2a$10$r8p9W9n3T/UMzJJ4FVBz7um2b2y.3J0i7T4b5h8Q2y2P8h0V6c2w2', 'USER'),
('550e8400-e29b-41d4-a716-446655440004', 'Sachini Karunaratne', 'sachini@gmail.com', '$2a$10$r8p9W9n3T/UMzJJ4FVBz7um2b2y.3J0i7T4b5h8Q2y2P8h0V6c2w2', 'USER');

INSERT INTO events (id, title, description, host_id, start_time, end_time, location, visibility) VALUES 
('650e8400-e29b-41d4-a716-446655440001', 'Spring Boot Workshop', 'Learn Spring Boot development best practices', '550e8400-e29b-41d4-a716-446655440002', '2025-08-15 10:00:00', '2025-08-15 17:00:00', 'Tech Conference Center', 'PUBLIC'),
('650e8400-e29b-41d4-a716-446655440002', 'Team Building Event', 'Annual team building activities', '550e8400-e29b-41d4-a716-446655440003', '2025-08-20 09:00:00', '2025-08-20 18:00:00', 'Adventure Park', 'PRIVATE'),
('650e8400-e29b-41d4-a716-446655440003', 'Code Review Session', 'Monthly code review and knowledge sharing', '550e8400-e29b-41d4-a716-446655440001', '2025-08-25 14:00:00', '2025-08-25 16:00:00', 'Office Meeting Room', 'PUBLIC');

INSERT INTO attendances (event_id, user_id, status) VALUES 
('650e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440003', 'GOING'),
('650e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440004', 'MAYBE'),
('650e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440002', 'GOING'),
('650e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440004', 'GOING'),
('650e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440002', 'GOING'),
('650e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440003', 'GOING');