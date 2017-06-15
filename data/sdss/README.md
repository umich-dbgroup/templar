# Sloan Data Sky Survey

## Data Information

The first recorded SDSS SQL query begins on 12/23/2002 9:12:12 PM and the last recorded query is on 9/4/2012 11:09:59 PM.

We retrieve data in CSV format using the script `download_logs.sh`. This only selects queries which did not result in errors and also retrieved a non-zero amount of rows from the database. For convenience of scripting, we retrieved logs beginning in 01/01/2003 until 09/04/2012.

We retrieve the following columns for use:

* `theTime` - date/time query was issued
* `clientIpID` - unique ID for client IP (for session information)
* `server`/`dbname` - so we can segment tests across different databases which have unique schemas
* `statement` - the SQL statement
