import argparse
import json
import os

parser = argparse.ArgumentParser()
parser.add_argument('input_dir')
parser.add_argument('outfile_name')
args = parser.parse_args()

sqls = []
for filename in os.listdir(args.input_dir):
    if filename.endswith('.json'):
        with open(os.path.join(args.input_dir, filename)) as f:
            question = json.load(f)
            if 'sql' in question:
                if type(question['sql']) == list:
                    for s in question['sql']:
                        sqls.append(s)
                else:
                    sqls.append(question['sql'])

with open(args.outfile_name, 'wb') as f:
    for sql in sqls:
        f.write(sql + '\n')
