#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Tool to check for inconsistencies in the SQL + questions dataset

For a description of the specification, see the README.md.
"""

from __future__ import print_function

import sys
from string import ascii_uppercase

to_fix = set()
for filename in sys.stdin:
    filename = filename.strip()
    has_question = False
    in_sql = False
    info_count = 0
    for line in open(filename):
        if line.startswith("Q - "):
            has_question = len(line) > 5
            in_sql = False
        elif line.startswith("VARIANT - "):
            in_sql = False
        elif line.startswith("INFO - "):
            in_sql = False
            info_count += 1
        elif line.startswith("SQL_QUERY"):
            in_sql = True
        elif line.startswith("SQL - "):
            if not in_sql:
                print(filename, "is missing an SQL_QUERY line before a query")
                to_fix.add(filename)
        elif line.startswith("#"):
            if len(line) < 2 or line[1] not in ascii_uppercase:
                print(filename, "has a comment without a user:", line.strip())
                to_fix.add(filename)
            in_sql = True
        elif line.startswith("ACCURACY_"):
            in_sql = True
        elif line.startswith("HELPFULNESS_"):
            in_sql = True
        else:
            print(filename, "has an unknown line:", line.strip())
            to_fix.add(filename)
            in_sql = False
    if not has_question:
        print(filename, "is missing a question line")
        to_fix.add(filename)
    if info_count > 1:
        print(filename, "has multiple INFO lines")
        to_fix.add(filename)

if len(to_fix) > 0:
    print("\nFiles to fix:\n\n", ' '.join(to_fix))
