CREATE TABLE user_signed_agreements (
    user_id UUID NOT NULL,
    agreement_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    version BIGINT NOT NULL,
    signed_at TIMESTAMPTZ NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_user_signed_agreements_user_id ON user_signed_agreements(user_id);
