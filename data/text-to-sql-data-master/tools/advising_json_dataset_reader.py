"""
The advising dataset has paraphrases, non-sql json files, and other
trickiness that makes it difficult to handle with the standard json_dataset_reader.py. This script handles it.
Eventually, it would be nice to merge the two scripts into something robust enough to handle both the simpler json format and this one.
But not today.
"""

import argparse
import itertools
import json
import os

def filename_from_number(number):
    fname = "question" + number + ".0.json"
    path = os.path.join("../data/advising", fname)
    if not os.path.isfile(path):
        fname = "question" + number + ".1.json"
        path = os.path.join("../data/advising", fname)
    return path


def get_question_query_pairs_from_file(filenumber):
    all_questions = []
    all_queries = []
    path = filename_from_number(filenumber)
    if not os.path.isfile(path):
        return all_questions, all_queries

    with open(path, 'r') as f:
        json_dict = json.load(f)
    if not "sql" in json_dict:
        return all_questions, all_queries
    query = json_dict["sql"][0]
    if "sentence" in json_dict:
        orig = json_dict["sentence"]
        all_questions.append(orig)
    if "paraphrases" in json_dict:
        paraphrases = json_dict["paraphrases"]
        all_questions.extend(paraphrases)
    all_queries = list(itertools.repeat(query, len(all_questions)))
    return all_questions, all_queries

def get_all_question_query_pairs(json_dict, dataset):
    """dataset should be 'train', 'test', or 'dev'"""
    all_encode, all_decode = [], []
    nums = json_dict[dataset]
    for num in nums:
        encode, decode = get_question_query_pairs_from_file(num)
        all_encode.extend(encode)
        all_decode.extend(decode)
    return all_encode, all_decode

def get_sentences_and_sql(use_question_split=False):
    if use_question_split: 
        fname = "../data/advising/split_by_question.json"
        with open(fname, 'r') as f:
            json_dict = json.load(f)
        # Get all the test data
        test_encode, test_decode = get_pairs_by_question(json_dict, "test")
        # Get all the dev data
        dev_encode, dev_decode = get_pairs_by_question(json_dict, "dev")
        # Get all the train data
        train_encode, train_decode = get_pairs_by_question(json_dict, "train")
    else: # use query split. 
        fname = "../data/advising/split_by_query.json"
        with open(fname, 'r') as f:
            json_dict = json.load(f)
        # Get all the test data
        test_encode, test_decode = get_all_question_query_pairs(json_dict, "test")
        # Get all the dev data
        dev_encode, dev_decode = get_all_question_query_pairs(json_dict, "dev")
        # Get all the train data
        train_encode, train_decode = get_all_question_query_pairs(json_dict, "train")

    return train_encode, train_decode, dev_encode, dev_decode, test_encode, test_decode

def get_pairs_by_question(json_dict, dataset):
    query_dict = {}
    question_defs = json_dict[dataset]
    encode, decode = [], []
    for question_def in question_defs:
        num = question_def[0]
        question = question_def[1]
        if num in query_dict:
            query = query_dict[num]
        else:
            path = filename_from_number(num)
            if not os.path.isfile(path):
                raise ValueError("%s is not a valid file number" % num)
            with open(path, 'r') as f:
                file_json = json.load(f)
            query = file_json["sql"][0]
            query_dict[num] = query
        encode.append(question)
        decode.append(query)
    return encode, decode


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Convert JSON-formatted NL-SQL datasets to format expected by data_utils')
    parser.add_argument('output_path', help='Directory to save _encode.txt and _decode.txt files.')
    parser.add_argument("-q", '--question', dest='question_split', 
                        action='store_true', 
                        help="Use the question split. If not set, use query split.")

    args = parser.parse_args()

    # Verify that the output path exists.
    if not os.path.isdir(args.output_path):
        os.makedirs(args.output_path)
    output_path = args.output_path
    # Create subfolders in the output path if needed.
    train_path = os.path.join(output_path, "train/") 
    dev_path = os.path.join(output_path, "dev/")
    test_path = os.path.join(output_path, "test/")
    for p in [train_path, dev_path, test_path]:
        if not os.path.isdir(p):
            os.makedirs(p)

    train_encode, train_decode, dev_encode, dev_decode, test_encode, test_decode = get_sentences_and_sql(args.question_split)

    # Save all files. 
    with open(os.path.join(train_path, "train_encode.txt"), 'w') as f:
        for line in train_encode:
            f.write(line + "\n")
    with open(os.path.join(train_path, "train_decode.txt"), 'w') as f:
        for line in train_decode:
            f.write(line + "\n")
    with open(os.path.join(dev_path, "dev_encode.txt"), 'w') as f:
        for line in dev_encode:
            f.write(line + "\n")
    with open(os.path.join(dev_path, "dev_decode.txt"), 'w') as f:
        for line in dev_decode:
            f.write(line + "\n")
    with open(os.path.join(test_path, "test_encode.txt"), 'w') as f:
        for line in test_encode:
            f.write(line + "\n")
    with open(os.path.join(test_path, "test_decode.txt"), 'w') as f:
        for line in test_decode:
            f.write(line + "\n")
