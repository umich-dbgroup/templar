import json
import os
import random

def get_query_numbers(fnames):
    numbers = []
    for fname in fnames:
        #fname format is "question060.1.json"
        if not "question" in fname or not ".json" in fname:
            continue
        num = fname[8:11]
        numbers.append(num)
    return numbers
def generate_query_split(fnames, dirname):
    query_numbers = get_query_numbers(fnames)
    split_list(query_numbers, 70, 30, dirname, "split_by_query.json")

def split_list(query_list, test_count, dev_count, dirname, fname):
    random.shuffle(query_list)
    test = query_list[:test_count]
    dev_end = test_count+dev_count
    dev = query_list[test_count:dev_end]
    train = query_list[dev_end:]
    output = {"test":test, "dev":dev, "train":train}
    with open(os.path.join(dirname, fname), "w") as f:
        json.dump(output, f, indent=2)

def get_all_questions(fnames):
    all_questions = []
    for fname in fnames:
        if not "question" in fname or not ".json" in fname:
            continue
        num = fname[8:11]
        with open(os.path.join(dirname, fname), 'r') as f:
            json_dict = json.load(f)
        if "sentence" in json_dict:
            orig = json_dict["sentence"]
            all_questions.append([num, orig])
        if "paraphrases" in json_dict:
            paraphrases = json_dict["paraphrases"]
            for p in paraphrases:
                all_questions.append([num, p])
    return all_questions

def generate_question_split(fnames, dirname):
    all_questions = get_all_questions(fnames)
    split_list(all_questions, 500, 200, dirname, "split_by_question.json")

if __name__ == "__main__":
    dirname = "../data/advising"
    fnames = [fname for fname in os.listdir(dirname) if os.path.isfile(os.path.join(dirname, fname)) and "no-sql" not in fname and ".json" in fname]
    generate_query_split(fnames, dirname)
    generate_question_split(fnames, dirname)
