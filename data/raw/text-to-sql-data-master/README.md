# text-to-sql-data
This is a corpus for building and evaluating systems that map sentences to SQL. We provide:

- Sentences
- SQL queries
- A database schema
- (elsewhere?) a sample database
- (tbd) answers to questions

There can be multiple sentences that all lead to the same SQL query, and there can be multiple SQL queries for the same sentence (either when it is ambiguous or when there are multiple valid queries that are very different).

The repository is structured as follows:

```
LICENSE.txt
README.md
data/
  -- schema.txt
  -- data files, one per question type
tools/
  -- corpus_stats.py [TODO]
  -- pretty_print_schema.py [TODO]
```

The tools are Python 2 and 3 compatible. When run with no arguments they print their usage instructions and exit.

# Data format

Each data file contains one question type, in json format with the following fields:

Symbol      | Type                             | Meaning
----------- | -------------------------------- | -----------------------------
sentence    | string                           | The question type, described by an example question.
paraphrases | list of strings                  | Alternative examples of questions that are paraphrases of the original question once variables are ignored (e.g. course numbers).
info        | list of strings                  | A summary of why this question cannot be answered or other issues [TODO - restrict this to a limited set].
sql         | list of strings                  | A series of SQL queries that correspond to the question.
comments    | list of strings, "USER - ..."    | Any comments, with the person who made them indicated by their initials in all-caps.
accuracy    | list of strings, "USER - [0, 1]" | Used for quality assurance, measuring how accurately the query answers the question.
helpfulness | list of strings, "USER - [0, 1, 2]" | Used for quality assurance, measuring how helpful the information provided by the query is for answering the question.
variables   | list of objects, each with three fields, 'name', 'sql-value', 'sentence-value' | Used for easier generation.
sentence-with-vars | string                    | The sentence with each variable replaced by its name.
sql-with-vars | list of strings                | Each query with variables replaced by their names.

Example:

```
{
    "accuracy": [
        "DRK - 1",
        "JKK - 1"
    ],
    "comments": [
        "JKK - This is an example comment"
    ],
    "helpfulness": [
        "DRK - 2",
        "JKK - 2"
    ],
    "info": [
        "This is some information"
    ],
    "paraphrases": [
        "What major cities are located in Illinois?",
        "List the major cities in Illinois."
    ],
    "sentence": "What are the names of the major cities in Illinois ?",
    "sentence-with-vars": "What are the names of the major cities in state0 ?",
    "sql": [
        "select city.city_name from city where city.state_name='illinois' and city.population>150000;"
    ],
    "sql-with-vars": [
        "select city.city_name from city where city.state_name='state0' and city.population>150000;"
    ],
    "variables": [
        {
            "name": "state0",
            "sql-value": "illinois",
            "sentence-value": "Illinois"
        }
    ],
}
```

When a question has multiple interpretations it appears in multiple files.
At the moment, there is only one SQL query per file, one todo item is to merge these cases where the query does the same thing but differently (as opposed to being a different interpretation of the question).

For release, we will scrub usernames from all of the data. The QA related fields will be retained, but hopefully the scores all become excellent!

The schema is formatted as a series of lines, each describing one field from a table:

- Table name
- Field name
- Type
- Null
- Key
- Default
- Extra

When a value is not set (e.g. default) a `-` is used.

# Summary stats on the data

Sentences | Queries | Formalism | Dataset
--------- | ------- | --------- | -------
880       | 392     | prolog    | geo
800       | 226     | sql       | geo
880       | 310     | logic     | geo
3999      | 180     | sql       | advising
5373      | 1422    | logic     | atis

# Notes on data history

## Advising

1. Collected questions from facebook and undergraduates (past clair students), then wrote further questions of a similar style.
2. Four people (Cathy, Sesh, Harry, Karthik) wrote SQL queries for all of the questions.
3. Six people double annotated the queries for helpfulness and accuracy.
4. Collected paraphrases on mechanical turk, then one person (Jonathan) checked them all, correcting/filtering for major grammatical or correctness issues (plus paraphrases to 6 questions to get us up to 10 for all).

We have received permission to release course related data that was collected via the public API.
Data for PROGRAM_COURSE.workload came from https://www.eecs.umich.edu/eecs/undergraduate/survey/

## Geography

1. Originally a dataset created at UT Austin
2. Prolog converted to SQL at UW
3. Further queries converted and SQL improved at the University of Trento


## Restaurants

Same general idea as Geography
