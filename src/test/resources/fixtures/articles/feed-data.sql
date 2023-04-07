INSERT INTO users
VALUES (10, 'jake@example.com', 'jake', 'secret password', 'I work at statefarm', 'https://i.stack.imgur.com/xHWG8.jpg'),
       (20, 'john@example.com', 'john', 'secret password', 'I no longer work at statefarm', 'https://i.stack.imgur.com/xHWG8.jpg'),
       (30, 'bill@example.com', 'bill', 'secret password', 'I work in the bank', 'https://i.stack.imgur.com/xHWG8.jpg'),
       (40, 'michael@example.com', 'michael', 'secret password', 'I no longer work in the bank', 'https://i.stack.imgur.com/xHWG8.jpg');

INSERT INTO followers (user_id, follower_id) VALUES (10, 20), (30, 20);

INSERT INTO articles
VALUES  ('how-to-train-your-dragon', 'How to train your dragon', 'Ever wonder how?', 'It takes a Jacobian',
        1455765776637, 1455767315824, 10),
        ('how-to-train-your-dragon-2', 'How to train your dragon 2', 'So toothless', 'Its a dragon', 1455765776637,
        1455767315824, 10),
        ('how-to-train-your-dragon-3', 'How to train your dragon 3', 'The tagless one', 'Its not a dragon', 1455765776637,
        1455767315824, 20),
        ('how-to-train-your-dragon-4', 'How to train your dragon 4', 'So toothfull', 'Its not a red dragon', 1455765776637,
        1455767315824, 20),
        ('how-to-train-your-dragon-5', 'How to train your dragon 5', 'The tagfull one', 'Its a blue dragon', 1455765776637,
        1455767315824, 30),
        ('how-to-train-your-dragon-6', 'How to train your dragon 6', 'Not wonder how', 'Its not a test dragon', 1455765776637,
        1455767315824, 40);

INSERT INTO tags_articles
VALUES ('dragons', 'how-to-train-your-dragon'),
       ('training', 'how-to-train-your-dragon'),
       ('dragons', 'how-to-train-your-dragon-2'),
       ('goats', 'how-to-train-your-dragon-2'),
       ('training', 'how-to-train-your-dragon-2');

INSERT INTO favorites_articles
VALUES (10, 'how-to-train-your-dragon'),
       (20, 'how-to-train-your-dragon'),
       (20, 'how-to-train-your-dragon-2');