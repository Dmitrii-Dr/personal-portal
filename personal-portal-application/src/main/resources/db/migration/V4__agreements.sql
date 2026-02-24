-- Agreements tables (final schema)

CREATE TABLE agreements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    slug VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uc_agreements_slug UNIQUE (slug)
);

CREATE INDEX idx_agreements_name ON agreements(name);

CREATE TABLE user_signed_agreements (
    user_id UUID NOT NULL,
    agreement_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    version BIGINT NOT NULL,
    signed_at TIMESTAMPTZ NOT NULL,
    slug VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_user_signed_agreements_user_id ON user_signed_agreements(user_id);
