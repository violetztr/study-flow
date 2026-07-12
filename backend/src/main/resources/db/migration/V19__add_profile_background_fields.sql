ALTER TABLE user_profiles
    ADD COLUMN profile_background_url VARCHAR(500);

ALTER TABLE user_profiles
    ADD COLUMN profile_background_type VARCHAR(20) DEFAULT 'IMAGE';
