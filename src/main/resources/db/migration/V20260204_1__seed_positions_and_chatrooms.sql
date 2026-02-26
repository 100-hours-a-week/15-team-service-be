INSERT INTO positions (name)
SELECT '백엔드'
WHERE NOT EXISTS (SELECT 1 FROM positions WHERE name = '백엔드');

INSERT INTO positions (name)
SELECT '프론트엔드'
WHERE NOT EXISTS (SELECT 1 FROM positions WHERE name = '프론트엔드');

INSERT INTO positions (name)
SELECT '풀스택'
WHERE NOT EXISTS (SELECT 1 FROM positions WHERE name = '풀스택');

INSERT INTO positions (name)
SELECT '데이터'
WHERE NOT EXISTS (SELECT 1 FROM positions WHERE name = '데이터');

INSERT INTO positions (name)
SELECT 'AI'
WHERE NOT EXISTS (SELECT 1 FROM positions WHERE name = 'AI');

INSERT INTO positions (name)
SELECT 'DevOps'
WHERE NOT EXISTS (SELECT 1 FROM positions WHERE name = 'DevOps');

INSERT INTO positions (name)
SELECT '모바일'
WHERE NOT EXISTS (SELECT 1 FROM positions WHERE name = '모바일');

INSERT INTO positions (name)
SELECT '보안'
WHERE NOT EXISTS (SELECT 1 FROM positions WHERE name = '보안');

INSERT INTO position_chat_chatroom (created_at, updated_at, position_id)
SELECT NOW(6), NOW(6), p.id
FROM positions p
WHERE p.name IN ('백엔드', '프론트엔드', '풀스택', '데이터', 'AI', 'DevOps', '모바일', '보안')
  AND NOT EXISTS (
    SELECT 1
    FROM position_chat_chatroom c
    WHERE c.position_id = p.id
  );
