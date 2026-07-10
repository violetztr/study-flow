INSERT INTO circle_members (circle_id, user_id, role, status)
SELECT c.id, u.id, 'MEMBER', 'ACTIVE'
FROM circles c
JOIN users u ON u.status = 'ACTIVE'
WHERE c.slug = 'violet-circle'
  AND NOT EXISTS (
      SELECT 1
      FROM circle_members existing_member
      WHERE existing_member.circle_id = c.id
        AND existing_member.user_id = u.id
  );

INSERT INTO user_profiles (user_id, display_name)
SELECT u.id, u.username
FROM users u
WHERE u.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM user_profiles existing_profile
      WHERE existing_profile.user_id = u.id
  );
