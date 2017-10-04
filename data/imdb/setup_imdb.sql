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

-- add fulltext indexes needed (only run once)
ALTER TABLE actor ADD FULLTEXT(birth_city);
ALTER TABLE actor ADD FULLTEXT(birth_year);
ALTER TABLE actor ADD FULLTEXT(nationality);
ALTER TABLE actor ADD FULLTEXT(gender);
ALTER TABLE actor ADD FULLTEXT(name);
ALTER TABLE cast ADD FULLTEXT(role);
ALTER TABLE company ADD FULLTEXT(name);
ALTER TABLE company ADD FULLTEXT(country_code);
ALTER TABLE director ADD FULLTEXT(birth_city);
ALTER TABLE director ADD FULLTEXT(birth_year);
ALTER TABLE director ADD FULLTEXT(nationality);
ALTER TABLE director ADD FULLTEXT(gender);
ALTER TABLE director ADD FULLTEXT(name);
ALTER TABLE genre ADD FULLTEXT(genre);
ALTER TABLE keyword ADD FULLTEXT(keyword);
ALTER TABLE movie ADD FULLTEXT(title);
ALTER TABLE movie ADD FULLTEXT(title_aka);
ALTER TABLE movie ADD FULLTEXT(budget);
ALTER TABLE producer ADD FULLTEXT(birth_city);
ALTER TABLE producer ADD FULLTEXT(birth_year);
ALTER TABLE producer ADD FULLTEXT(nationality);
ALTER TABLE producer ADD FULLTEXT(gender);
ALTER TABLE producer ADD FULLTEXT(name);
ALTER TABLE tv_series ADD FULLTEXT(title);
ALTER TABLE tv_series ADD FULLTEXT(title_aka);
ALTER TABLE tv_series ADD FULLTEXT(budget);
ALTER TABLE writer ADD FULLTEXT(birth_city);
ALTER TABLE writer ADD FULLTEXT(birth_year);
ALTER TABLE writer ADD FULLTEXT(nationality);
ALTER TABLE writer ADD FULLTEXT(gender);
ALTER TABLE writer ADD FULLTEXT(name);
