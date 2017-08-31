# extract sql only from queries file

import argparse

parser = argparse.ArgumentParser()
parser.add_argument('in_filename')
parser.add_argument('out_filename')
args = parser.parse_args()

with open(args.in_filename) as in_f:
    with open(args.out_filename, 'wb') as out_f:
        for line in in_f:
            sql = line.split('\t')[1].strip()
            out_f.write(sql)
            out_f.write('\n')
