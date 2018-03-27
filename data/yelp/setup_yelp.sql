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

-- add fulltext indexes (only run once)
ALTER TABLE checkin ADD FULLTEXT(day);
ALTER TABLE review ADD FULLTEXT(month);
ALTER TABLE review ADD FULLTEXT(text);
ALTER TABLE tip ADD FULLTEXT(month);
ALTER TABLE tip ADD FULLTEXT(text);
ALTER TABLE category ADD FULLTEXT(category_name);
ALTER TABLE user ADD FULLTEXT(name);
ALTER TABLE neighborhood ADD FULLTEXT(neighborhood_name);
ALTER TABLE business ADD FULLTEXT(city);
ALTER TABLE business ADD FULLTEXT(latitude);
ALTER TABLE business ADD FULLTEXT(longitude);
ALTER TABLE business ADD FULLTEXT(full_address);
ALTER TABLE business ADD FULLTEXT(state);
ALTER TABLE business ADD FULLTEXT(name);
