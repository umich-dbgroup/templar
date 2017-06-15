# Sloan Data Sky Survey

## Data Information

The first recorded SDSS SQL query begins on 12/23/2002 9:12:12 PM and the last recorded query is on 9/4/2012 11:09:59 PM.

We retrieve data in CSV format using the script `get_log.sh`. This only selects queries which did not result in errors and also retrieved a non-zero amount of rows from the database.

In addition, we retain information the following meta information for use:

* `theTime` - date/time query was issued
* `clientIpID` - unique ID for client IP (for session information)
* `server`/`dbname` - so we can segment tests across different databases which have unique schemas
