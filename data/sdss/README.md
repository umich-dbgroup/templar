# Sloan Data Sky Survey

## Data Information

The first recorded SDSS SQL query begins on 12/23/2002 9:12:12 PM and the last recorded query is on 9/4/2012 11:09:59 PM. As we examined the SQL log data, however, we find that the initial logs from late 2002/most of 2003 have truncated SQL queries that were not logged correctly. As such, we examine the data for:

* Log Start Date: 11/11/2003
* Log End Date: TBD

We retrieve data in CSV format using the script `download_logs.sh`. This only selects queries which did not result in errors and also retrieved a non-zero amount of rows from the database.

*Note:* Result tuples are not ordered by date by default within each CSV file.

We retrieve the following columns for use:

* `theTime` - date/time query was issued
* `clientIpID` - unique ID for client IP (for session information)
* `server`/`dbname` - so we can segment tests across different databases which have unique schemas
* `statement` - the SQL statement
