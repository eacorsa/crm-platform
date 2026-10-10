-- Apply before deploying with spring.jpa.hibernate.ddl-auto=validate.
-- No passwords or existing user records are changed.
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
