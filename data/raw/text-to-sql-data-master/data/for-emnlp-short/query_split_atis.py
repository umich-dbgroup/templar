#!/usr/bin/env python3

import sys
import random

two_word_states = [
"new hampshire",
"new jersey",
"new mexico",
"north carolina",
"north dakota",
"rhode island",
"south carolina",
"south dakota",
"west virginia",
"new york",
"kansas city",
"lester b. pearson international",
"general mitchell international",
"westchester county",
"baton rouge",
"dallas fort worth",
"des moines",
"fort wayne",
"las vegas",
"long beach",
"los angeles",
"new orleans",
"salt lake city",
"san antonio",
"san diego",
"san francisco",
"san jose",
"scotts valley",
"st. louis",
"st. paul",
]

states = [
"alabama",
"alaska",
"arizona",
"arkansas",
"california",
"colorado",
"delaware",
"florida",
"georgia",
"hawaii",
"idaho",
"illinois",
"indiana",
"iowa",
"kansas",
"kentucky",
"louisiana",
"maine",
"maryland",
"massachusetts",
"michigan",
"minnesota",
"mississippi",
"missouri",
"montana",
"nebraska",
"nevada",
"ohio",
"oregon",
"pennsylvania",
"tennessee",
"texas",
"utah",
"vermont",
"virginia",
"wisconsin",
"wyoming",
"albany",
"atlanta",
"austin",
"baltimore",
"boston",
"boulder",
"burbank",
"charlotte",
"chicago",
"cincinnati",
"cleveland",
"columbus",
"dallas",
"denver",
"detroit",
"dover",
"durham",
"erie",
"flint",
"houston",
"indianapolis",
"kalamazoo",
"lester",
"memphis",
"miami",
"milwaukee",
"minneapolis",
"montgomery",
"montreal",
"nashville",
"newark",
"oakland",
"oklahoma",
"ontario",
"orlando",
"petersburg",
"philadelphia",
"phoenix",
"pittsburgh",
"plano",
"portland",
"quebec",
"riverside",
"rochester",
"sacramento",
"salem",
"seattle",
"spokane",
"springfield",
"tacoma",
"tampa",
"tempe",
"toronto",
"tucson",
]

airline_acronym = [
###"aa",
###"ac",
###"ap",
###"bh",
###"dl",
###"ff",
###"kw",
###"mg",
###"nx",
###"sa",
###"tw",
###"ua",
###"wn",
###"yn",
###"yx",
]

airport_acronym = [
"bna",
"bos",
"bur",
"bwi",
"clt",
"cvg",
"dal",
"dfw",
"dtw",
"ewr",
"hou",
"hpn",
"iad",
"iah",
"jfk",
"lax",
"lga",
"mco",
"mia",
"mke",
"ont",
"ord",
"sfo",
"slc",
"tpa",
"yyz",
]

days = [
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
]

# Note - order of these is important to prevent errors
word_sets = [
    two_word_states,
    states,
    airline_acronym,
    airport_acronym,
    days,
]

data = {}

for line in sys.stdin:
    if len(line.strip()) == 0:
        print()
        continue
    parts = line.strip().split(" ||| ")
    sentence = ' '+ parts[0] +' '
    logic = parts[1]
    ologic = logic

    # Remove inside quotes
    nlogic = []
    in_quote = False
    for char in logic:
        if char == "'":
            in_quote = not in_quote
        elif char in '0987654321':
            char = '0'
            if len(nlogic) > 0 and nlogic[-1] == char:
                continue
        if not in_quote:
            nlogic.append(char)
    logic = ''.join(nlogic)

    for word_set in word_sets:
        for word in word_set:
            word = ' '+ word.upper() +' '
            if word in logic:
                logic = ' _var_ '.join(logic.split(word))

###    print(sentence.strip(), "|||", logic, "|||", ologic)
###    print(sentence.strip(), "|||", logic)
###    print(ologic)
###    print(logic)
    data.setdefault(logic, []).append(line.strip())

list_form = []
for logic in data:
    list_form.append((logic, data[logic]))

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
