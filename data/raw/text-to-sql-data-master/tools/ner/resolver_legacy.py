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

def build_train_data(train_data, path):
    os.chdir(path)
    gen = (filename for filename in os.listdir(os.getcwd()) if filename[-5:] == ".json")
    count_trained = 0
    print ("Building training set...")
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
    print ("Training set built upon", count_trained, "files.")

def test(nlp, ner, path):
    os.chdir(path)
    gen = (filename for filename in os.listdir(os.getcwd()) if filename[-5:] == ".json")
    total = {"count_tested" : 0, "true_positive" : 0, "retrieved" : 0, "relevant" : 0}
    number = {"count_tested" : 0, "true_positive" : 0, "retrieved" : 0, "relevant" : 0}
    dept = {"count_tested" : 0, "true_positive" : 0, "retrieved" : 0, "relevant" : 0}
    instr = {"count_tested" : 0, "true_positive" : 0, "retrieved" : 0, "relevant" : 0}
    
    print ("Begin testing...")
    for filename in gen:
        with open(filename) as f:
            parsed_json = json.loads(f.read())
            sentence = parsed_json["sentence"]
            sentence = re.sub(r"/", " ", sentence)
            #print ("In sentence:", sentence)
            doc = nlp.make_doc(sentence)
            nlp.tagger(doc)
            ner(doc)

            produced = set()
            num_produced = set()
            dept_produced = set()
            instr_produced = set()
            for ent in doc.ents:
                produced.add((ent.label_, ent.text))
                total["retrieved"] += 1
                if ent.label_ == "number":
                    num_produced.add((ent.label_, ent.text))
                    number["retrieved"] += 1
                if ent.label_ == "department":
                    dept_produced.add((ent.label_, ent.text))
                    dept["retrieved"] += 1
                if ent.label_ == "instructor":
                    instr_produced.add((ent.label_, ent.text))
                    instr["retrieved"] += 1

            correct = set()
            num_correct = set()
            dept_correct = set()
            instr_correct = set()
            vars = parsed_json["variables"]
            for var in vars:
                if var["sentence-value"] != "":
                    correct.add((var['name'][0:-1], var["sentence-value"]))
                    total["relevant"] += 1
                    if var["name"][0:-1] == "number":
                        num_correct.add((var['name'][0:-1], var["sentence-value"]))
                        number["relevant"] += 1
                    if var["name"][0:-1] == "department":
                        dept_correct.add((var['name'][0:-1], var["sentence-value"]))
                        dept["relevant"] += 1
                    if var["name"][0:-1] == "instructor":
                        instr_correct.add((var['name'][0:-1], var["sentence-value"]))
                        instr["relevant"] += 1
            num_good = len(produced.intersection(correct))
            total["true_positive"] += num_good

            number["true_positive"] += len(num_produced.intersection(num_correct))
            dept["true_positive"] += len(dept_produced.intersection(dept_correct))
            instr["true_positive"] += len(instr_produced.intersection(instr_correct))

            total["count_tested"] += 1
            #print ("Correctly predicted", num_good, "out of", len(correct), "entities.")
            #print (len(produced - correct), "of the predicted entites is not correct.\n")

    print ("Tested on ", total["count_tested"], " files.")
    precision = float(total["true_positive"])/total["retrieved"]
    recall = float(total["true_positive"])/total["relevant"]
    print ("The overall precision is", total["true_positive"], "out of", total["retrieved"], ":", precision)
    print ("The overall recall is:", total["true_positive"], "out of", total["relevant"], ":", recall)
    print ("The number precision is", number["true_positive"], "out of", number["retrieved"])
    print ("The number recall is:", number["true_positive"], "out of", number["relevant"])
    print ("The department precision is", dept["true_positive"], "out of", dept["retrieved"])
    print ("The department recall is:", dept["true_positive"], "out of", dept["relevant"])
    print ("The instructor precision is", instr["true_positive"], "out of", instr["retrieved"])
    print ("The instructor recall is:", instr["true_positive"], "out of", instr["relevant"])

def main(model_dir=None):
    nlp = spacy.load('en', parser=False, entity=False, add_vectors=False)
    pre_trained = spacy.load('en')

    train_data = []
    build_train_data(train_data, sys.argv[1])

    print ("Begin training...")
    ner = train_ner(nlp, train_data, ['number', 'department', 'year', 'semester', 'topic', 'instructor'])
    print ("Training completed.")

    test(nlp, ner, sys.argv[2])

    if model_dir is not None:
        save_model(ner, model_dir)





if __name__ == '__main__':
    main('ner')
    # Who "" 2
    # is "" 2
    # Shaka "" PERSON 3
    # Khan "" PERSON 1
    # ? "" 2