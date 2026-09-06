CREATE TABLE likes (
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_likes_post_id FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_user_id FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uq_likes_post_id_user_id UNIQUE (post_id, user_id)
);

CREATE INDEX idx_likes_post_id ON likes (post_id);
CREATE INDEX idx_likes_user_id ON likes (user_id);
