CREATE TABLE posts (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    body       TEXT NOT NULL,
    image_url  VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_posts_user_id FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_posts_user_id ON posts (user_id);
