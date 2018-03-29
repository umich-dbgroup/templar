#!/usr/bin/env python3

import sys

split_tokens = "(),\\+:'"

for line in sys.stdin:
    if len(line.strip()) == 0:
        print()
        continue
    parts = line.strip().split(" ||| ")
    sentence = parts[0]
    logic = parts[1]

    nlogic = []
    for part in logic.split():
        for token in split_tokens:
            replace = ' '+ token +' '
            if token == ',' and '<' in part and '>' in part:
                continue
            part = replace.join(part.split(token))
        nlogic.append(part.strip())
    logic = ' '.join(nlogic)

    # Remove repetition of spaces
    nlogic = []
    for char in logic:
        if len(nlogic) > 0 and nlogic[-1] == char == ' ':
            continue
        nlogic.append(char)
    logic = ''.join(nlogic)

    print(sentence, "|||", ''.join(nlogic).strip())
