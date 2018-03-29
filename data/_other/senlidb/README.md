# SENLIDB Dataset

We obtained the dataset from https://github.com/johnthebrave/nlidb-datasets.

After running it through JSQLParser and removing queries it could not parse as well as non-SELECT statements (CREATE TABLE statements), we result in:

* 22518 training SQL queries
* 286 test SQL queries
