# Yelp

This data is from the SQLizer authors.

The SQL dataset can be found at:

https://s3.amazonaws.com/umdb-users/cjbaik/YELP.sql

## Setup

To enable NaLIR and/or Templar code, make sure to run `setup_yelp.sql` on the database before running.

## Details

The dataset is split up into natural language queries in the `*.nlqs` files, and possible SQL queries in the `*.sqls` files, where each line in the `*.sqls` file can contain multiple possible queries, separated by tabs.

## Deleted Query

Deleted previous query: *Find the number of users called Michelle*, because this is an unsupported query where we know the answer will always be either 1 or 0.
