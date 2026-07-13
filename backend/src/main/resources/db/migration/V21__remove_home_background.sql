DELETE FROM background_presets
WHERE placement = 'HOME';

ALTER TABLE user_profiles
    DROP COLUMN home_background_url;

ALTER TABLE user_profiles
    DROP COLUMN home_background_type;
