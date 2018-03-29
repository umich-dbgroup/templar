import os
import pickle
import re

# regex for tuple line
tuple_regex = re.compile('^[0-9]+/[0-9]+/[0-9]+ ');

# map for all tuples
tuples_by_db = {}
tuple_count = 0

# load files in current directory
for filename in sorted(os.listdir('csv')):
    filename = os.path.join('csv', filename)
    if os.path.splitext(filename)[1] == '.csv':
        print 'Iterating through: ' + filename
        with open(filename) as f:
            cur_tuple = {}

            for i, line in enumerate(f):
                if i == 0:
                    continue  # skip header

                m = tuple_regex.match(line)

                if m is not None:    # beginning of a valid tuple
                    if 'stmt' in cur_tuple:
                        if cur_tuple['db'] not in tuples_by_db:
                            tuples_by_db[cur_tuple['db']] = []
                        tuples_by_db[cur_tuple['db']].append(cur_tuple)
                        tuple_count += 1

                        cur_tuple = {}

                    split_line = line.split(',', 4)
                    cur_tuple['datetime'] = split_line[0]
                    cur_tuple['clientIpID'] = split_line[1]
                    cur_tuple['db'] = split_line[2] + '_' + split_line[3]
                    cur_tuple['stmt'] = split_line[4]
                else:
                    cur_tuple['stmt'] += line
        print 'Total tuples so far: ' + str(tuple_count)
        print 'Finished with: ' + filename

# pickle tuples_by_db to file
for db, tuples in tuples_by_db.iteritems():
    fname = os.path.join('pkl', db + '.pkl')
    with open(fname, 'wb') as f:
        print 'Saving file: ' + fname
        pickle.dump(tuples, f)
