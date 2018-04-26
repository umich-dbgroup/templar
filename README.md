# Templar

## Build

We use the gradle shadowJar plugin to build the project.
```
gradle shadowJar
```

## Setup

* Make sure to run `python word2vec_server.py` before (this will use port 10000 by default).
* Ensure your database properties are set correctly in `config.properties` in the main project folder. If this doesn't exist, you should create it by copying `config.properties.example`.
* a functioning MySQL instance with necessary data from each dataset pre-loaded. For example, have MySQL up and running, then create database name `mas` and follow instructions on the [MAS dataset](data/mas) README.

## Running Templar tests

* `TemplarCV` - Runs a cross-validation test on a specific dataset given some parameters.

After building, we can run:
```
java -cp build/libs/templar-all.jar edu.umich.templar.TemplarCV <dataset> <log_level> <log_join_on>
```

Choices for each argument:
* `<dataset>`: `mas`, `yelp`, `imdb`
* `<log_level>`: `full`, `no_const`, `no_const_op`
* `<log_join_on>`: `true`, `false`

## Disabling the candidates cache

Since a lot of keywords are frequently reused in each dataset, we implemented a cache to speed up testing. This can be enabled/disabled by changing the setting for `ENABLE_CACHE` in the `edu.umich.templar.main.settings.Params`.

These caches will be saved in `data/<dataset>/<dataset>.cands.cache`, so to clear the cache, just delete these files.

## Adding new datasets

In order to add new datasets, you need to

1. Load the dataset with name `<dataset>` into MySQL.
2. Create the folder `data/<dataset>`. Each dataset is required to have the following files (see existing datasets for examples):
    * `<dataset>_keywords.csv`: pre-parsed keywords, metadata, and answers. See other datasets for examples. Note specifically that we allow multiple correct answers, separated by semicolons, and that pairs are given in comma-separated form. This formatting matters because our accuracy evaluation is done via string comparison.
    * `<dataset>_joins.csv`: correct join paths for each query. These are in a nested, parenthetical format, where the first table alphabetically is always the first, then a table's children is given by parentheses after it, and multiple children of a tree are separated by commas. For example, `author(organization,writes(publication))` is a join path where `author` is the first alphabetical table name, then its children are `organization` and `writes`, and then `writes` has `publication` as a child. This formatting matters because our accuracy evaluation is done via string comparison.
    * `<dataset>_all.sqls`: the correct SQL labels for each NLQ, one query per line. This is fed in as our query log.
    * `<dataset>.fkpk.json`: a JSON file listing all the foreign key-primary key relationships in the schema
    * `<dataset>.main_attrs.json`: defining the main/display/default attributes for each relation
    * `<dataset>.proj_attrs.json`: defining the paired attributes for each relation



