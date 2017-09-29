-- create and populate size table
DROP TABLE IF EXISTS size;
CREATE TABLE size(
    size INTEGER,
    relation VARCHAR(255)
);

INSERT INTO size SELECT COUNT(*), "business" FROM business;
INSERT INTO size SELECT COUNT(*), "category" FROM category;
INSERT INTO size SELECT COUNT(*), "checkin" FROM checkin;
INSERT INTO size SELECT COUNT(*), "neighborhood" FROM neighborhood;
INSERT INTO size SELECT COUNT(*), "review" FROM review;
INSERT INTO size SELECT COUNT(*), "tip" FROM tip;
INSERT INTO size SELECT COUNT(*), "user" FROM user;

-- create history table
DROP TABLE IF EXISTS history;
CREATE TABLE history(
    content VARCHAR(1000)
);

-- add fulltext index for publication (only run once)
-- ALTER TABLE publication ADD FULLTEXT(title);
-- ALTER TABLE publication ADD FULLTEXT(abstract);
