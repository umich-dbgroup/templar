import argparse
import os
import random

def split_dataset_variable(dataset_var):
    l = dataset_var.split("-")
    dataset = l[0]
    language = l[1]
    var_bool = l[2] + "-" + l[3]
    split_type = l[4] + "-split"
    return dataset, language, var_bool, split_type

def format_data(dataset, language, var_bool, split_type):
    fname_end = ".".join([language, var_bool, split_type, "txt"])
    train_fname = dataset + ".train." + fname_end
    if not os.path.isfile(train_fname): return
    test_fname = dataset + ".test." + fname_end
    if not os.path.isfile(test_fname): return
    dev_fname = dataset + ".dev." + fname_end
    has_dev = os.path.isfile(dev_fname)

    # make the appropriate directory
    directory = os.path.join("formatted", dataset, language, var_bool, split_type)
    if not os.path.exists(directory):
        os.makedirs(directory)
    
    # get test data
    test_encode, test_decode = get_encode_and_decode(test_fname)
    save(test_encode, test_decode, directory, "test")

    # handle dev and train
    train_encode, train_decode = get_encode_and_decode(train_fname)
    if has_dev:
        dev_encode, dev_decode = get_encode_and_decode(dev_fname)
    else:
        train_encode, train_decode, dev_encode, dev_decode = sample_dev(train_encode, train_decode)
    save(train_encode, train_decode, directory, "train")
    save(dev_encode, dev_decode, directory, "dev")
    suggest_vocab_size(directory, train_encode, train_decode)

def suggest_vocab_size(directory, encode, decode):
    encode = " ".join(encode)
    decode = " ".join(decode)
    encode_tokens = set(encode.split(" "))
    decode_tokens = set(decode.split(" "))
    print "%s has encode vocab size ~%d and decode ~%d" %(directory, 
                                                          len(encode_tokens),
                                                          len(decode_tokens))

def sample_dev(encode, decode):
    # Picks 100 random pairs from train to be our dev set
    train_encode, train_decode = [], []
    dev_encode, dev_decode = [], []
    indices = range(len(encode))
    random.shuffle(indices)
    for i in indices[:100]:
        dev_encode.append(encode[i])
        dev_decode.append(decode[i])
    for j in indices[100:]:
        train_encode.append(encode[j])
        train_decode.append(decode[j])
    return train_encode, train_decode, dev_encode, dev_decode

def save(encode, decode, directory, split):
    # split is train, dev, or test
    directory = os.path.join(directory, split)
    if not os.path.exists(directory):
        os.makedirs(directory)
    with open(os.path.join(directory, split + "_encode.txt"), 'w') as f:
        for line in encode:
            f.write(line + "\n")
    with open(os.path.join(directory, split + "_decode.txt"), 'w') as f:
        for line in decode:
            f.write(line + "\n")

def get_encode_and_decode(fname):
    with open(fname, 'r') as f:
        lines = f.readlines()
    encode,decode = [],[]
    for l in lines:
        l = l.split("|||")
        if not len(l) == 2:
            if (l[0].strip()):
                print "messed up line: %s" % str(l)
            continue
        encode.append(l[0].strip())
        decode.append(l[1].strip())
    return encode, decode

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Make these files ready for data_utils.')
    parser.add_argument('-d', "--dataset", 
                        help="""Dataset to run on. 
                        Enter as <dataset>-<language>-with|no-vars-question|query.
                        E.g., "atis-logic-with-vars-question" 
                        for atis logic with variables question-split.
                        If not specified, runs on all datasets in the folder.""")

    args = parser.parse_args()
    if args.dataset:
        dataset, language, var_bool, split_type = split_dataset_variable(args.dataset)
        format_data( dataset, language, var_bool, split_type)
    else:
        for dataset in ['atis', 'geo']:
            for language in ['logic', 'prolog', 'sql']:
                for var_bool in ['with', 'no']:
                    var_bool += "-vars"
                    for split_type in ["question", "query"]:
                        split_type += "-split"
                        format_data(dataset, language, var_bool, split_type)
