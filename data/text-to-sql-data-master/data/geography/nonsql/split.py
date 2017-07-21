import json
import os
import numpy as np

with open("split880.train.ids", 'r') as f:
    train_ids = [int(i.strip()) for i in f.readlines()]

with open("split880.test.ids", 'r') as f:
    test_ids = [int(i.strip()) for i in f.readlines()]

with open("geoquery.en", 'r') as f:
    sentences = [line.strip() for line in f.readlines()]

i_train = 0
i_test = 0
train_sents = []
test_sents = []

for k, sent in enumerate(sentences):
    if i_train < len(train_ids) and k == train_ids[i_train]:
        train_sents.append(sent)
        i_train += 1
    elif i_test < len(test_ids) and k == test_ids[i_test]:
        test_sents.append(sent)
        i_test += 1
    else:
        print "Missing an id."
        
print "%d sentences in train and %d in test." %(len(train_sents), len(test_sents))
train_sents = set(train_sents)
test_sents = set(test_sents)
print "%d unique sentences in train and %d in test." %(len(train_sents), len(test_sents))

for tr in train_sents:
    for te in test_sents:
        if tr == te:
            print "Duplicate: %s" % tr


# Now, try to match sentences in our dataset to sentences in train or test
our_train = []
our_test = []
our_novel = [] # Things in neither train nor test of normal GeoQuery.

# For each json in the geography directory, if its sentence matches something in train_sents, put it in our train set; elif its sentence matches something in test, put it in our test set; else put it in our "novel" set. 
dir_name = "../"
for fname in os.listdir(dir_name):
    fname = os.path.join(dir_name, fname)
    if not os.path.isfile(fname): continue
    if not fname[-4:] == "json": continue
    with open(fname, 'r') as f:
        json_dict = json.load(f)
    if not "sentence" in json_dict:
        print "Missing sentence: %s" %fname
        continue
    sent = json_dict["sentence"].strip("?").lower().strip()
    if sent in train_sents:
        our_train.append(fname)
    elif sent in test_sents:
        our_test.append(fname)
    else:
        our_novel.append(fname)
        #print sent

print "%d in our_train, %d in our_test, and %d in our_novel." % (len(our_train), len(our_test), len(our_novel))

our_train = our_train + our_novel
dev_indices = np.random.choice(our_train, 100, replace=False)
train = [os.path.split(x)[1] for x in our_train if x not in dev_indices]
dev = [os.path.split(x)[1] for x in dev_indices]
test = [os.path.split(x)[1] for x in our_test]

with open(os.path.join(dir_name, "train-dev-test.txt"), 'w') as f:
    for i in train:
        f.write(i + "\n")
    f.write("\n")
    for i in dev:
        f.write(i + "\n")
    f.write("\n")
    for i in test:
        f.write(i + "\n")

