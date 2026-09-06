CREATE TABLE follows (
    follower_id BIGINT NOT NULL,
    followee_id BIGINT NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    PRIMARY KEY (follower_id, followee_id),
    CONSTRAINT fk_follows_follower_id FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follows_followee_id FOREIGN KEY (followee_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_follows_no_self_follow CHECK (follower_id <> followee_id)
);

CREATE INDEX idx_follows_follower_id ON follows (follower_id);
CREATE INDEX idx_follows_followee_id ON follows (followee_id);
