# IMDB 

This data is from the SQLizer authors.

The SQL dataset can be found at:

https://s3.amazonaws.com/umdb-users/cjbaik/IMDB.sql

## Setup

To enable NaLIR and/or Templar code, make sure to run `setup_imdb.sql` on the database before running.

## Details

The dataset is split up into natural language queries in the `*.nlqs` files, and possible SQL queries in the `*.sqls` files, where each line in the `*.sqls` file can contain multiple possible queries, separated by tabs.

## Deleted Queries

Removed two queries:

* *Find the number of companies which worked with Gabriele Ferzetti*
* *Which producer has worked with the most number of directors?*

because it's not very clear whether the pathway is through `tv_series` or `movie` and we don't support investigating both disjunctively.

We also removed: *Find all movies written and produced by Woody Allen* because it requires two join paths which we don't support.
