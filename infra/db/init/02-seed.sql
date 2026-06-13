USE blog_mvp;

SET NAMES utf8mb4;

INSERT INTO users (username, password)
SELECT 'admin', '$2b$12$6VvKaXDcdhcBwqRPaWOjAewGPqq6a69Skl1X1X1qPJp60aGQBZi7C'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE username = 'admin'
);

INSERT INTO posts (title, content, author_id, created_at, updated_at)
SELECT
    '欢迎来到 Spring Boot + Vue3 博客系统',
    '这是第一篇示例文章。你可以登录后台发布新文章，并在首页查看最新内容。',
    u.id,
    NOW(),
    NOW()
FROM users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM posts
      WHERE title = '欢迎来到 Spring Boot + Vue3 博客系统'
  );

INSERT INTO posts (title, content, author_id, created_at, updated_at)
SELECT
    'MVP 功能演示指南',
    '建议流程：1) 游客浏览列表与详情；2) 管理员登录后台；3) 发布/编辑/删除文章；4) 验证列表与详情同步。',
    u.id,
    NOW(),
    NOW()
FROM users u
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM posts
      WHERE title = 'MVP 功能演示指南'
  );
