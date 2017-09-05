#!/usr/bin/env python3

from __future__ import print_function

import argparse
import json
import sys

def read_example(src):
    lines = []
    while True:
        line = src.readline().strip()
        if line == '' or line == '</example>':
            break
        elif len(line) > 0 and line[0] != '<':
            lines.append(line)
    return lines

STATES = [
    "alabama", "alaska", "arizona", "arkansas", "california", "colorado",
    "connecticut", "delaware", "district of columbia", "florida", "georgia",
    "hawaii", "idaho", "illinois", "indiana", "iowa", "kansas", "kentucky",
    "louisiana", "maine", "maryland", "massachusetts", "michigan", "minnesota",
    "mississippi", "missouri", "montana", "nebraska", "nevada",
    "new hampshire", "new jersey", "new mexico", "new york", "north carolina",
    "north dakota", "ohio", "oklahoma", "oregon", "pennsylvania",
    "rhode island", "south carolina", "south dakota", "tennessee", "texas",
    "utah", "vermont", "virginia", "washington", "west virginia", "wisconsin",
    "wyoming"
]
def get_prolog(text):
    text = ' '.join(original.split()[1:])
    for state in STATES:
        text = 'STATE'.join(text.split(state))
    return text

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Converts geoquery data to our format.')
    parser.add_argument('trento_data', help='Questions and SQL queries from the University of Trento (geoQ0.txt)')
    parser.add_argument('ut_queries', help='Questions and logical queries from UT Austin')
    args = parser.parse_args()

    prefix = "question"
    reference_questions = {}
    reference_queries = {}
    for line in open(args.ut_queries):
        original = line.strip()
        line = line.strip().split(']')[0].split('[')[1]
        words = []
        for word in line.split(','):
            if word == "'.'":
                words.append('.')
            else:
                words.append(word)
        prolog = get_prolog(original)
        reference_questions[' '.join(words[:-1])] = (' '.join(words), original, prolog)
        reference_queries[prolog] = set()
    seen = set()

    cur = 0
    src = open(args.trento_data)
    while True:
        text = read_example(src)
        if len(text) == 0:
            break
        trento_question = text[0][1:-1]
        prolog = ''
        if trento_question in reference_questions:
            seen.add(trento_question)
            trento_question, _, prolog = reference_questions[trento_question]
            reference_queries[prolog].add(cur)
        else:
            trento_question = trento_question + " ?"
            print(trento_question, "- is new", cur)

        sql = text[1]

        structure = {
            "sentence": trento_question,
            "sql": sql,
            "paraphrases": [],
            "variables": [],
        }
        num = str(cur)
        while len(num) < 3:
            num = '0' + num
        out = open(prefix + num +".json", 'w')
        print(json.dumps(structure, sort_keys=True, indent=5), file=out)
        out.close()
        cur += 1
    for question in reference_questions:
        if question not in seen:
            text, _, prolog = reference_questions[question]
            print(text, "   ", reference_queries[prolog])
