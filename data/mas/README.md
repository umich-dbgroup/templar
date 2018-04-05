# MAS Dataset

The MAS dataset and queries were obtained from Fei Li for his former work in NaLIR.

The SQL dataset can be found at:

https://s3.amazonaws.com/umdb-users/cjbaik/mas.sql

After loading this dataset into your database, you might need to execute `setup_mas.sql` to make the database work on the NaLIR and/or Templar system.

## Deleted Queries

Deleted query: *return me the authors who have more papers on VLDB than ICDE.* because it has nested subqueries with correlation, as well as *return me the number of citations of "Making database systems usable" before 2010.* because it is unclear.
