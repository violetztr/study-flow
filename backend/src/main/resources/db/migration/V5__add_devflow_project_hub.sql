CREATE TABLE project_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    headline VARCHAR(160),
    production_url VARCHAR(300),
    api_doc_url VARCHAR(300),
    database_doc_url VARCHAR(300),
    architecture_summary TEXT,
    interview_highlights TEXT,
    cover_image_url VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_profiles_project_id (project_id),
    INDEX idx_project_profiles_user_id (user_id)
);

CREATE TABLE project_tech_stacks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(60) NOT NULL,
    category VARCHAR(40) NOT NULL DEFAULT 'OTHER',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_tech_stacks_project_id (project_id),
    INDEX idx_project_tech_stacks_user_id (user_id)
);

CREATE TABLE github_repositories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    owner VARCHAR(80) NOT NULL,
    repo VARCHAR(120) NOT NULL,
    html_url VARCHAR(300),
    description VARCHAR(500),
    default_branch VARCHAR(80),
    primary_language VARCHAR(80),
    stars INT NOT NULL DEFAULT 0,
    forks INT NOT NULL DEFAULT 0,
    open_issues INT NOT NULL DEFAULT 0,
    pushed_at DATETIME,
    last_synced_at DATETIME,
    readme_present BOOLEAN NOT NULL DEFAULT FALSE,
    languages_json TEXT,
    latest_commits_json TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_github_repositories_project_id (project_id),
    INDEX idx_github_repositories_user_id (user_id)
);

CREATE TABLE portfolio_projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    slug VARCHAR(120) NOT NULL,
    public_visible BOOLEAN NOT NULL DEFAULT FALSE,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    public_summary VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_portfolio_projects_slug (slug),
    UNIQUE KEY uk_portfolio_projects_project_id (project_id),
    INDEX idx_portfolio_projects_visible (public_visible)
);
