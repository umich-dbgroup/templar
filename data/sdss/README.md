# Sloan Data Sky Survey

## Step 1: Retrieving data from SDSS servers

The first recorded SDSS SQL query begins on 12/23/2002 9:12:12 PM and the last recorded query is on 9/4/2012 11:09:59 PM. As we examined the SQL log data, however, we find that the initial logs from late 2002/most of 2003 have truncated SQL queries that were not logged correctly. As such, we examine the data for:

* Log Start Date: 11/11/2003
* Log End Date: 06/10/2012
* Total Tuples: 94,352,888

We retrieve data in CSV format using the script `download_logs.sh` into the `csv/` folder. This only selects queries which did not result in errors and also retrieved a non-zero amount of rows from the database.

*Notes:*

* Result tuples are not ordered by date by default within each CSV file.
* SDSS CSV files are not correctly formatted and quoted (i.e. the SQL statements incorrectly contain commas that mess with CSV formatting).

We retrieve the following columns for use:

* `theTime` - date/time query was issued
* `clientIpID` - unique ID for client IP (for session information)
* `server`/`dbname` - so we can segment tests across different databases which have unique schemas
* `statement` - the SQL statement

## Step 2: Reorganizing data by server/database

We take the resulting CSV files and run `python reorg_by_schema.py` to group tuples by schema (a specific server/database pair) instead of simply ordering them by date. We write the results to `pkl/<server>_<db>.pkl`, so they can easily be accessed by unpickling them in Python.

## Step 3: Converting pickled files to CSV

*Note:* This is inefficient because we could have done away with Step 2, but didn't think of grouping by `<db>` and not `<server>/<db>` pairs at first so this is what we get.

In `python pkl_to_csv.py`, we take the pickled files and:

* Group them by database name
* Sort them by ascending date

So the final results land in `final/<dbname>.csv` as well-formatted CSV files for each database. We chose the top 8 query logs (which all have > 100M data), as follows:

* `bestdr7.csv` (18G)
* `bestdr5.csv` (5.6G)
* `bestdr6.csv` (4.1G)
* `bestdr4.csv` (3.0G)
* `bestdr2.csv` (1.7G)
* `bestdr3.csv` (1.2G)
* `bestdr1.csv` (987G)
* `bestdr8.csv` (594G)
