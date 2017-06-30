import csv
from random import random

# configurable
filename = 'final/bestdr7.csv'

output_proportion = 0.05
outfile_name = 'final/bestdr7_' + str(output_proportion) + '.csv'

lines_read = 0
lines_written = 0
with open(filename) as f:
    reader = csv.reader(f)
    with open(outfile_name, 'wb') as outfile:
        writer = csv.writer(outfile)
        for row in reader:
            lines_read += 1
            if random() <= output_proportion:
                writer.writerow(row)
                lines_written += 1

print "Lines read: " + str(lines_read)
print "Lines written: " + str(lines_written)


