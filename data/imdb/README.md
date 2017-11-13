# IMDB 

This data is from the SQLizer authors.

The SQL dataset can be found at:

https://s3.amazonaws.com/umich-dbgroup/cjbaik/tb-nalir/IMDB.sql

## Setup

To enable legacy NaLIR code, make sure to run `setup_imdb.sql` on the database before running.

## Details

The dataset is split up into natural language queries in the `*.txt` files, and possible SQL queries in the `*.ans` files, where each line in the `*.ans` file can contain multiple possible queries, separated by tabs.

In addition, while the `*_all.*` files contain all the natural language queries and their corresponding SQL translations, the `c1`, `c2`, etc. break down the set of all queries into levels of different difficulty as assigned by the SQLizer authors.
