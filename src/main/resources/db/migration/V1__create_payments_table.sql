CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    amount NUMERIC(38, 2) NOT NULL,
    status VARCHAR(255) NOT NULL
);
