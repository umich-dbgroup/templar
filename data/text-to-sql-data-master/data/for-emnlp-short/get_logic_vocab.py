#!/usr/bin/env python3

import sys
from collections import defaultdict

counts = defaultdict(lambda: 0)
for line in sys.stdin:
    if len(line.strip()) == 0:
        continue
    line = line.split(" ||| ")[1]
    for token in line.strip().split():
        counts[token] += 1

for token in counts:
    print(counts[token], token)
