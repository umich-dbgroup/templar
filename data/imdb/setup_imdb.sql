-- create and populate size table
DROP TABLE IF EXISTS size;
CREATE TABLE size(
    size INTEGER,
    relation VARCHAR(255)
);

INSERT INTO size SELECT COUNT(*), "actor" FROM actor;
INSERT INTO size SELECT COUNT(*), "cast" FROM cast;
INSERT INTO size SELECT COUNT(*), "classification" FROM classification;
INSERT INTO size SELECT COUNT(*), "company" FROM company;
INSERT INTO size SELECT COUNT(*), "copyright" FROM copyright;
INSERT INTO size SELECT COUNT(*), "directed_by" FROM directed_by;
INSERT INTO size SELECT COUNT(*), "director" FROM director;
INSERT INTO size SELECT COUNT(*), "genre" FROM genre;
INSERT INTO size SELECT COUNT(*), "keyword" FROM keyword;
INSERT INTO size SELECT COUNT(*), "made_by" FROM made_by;
INSERT INTO size SELECT COUNT(*), "movie" FROM movie;
INSERT INTO size SELECT COUNT(*), "producer" FROM producer;
INSERT INTO size SELECT COUNT(*), "tags" FROM tags;
INSERT INTO size SELECT COUNT(*), "tv_series" FROM tv_series;
INSERT INTO size SELECT COUNT(*), "writer" FROM writer;
INSERT INTO size SELECT COUNT(*), "written_by" FROM written_by;

-- create history table
DROP TABLE IF EXISTS history;
CREATE TABLE history(
    content VARCHAR(1000)
);

-- add fulltext index for publication (only run once)
-- ALTER TABLE publication ADD FULLTEXT(title);
-- ALTER TABLE publication ADD FULLTEXT(abstract);
