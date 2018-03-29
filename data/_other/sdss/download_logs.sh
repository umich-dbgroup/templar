extract_and_drop () {
    # extract table so we can get output
    java -jar casjobs.jar extract -b $TABLE_NAME -d

    # drop table
    java -jar casjobs.jar execute -t "MyDB" "DROP TABLE ${TABLE_NAME}" 
}

for YY in 2003 2004 2005 2006 2007 2008 2009 2010 2011 2012
do
    for MO in $(seq -f "%02g" 1 12)
    do
        # FIRST THIRD OF MONTH
        TABLE_NAME=log${YY}_${MO}a
        java -jar casjobs.jar run -t "SdssWeblogs/1" -n "${TABLE_NAME}" "SELECT l.theTime, l.clientIpID, l.server, l.dbname, s.statement INTO mydb.${TABLE_NAME} FROM SqlLog l, SqlStatement s WHERE l.statementID=s.statementID AND l.rows > 0 AND l.error = 0 AND yy = $YY AND mm = $MO AND dd <= 10"
        extract_and_drop

        # SECOND THIRD OF MONTH
        TABLE_NAME=log${YY}_${MO}b
        java -jar casjobs.jar run -t "SdssWeblogs/1" -n "${TABLE_NAME}" "SELECT l.theTime, l.clientIpID, l.server, l.dbname, s.statement INTO mydb.${TABLE_NAME} FROM SqlLog l, SqlStatement s WHERE l.statementID=s.statementID AND l.rows > 0 AND l.error = 0 AND yy = $YY AND mm = $MO AND dd >= 11 AND dd <= 20"
        extract_and_drop

        # FINAL THIRD OF MONTH
        TABLE_NAME=log${YY}_${MO}c
        java -jar casjobs.jar run -t "SdssWeblogs/1" -n "${TABLE_NAME}" "SELECT l.theTime, l.clientIpID, l.server, l.dbname, s.statement INTO mydb.${TABLE_NAME} FROM SqlLog l, SqlStatement s WHERE l.statementID=s.statementID AND l.rows > 0 AND l.error = 0 AND yy = $YY AND mm = $MO AND dd >= 21"
        extract_and_drop

    done
done
