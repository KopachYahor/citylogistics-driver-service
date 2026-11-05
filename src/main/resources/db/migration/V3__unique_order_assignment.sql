-- Один заказ — не больше одного назначения водителя.
-- Проверка existsByOrderId в OrderCreatedListener отсекает повторное событие, но если
-- два экземпляра сервиса обработают одно событие одновременно, оба пройдут проверку.
-- UNIQUE в базе не даст вставить вторую запись: транзакция второго экземпляра откатится.
DROP INDEX IF EXISTS idx_assignments_order_id; -- UNIQUE создаёт свой индекс, обычный больше не нужен
ALTER TABLE order_assignments ADD CONSTRAINT uk_order_assignments_order_id UNIQUE (order_id);
