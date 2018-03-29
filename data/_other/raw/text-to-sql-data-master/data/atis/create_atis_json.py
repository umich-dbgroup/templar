import os
import sys
import json

# find morana1/corpora/LDC/ATIS3/ | egrep '[.]sq[2l]' > file.list

def build_jsons(files, cnt):
    fname_list = []
    sites = {'BBN':0, 'CMU':0, "MIT":0, "SRI":0, "NIST":0}
    for f in files:
        path_sql = f.strip()
        if 'sq2' in path_sql:
            continue
        path_instance = path_sql[:-4]    
        #path_question = path_instance + '.win' 
        path_question = path_instance + '.sro' 
        path_category = path_instance + '.cat'
        path_sq2 = path_instance + '.sq2'
        path_ref = path_instance + ".ref"
        path_rf2 = path_instance + ".rf2"

        if not os.path.exists(path_category):
            print 'ERROR: No category for',
            print path_instance
            continue

        category = open(path_category).read().strip()
        if not category.startswith('A:'):
            #print 'skipping %s due to wrong category, %s' % (path_instance, category)
            continue

        if not os.path.exists(path_question):
            path_question = path_instance + '.lsn.'
            if not os.path.exists(path_question):
                print 'ERROR: No question for',
                print path_instance
                continue

        sqls = open(path_sql).read().strip().split('\n')
        questions = open(path_question).readlines()

        sq2s = [''] * len(sqls)
        if os.path.exists(path_sq2):
            sq2s = open(path_sq2).read()
            sq2s = sq2s.strip().split('\n\n')

        # if not len(sqls) == len(sq2s) == len(questions):
            # print 'ERROR: Unequal numbers of sql/sql2/questions',
            # print path_instance
            # print len(sqls), len(sq2s), len(questions)
            # continue

#        for sql,sq2,question in zip(sqls,sq2s,questions):
        sql=sqls[0]
        sq2 = sq2s[0]
        question = questions[0]
        #json object
        j = {}
        j['sentence'] = question.strip()
        j['sql'] = sql.strip() 
        j['sq2'] = sq2.strip()
        j['variables'] = []
        j['paraphrases'] = []
        fname = 'question%04d.json' % cnt
        with open(fname,'w') as outfile:
            json.dump(j,outfile,sort_keys=True, indent=2)
        fname_list.append(fname)
        cnt += 1
    return fname_list, cnt

train_files = open('train_file.list')
dev_files = open('dev_file.list')
# test_files = open('test_file.list')
test_files = open('test.list')

train_files = train_files.readlines()
dev_files = dev_files.readlines()
test_files = test_files.readlines()

cnt = 0
train_indices, cnt = build_jsons(train_files, cnt)
dev_indices, dev_cnt = build_jsons(dev_files, cnt)
test_indices, test_cnt = build_jsons(test_files, dev_cnt)

with open("train-dev-test.txt", 'w') as f:
    for index in train_indices:
        f.write(index.strip() + "\n")
    f.write("\n")
    for index in dev_indices:
        f.write(index.strip() + "\n")
    f.write("\n")
    for index in test_indices:
        f.write(index.strip() + "\n")
        #sys.exit()
print "%d train, %d dev, and %d test" % (cnt, dev_cnt-cnt, test_cnt - dev_cnt)
