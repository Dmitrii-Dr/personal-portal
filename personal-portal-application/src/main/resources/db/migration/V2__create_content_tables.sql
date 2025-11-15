-- Create tags table
CREATE TABLE tags (
    tag_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    slug VARCHAR(255) NOT NULL UNIQUE
);

-- Create media table
CREATE TABLE media (
    media_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_url VARCHAR(1000) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    alt_text VARCHAR(500),
    uploaded_by_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_media_uploaded_by FOREIGN KEY (uploaded_by_id) REFERENCES users(id)
);

-- Create articles table
CREATE TABLE articles (
    article_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    slug VARCHAR(500) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    excerpt TEXT,
    status VARCHAR(20) NOT NULL,
    author_id UUID NOT NULL,
    featured_image_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    CONSTRAINT fk_articles_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_articles_featured_image FOREIGN KEY (featured_image_id) REFERENCES media(media_id)
);

-- Create article_tags join table
CREATE TABLE article_tags (
    article_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    CONSTRAINT fk_article_tags_article FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE CASCADE,
    CONSTRAINT fk_article_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE
);

-- Create article_media join table
CREATE TABLE article_media (
    article_id UUID NOT NULL,
    media_id UUID NOT NULL,
    PRIMARY KEY (article_id, media_id),
    CONSTRAINT fk_article_media_article FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE CASCADE,
    CONSTRAINT fk_article_media_media FOREIGN KEY (media_id) REFERENCES media(media_id) ON DELETE CASCADE
);

-- Create private_article_permissions join table
CREATE TABLE private_article_permissions (
    article_id UUID NOT NULL,
    user_id UUID NOT NULL,
    PRIMARY KEY (article_id, user_id),
    CONSTRAINT fk_private_articles_article FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE CASCADE,
    CONSTRAINT fk_private_articles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX idx_articles_status ON articles(status);
CREATE INDEX idx_articles_author_id ON articles(author_id);
CREATE INDEX idx_articles_slug ON articles(slug);
CREATE INDEX idx_tags_slug ON tags(slug);
CREATE INDEX idx_media_uploaded_by_id ON media(uploaded_by_id);

