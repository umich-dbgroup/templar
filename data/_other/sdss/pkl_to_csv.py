import os
import cPickle
import csv
from datetime import datetime

files_by_db = {}

# group files by db
for filename in sorted(os.listdir('pkl')):
    basename = os.path.splitext(filename)[0]
    dbname = basename.split('_', 1)[1]
    dbname = dbname.strip().lower()
    filename = os.path.join('pkl', filename)
    if dbname not in files_by_db:
        files_by_db[dbname] = []
    files_by_db[dbname].append(filename)

for db, info in files_by_db.iteritems():
    all_tuples = []
    for filename in info:
        if os.path.splitext(filename)[1] == '.pkl':
            with open(filename) as f:
                print 'Loading ' + filename
                obj = cPickle.load(f)
                all_tuples.extend(obj)

    # create csv writer
    csv_fname = os.path.join('final', db + '.csv')
    with open(csv_fname, 'wb') as csvfile:
        csvw = csv.writer(csvfile)

        print 'Sorting all tuples...'
        all_tuples = sorted(all_tuples, key=lambda x: datetime.strptime(x['datetime'], '%m/%d/%Y %I:%M:%S %p'))

        # write to CSV file
        print 'Writing to file: ' + csv_fname
        for tup in all_tuples:
            csvw.writerow([tup['datetime'], tup['clientIpID'], tup['db'], tup['stmt']])

print "DONE!"
