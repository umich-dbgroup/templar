#!/usr/bin/env python3

import sys

# TODO: make 2 word states and do first

two_word_states = [
    "new hampshire", "new jersey", "new mexico", "south carolina", "north carolina",
    "west virginia", "rhode island", "north dakota", "south dakota"
]

states = [
    "alabama", "alaska", "arizona", "arkansas", "california", "colorado",
    "delaware", "florida", "georgia", "hawaii", "idaho", "illinois", "indiana",
    "iowa", "kansas", "kentucky", "louisiana", "maine", "maryland",
    "massachusetts", "michigan", "minnesota", "mississippi", "missouri",
    "montana", "nebraska", "nevada", "ohio", "oregon", "pennsylvania",
    "tennessee", "utah", "vermont", "virginia", "wisconsin",
    "wyoming", "texas", "washington"
]

multi_word_cities = [
    "baton rouge",
    "des moines", 
    "fort wayne", 
    "new orleans",
    "salt lake city", "san antonio", "san diego", "san francisco", "san jose", "scotts valley",
]

cities = [
    "albany", "atlanta", "austin",
    "boston", "boulder", "chicago",
    "columbus", "dallas", "denver", 
    "detroit", "dover", "durham",
    "erie", "flint", 
    "houston", "indianapolis", "kalamazoo", "miami",
    "montgomery", 
    "oklahoma", "pittsburgh", "plano", "portland",
    "riverside", "rochester", "sacramento", "salem",
    "seattle", "spokane", "springfield", "tempe", "tucson",
    "boulder", 
    "minneapolis"
]

places = [
    "guadalupe peak", "death valley", "mount mckinley", "mount whitney"
]

rivers = [
    "north platte",
    "rio grande", "hudson", "platte", "potomac", "chattahoochee", "red",
]

country = [
    "usa",
    "us",
    "united states",
    "america",
]

# Note - order of these is important to prevent errors
word_sets = [
    ("r", rivers),
    ("s", two_word_states),
    ("s", states),
    ("c", multi_word_cities),
    ("c", cities),
    ("p", places),
]

for line in sys.stdin:
    if len(line.strip()) == 0:
        print()
        continue
    parts = line.strip().split(" ||| ")
    sentence = ' '+ parts[0] +' '
    logic = parts[1]

    # First handle special cases
    if 'colorado' in sentence and 'riverid' in logic:
        sentence = "r0".join(sentence.split("colorado"))
        logic = "r0".join(logic.split("colorado"))
    if 'mississippi' in sentence and 'riverid' in logic:
        sentence = "r0".join(sentence.split("mississippi"))
        logic = "r0".join(logic.split("mississippi"))
    if 'washington dc' in sentence:
        sentence = "c0".join(sentence.split("washington dc"))
        logic = "c0".join(logic.split("washington , dc"))
    if "stateid ( ' newyork ' " in logic or "stateid ( ' new york ' " in logic:
        sentence = "s0".join(sentence.split("new york"))
        logic = "s0".join(logic.split("newyork"))
        logic = "s0".join(logic.split("new york"))
    if "cityid ( ' newyork ' " in logic or "cityid ( ' new york ' " in logic:
        sentence = "c0".join(sentence.split("new york"))
        logic = "c0".join(logic.split("newyork"))
        logic = "c0".join(logic.split("new york"))

    # Handle usa
    if ' usa ' in logic:
        logic = " co0 ".join(logic.split(" usa "))
        for word in country:
            word = " "+ word +" "
            sentence = " cc0 ".join(sentence.split(word))

    for symbol, word_set in word_sets:
        for word in word_set:
            no_space_word = ' '+ ''.join(word.split()) +' '
            word = ' '+ word +' '
            variable = ' '+ symbol +'0 '
            while variable in sentence:
                variable = ' '+ symbol + str(int(variable[2:]) + 1) +" "

            if word in sentence and word in logic:
                sentence = variable.join(sentence.split(word))
                logic = variable.join(logic.split(word))
            elif word in sentence and no_space_word in logic:
                sentence = variable.join(sentence.split(word))
                logic = variable.join(logic.split(no_space_word))
###            elif word in sentence and word not in logic:
###                print("Got", word, "in sentence only:", line.strip())
###            elif word not in sentence and word in logic:
###                print("Got", word, "in logic only:", line.strip())

    print(sentence.strip(), "|||", ''.join(logic).strip())
