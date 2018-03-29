#!/usr/bin/env python3

import sys
import random

logic_to_queries = {}

for line in sys.stdin:
    if len(line.strip()) == 0:
        continue
    logic = line.strip().split(" ||| ")[1]
    tokens = []
    for token in logic.split():
        tokens.append(token.split(":")[-1])
    logic = ' '.join(tokens)
    logic_to_queries.setdefault(logic, []).append(line.strip())

list_form = []
for logic in logic_to_queries:
    list_form.append((logic, logic_to_queries[logic]))

out_train = open("train", 'w')
out_dev = open("dev", 'w')
out_test = open("test", 'w')

ten_percent = len(list_form) / 10
count = 0
while count < ten_percent:
    case = list_form.pop(random.randint(0, len(list_form) - 1))
    print('\n'.join(case[1]), file=out_dev)
    print(file=out_dev)
    count += 1
count = 0
while count < ten_percent:
    case = list_form.pop(random.randint(0, len(list_form) - 1))
    print('\n'.join(case[1]), file=out_test)
    print(file=out_test)
    count += 1

for case in list_form:
    print('\n'.join(case[1]), file=out_train)
    print(file=out_train)

out_train.close()
out_dev.close()
out_test.close()
