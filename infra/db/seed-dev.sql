INSERT INTO restaurante (id, slug, nome)
VALUES ('00000000-0000-0000-0000-000000000001', 'restaurante-dev', 'Restaurante de desenvolvimento')
ON CONFLICT (slug) DO NOTHING;
