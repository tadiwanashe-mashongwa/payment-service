ALTER TABLE payments
    ADD COLUMN provider_reference VARCHAR(128);

CREATE UNIQUE INDEX ux_payments_provider_reference
    ON payments (provider_reference)
    WHERE provider_reference IS NOT NULL;
