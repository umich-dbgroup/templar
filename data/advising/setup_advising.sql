-- create and populate size table
DROP TABLE IF EXISTS size;
CREATE TABLE size(
    size INTEGER,
    relation VARCHAR(255)
);

INSERT INTO size SELECT COUNT(*), "area" FROM area;
INSERT INTO size SELECT COUNT(*), "comment_instructor" FROM comment_instructor;
INSERT INTO size SELECT COUNT(*), "course" FROM course;
INSERT INTO size SELECT COUNT(*), "course_offering" FROM course_offering;
INSERT INTO size SELECT COUNT(*), "course_prerequisite" FROM course_prerequisite;
INSERT INTO size SELECT COUNT(*), "course_tags_count" FROM course_tags_count;
INSERT INTO size SELECT COUNT(*), "gsi" FROM gsi;
INSERT INTO size SELECT COUNT(*), "instructor" FROM instructor;
INSERT INTO size SELECT COUNT(*), "offering_instructor" FROM offering_instructor;
INSERT INTO size SELECT COUNT(*), "program" FROM program;
INSERT INTO size SELECT COUNT(*), "program_course" FROM program_course;
INSERT INTO size SELECT COUNT(*), "program_requirement" FROM program_requirement;
INSERT INTO size SELECT COUNT(*), "semester" FROM semester;
INSERT INTO size SELECT COUNT(*), "student" FROM student;
INSERT INTO size SELECT COUNT(*), "student_record" FROM student_record;

-- create history table
DROP TABLE IF EXISTS history;
CREATE TABLE history(
    content VARCHAR(1000)
);

-- add fulltext index for text attributes (only run once)
ALTER TABLE course_offering ADD FULLTEXT(textbook);
ALTER TABLE course_offering ADD FULLTEXT(class_address);
