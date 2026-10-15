CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    new_password_flag BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS files (
    id BIGSERIAL PRIMARY KEY,
    original_file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    md_file_path VARCHAR(255),
    file_type VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    max_tokens INTEGER,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_files_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS file_chunks (
    id BIGSERIAL PRIMARY KEY,
    chunk_index INTEGER NOT NULL,
    chunk_name VARCHAR(255) NOT NULL,
    token_count INTEGER,
    file_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    file_path VARCHAR(255),
    CONSTRAINT fk_file_chunks_file
        FOREIGN KEY (file_id) REFERENCES files (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_files_user_id
    ON files (user_id);

CREATE INDEX IF NOT EXISTS idx_file_chunks_file_id
    ON file_chunks (file_id);
