# Sapphire Dataset Cross-validation Experiments

## Overview

We ran experiments to see the coverage we get from creating 4 folds for cross-validation for the Sapphire (restaurants, advising, geography-new) datasets.

## Running Experiments

```
java -cp build/libs/tb-nalir-all.jar edu.umich.tbnalir.SchemaAndLogTemplateGenerator data/text-to-sql-data-master/restaurants data/text-to-sql-data-master/restaurants.parsed 1 cv 1234 true | tee results/sapphire_cv/restaurants.join1.cv.1234.out
```

Program arguments:

* `schema_prefix` - if you use `data/text-to-sql-data-master/restaurants`, it will get files from `data/text-to-sql-data-master/restaurants.relations.json` and `data/text-to-sql-data-master/restaurants.edges.json` (only really used if `use_schema_templates` below is true)
* `parsed_sql_file` - filepath for the query log, with one SQL query per line
* `join_level` - number of joins to create templates for by default, with 0 being 1 table templates, 1 being 2 table templates, and so on. (only really used if `use_schema_templates` below is true)
* `cv` - the literal string `cv`, just used to indicate that we're running a cross-validation test
* `random_seed` - an integer to seed the randomizer for shuffling the SQL query log
* `use_schema_templates` - whether to use schema templates or not, will only apply to `pred_proj` templates

## Results

[Sapphire Datasets Coverage](https://docs.google.com/a/umich.edu/spreadsheets/d/1mViW3nc1ArJEp0a63CRnhV8ff1eGPNqV-OatyQ4f9RQ/edit?usp=drive_web)
