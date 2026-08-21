INSERT INTO categories (name)
VALUES
    ('Electronics'),
    ('Home & Kitchen'),
    ('Books'),
    ('Fitness'),
    ('Office');

INSERT INTO products (name, price, description, category_id)
VALUES
    (
        'Wireless Mechanical Keyboard',
        89.99,
        'Compact mechanical keyboard with wireless connectivity.',
        1
    ),
    (
        'Noise Cancelling Headphones',
        149.99,
        'Over-ear headphones designed to reduce background noise.',
        1
    ),
    (
        'Stainless Steel Water Bottle',
        24.95,
        'Insulated reusable water bottle for hot and cold drinks.',
        2
    ),
    (
        'LED Desk Lamp',
        39.99,
        'Adjustable desk lamp with multiple brightness settings.',
        2
    ),
    (
        'Clean Code Handbook',
        34.50,
        'A practical book about writing readable and maintainable software.',
        3
    ),
    (
        'Java Development Guide',
        42.00,
        'A guide covering modern Java development concepts and practices.',
        3
    ),
    (
        'Resistance Band Set',
        29.99,
        'A set of resistance bands suitable for home workouts.',
        4
    ),
    (
        'Adjustable Dumbbell',
        119.99,
        'A space-saving adjustable dumbbell for strength training.',
        4
    ),
    (
        'Ergonomic Laptop Stand',
        49.99,
        'An adjustable aluminum stand designed to improve laptop ergonomics.',
        5
    ),
    (
        'Wireless Mouse',
        27.99,
        'A comfortable wireless mouse suitable for everyday productivity.',
        5
    );