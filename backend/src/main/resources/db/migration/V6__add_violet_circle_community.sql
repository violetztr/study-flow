ALTER TABLE users
    ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'MEMBER';

ALTER TABLE users
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

CREATE TABLE circles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    visibility VARCHAR(40) NOT NULL DEFAULT 'PUBLIC_REGISTERED',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_circles_slug (slug)
);

CREATE TABLE circle_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_circle_members_circle_user (circle_id, user_id),
    INDEX idx_circle_members_user_id (user_id),
    INDEX idx_circle_members_circle_id (circle_id)
);

CREATE TABLE user_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    display_name VARCHAR(80),
    bio VARCHAR(500),
    avatar_url VARCHAR(500),
    skills VARCHAR(500),
    github_url VARCHAR(300),
    website_url VARCHAR(300),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_profiles_user_id (user_id)
);

CREATE TABLE community_topics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    color VARCHAR(30) NOT NULL DEFAULT '#2f6f60',
    sort_order INT NOT NULL DEFAULT 0,
    post_count INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_community_topics_circle_slug (circle_id, slug),
    INDEX idx_community_topics_circle_id (circle_id)
);

CREATE TABLE community_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    topic_id BIGINT,
    title VARCHAR(160) NOT NULL,
    content TEXT NOT NULL,
    content_format VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    visibility VARCHAR(30) NOT NULL DEFAULT 'CIRCLE',
    status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    comment_count INT NOT NULL DEFAULT 0,
    reaction_count INT NOT NULL DEFAULT 0,
    view_count INT NOT NULL DEFAULT 0,
    last_activity_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    INDEX idx_community_posts_circle_activity (circle_id, pinned, last_activity_at),
    INDEX idx_community_posts_author_id (author_id),
    INDEX idx_community_posts_topic_id (topic_id)
);

CREATE TABLE community_comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
    reaction_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    INDEX idx_community_comments_post_id (post_id),
    INDEX idx_community_comments_author_id (author_id)
);

CREATE TABLE community_reactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reaction_type VARCHAR(30) NOT NULL DEFAULT 'LIKE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_community_reactions_target_user (circle_id, target_type, target_id, user_id, reaction_type),
    INDEX idx_community_reactions_user_id (user_id)
);

CREATE TABLE community_moderation_actions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    circle_id BIGINT NOT NULL,
    admin_user_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    reason VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_community_moderation_actions_target (target_type, target_id),
    INDEX idx_community_moderation_actions_admin (admin_user_id)
);

INSERT INTO circles (name, slug, description)
VALUES ('Violet Circle', 'violet-circle', 'A small community for friends to learn, share, and grow together');

INSERT INTO community_topics (circle_id, name, slug, description, color, sort_order)
SELECT id, '学习', 'learning', '学习进度、技术问题、读书记录', '#2f6f60', 10 FROM circles WHERE slug = 'violet-circle';

INSERT INTO community_topics (circle_id, name, slug, description, color, sort_order)
SELECT id, '笔记', 'notes', '知识沉淀、教程、灵感整理', '#8a5a44', 20 FROM circles WHERE slug = 'violet-circle';

INSERT INTO community_topics (circle_id, name, slug, description, color, sort_order)
SELECT id, '日常', 'daily', '生活记录、碎碎念、日常打卡', '#4f6f8f', 30 FROM circles WHERE slug = 'violet-circle';

INSERT INTO community_topics (circle_id, name, slug, description, color, sort_order)
SELECT id, '项目', 'projects', '作品展示、项目复盘、协作想法', '#6f5f2f', 40 FROM circles WHERE slug = 'violet-circle';
