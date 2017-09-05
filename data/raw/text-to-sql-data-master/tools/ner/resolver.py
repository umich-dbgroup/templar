# Named entity recognizer for NL-DB project
# Code adapted from https://github.com/explosion/spaCy/blob/master/examples/training/train_ner.py
# Written by Harry Zhang, zharry@umich.edu

from __future__ import unicode_literals, print_function
import json
import pathlib
import random
import sys
import os
import re

import spacy
from spacy.pipeline import EntityRecognizer
from spacy.gold import GoldParse
from spacy.tagger import Tagger
 
try:
    unicode
except:
    unicode = str


def train_ner(nlp, train_data, entity_types):
    # Add new words to vocab.
    for raw_text, _ in train_data:
        doc = nlp.make_doc(raw_text)
        for word in doc:
            _ = nlp.vocab[word.orth]

    # Train NER.
    ner = EntityRecognizer(nlp.vocab, entity_types=entity_types)
    for itn in range(5):
        random.shuffle(train_data)
        for raw_text, entity_offsets in train_data:
            doc = nlp.make_doc(raw_text)
            gold = GoldParse(doc, entities=entity_offsets)
            ner.update(doc, gold)
    return ner

def save_model(ner, model_dir):
    model_dir = pathlib.Path(model_dir)
    if not model_dir.exists():
        model_dir.mkdir()
    assert model_dir.is_dir()

    with (model_dir / 'config.json').open('wb') as file_:
        data = json.dumps(ner.cfg)
        if isinstance(data, unicode):
            data = data.encode('utf8')
        file_.write(data)
    ner.model.dump(str(model_dir / 'model'))
    if not (model_dir / 'vocab').exists():
        (model_dir / 'vocab').mkdir()
    ner.vocab.dump(str(model_dir / 'vocab' / 'lexemes.bin'))
    with (model_dir / 'vocab' / 'strings.json').open('w', encoding='utf8') as file_:
        ner.vocab.strings.dump(file_)

def build_train_data(train_data, gen):
    count_trained = 0
    for filename in gen:
        with open(filename) as f:
            parsed_json = json.loads(f.read())
            sentence = parsed_json["sentence"]
            vars = parsed_json["variables"]
            l = []
            for var in vars:
                length = len(var["sentence-value"])
                if length != 0:
                    occurences = [m.start() for m in re.finditer(var["sentence-value"], sentence)]
                    for occ in occurences:
                        l.append((occ, occ + length, var['name'][0:-1]))
            if len(l) != 0:
                train_data.append((sentence, l))
        count_trained += 1


def test(nlp, ner, filename, total):
    print ("Testing", filename, "...")
    with open(filename) as f:
        parsed_json = json.loads(f.read())
        sentence = parsed_json["sentence"]
        sentence = re.sub(r"/", " ", sentence)
        doc = nlp.make_doc(sentence)
        nlp.tagger(doc)
        ner(doc)

        produced = set()
        for ent in doc.ents:
            produced.add((ent.label_, ent.text))
        total["retrieved"] += len(produced)

        correct = set()
        vars = parsed_json["variables"]
        for var in vars:
            if var["sentence-value"] != "":
                correct.add((var['name'][0:-1], var["sentence-value"]))
        total["relevant"] += len(correct)

        if produced == correct:
            print ("Exactly correct!")
            total["count_good"] += 1
            pass
        else:
            #print ("Testing", filename, "...")
            if len(produced) == 0:
                print ("Nothing predicted...")
            else:
                print ("What we predicted: ")
                for ent in doc.ents:
                    print (ent.label_, ent.text)
            if len(correct) == 0:
                print ("Nothing annotated...")
            else:
                print ("What they annotated: ")
                for var in vars:
                    if var["sentence-value"] != "":
                        print (var['name'][0:-1], var["sentence-value"])
            print ()
                   
        num_good = len(produced.intersection(correct))
        total["true_positive"] += num_good
        total["count_tested"] += 1


def leave_one_out(data_path):
    nlp = spacy.load('en', parser=False, entity=False, add_vectors=False)
    #os.chdir('C:\\Users\\Hazrael\\Documents\\GitHub\\text-to-sql-data\\data\\advising')
    os.chdir(data_path)
    fileNames = [filename for filename in os.listdir(os.getcwd()) if filename[-5:] == ".json"]
    fileCount = len(fileNames)
    total = {"count_tested" : 0, "count_good" : 0, "true_positive" : 0, "retrieved" : 0, "relevant" : 0}

    for count in range(fileCount):
        # Train stage
        train_data = []
        test_file_name = fileNames[count]
        gen = (filename for filename in fileNames if filename != test_file_name)
        build_train_data(train_data, gen)

        # Test stage
        ner = train_ner(nlp, train_data, ['number', 'department', 'year', 'semester', 'topic', 'instructor'])
        test(nlp, ner, test_file_name, total)
    
    print ("Correctly predicted", total["count_good"], "out of", total["count_tested"], "entities:", float(total["count_good"])/total["count_tested"])
    print ("The micro pricision is", total["true_positive"], "out of", total["retrieved"], ":", float(total["true_positive"])/total["retrieved"])
    print ("The micro recall is", total["true_positive"], "out of", total["relevant"], ":", float(total["true_positive"])/total["relevant"])

def train_and_test(data_path, test_file):
    nlp = spacy.load('en', parser=False, entity=False, add_vectors=False)
    os.chdir(data_path)

    # Train stage
    train_data = []
    gen = (filename for filename in os.listdir(os.getcwd()) if filename[-5:] == ".json")
    build_train_data(train_data, gen)

    # Test stage
    ner = train_ner(nlp, train_data, ['number', 'department', 'year', 'semester', 'topic', 'instructor'])
    with open(test_file) as f:
        for sentence in f:
            print (sentence)
            sentence = re.sub(r"/", " ", sentence)
            doc = nlp.make_doc(unicode(sentence))
            nlp.tagger(doc)
            ner(doc)
            for ent in doc.ents:
                print (ent.label_, ent.text)
            print ()

def main(model_dir=None):
    if sys.argv[1] == "loo":
        leave_one_out(sys.argv[2])
    elif sys.argv[1] == "tnt":
        train_and_test(sys.argv[2], sys.argv[3])
    else:
        print ("Check README for usage. Aborted.")
        sys.exit(1)

    #if model_dir is not None:
    #    save_model(ner, model_dir)


if __name__ == '__main__':
    main('ner')