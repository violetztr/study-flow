DROP TABLE IF EXISTS portfolio_projects;
DROP TABLE IF EXISTS github_repositories;
DROP TABLE IF EXISTS project_tech_stacks;
DROP TABLE IF EXISTS project_profiles;

DROP TABLE IF EXISTS habit_records;
DROP TABLE IF EXISTS habits;
DROP TABLE IF EXISTS journals;
DROP TABLE IF EXISTS daily_plans;

DROP TABLE IF EXISTS note_blocks;
DROP TABLE IF EXISTS notes;

DROP TABLE IF EXISTS task_tags;
DROP TABLE IF EXISTS tags;
DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS projects;

UPDATE circles
SET name = 'Ruru 社区',
    slug = 'ruru-community',
    description = '一个先从朋友小圈子开始的轻量社区'
WHERE slug = 'violet-circle';

UPDATE community_topics
SET name = '公告',
    slug = 'announcements',
    description = '社区规则、重要通知、版本更新',
    color = '#2f6f60',
    sort_order = 10
WHERE slug = 'learning';

UPDATE community_topics
SET name = '闲聊',
    slug = 'chat',
    description = '朋友之间的随手交流和近况',
    color = '#8a5a44',
    sort_order = 20
WHERE slug = 'notes';

UPDATE community_topics
SET name = '求助',
    slug = 'help',
    description = '问题求助、经验互助、踩坑记录',
    color = '#4f6f8f',
    sort_order = 30
WHERE slug = 'daily';

UPDATE community_topics
SET name = '分享',
    slug = 'share',
    description = '好东西、链接、作品和想法分享',
    color = '#6f5f2f',
    sort_order = 40
WHERE slug = 'projects';
