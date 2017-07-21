#!/usr/bin/env python3

from __future__ import print_function

import argparse
import json
import sys

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Prints stats about the specified files.')
    parser.add_argument('json_file', help='A file in our json format', nargs='+')
    args = parser.parse_args()

    for filename in args.json_file:
        data = json.load(open(filename))
###        print(json.dumps(data, sort_keys=True, indent=5))

        sentence = data['sentence']
        n_paraphrases = 0
        if 'paraphrases' in data:
            n_paraphrases = len(data['paraphrases'])
        n_unchecked = 0
        if 'unchecked-paraphrases' in data:
            n_unchecked = len(data['unchecked-paraphrases'])
        has_sql = len(data['sql']) > 0

        print(has_sql, n_paraphrases, n_unchecked, sentence, filename)

###stats = {
###    "Question types": 0,
###    "Question variants": 0,
###    "Types with queries": 0,
###    "Queries": 0,
###}
###for filename in sys.argv[1:]:
###    stats["Question types"] += 1
###    has_query = False
###    for line in open(filename):
###        if line.startswith("VARIANT"):
###            stats["Question variants"] += 1
###        if line.startswith("SQL_QUERY"):
###            stats["Queries"] += 1
###            has_query = True
###    if has_query:
###        stats["Types with queries"] += 1
###
###for stat in stats:
###    print(stats[stat], stat)
