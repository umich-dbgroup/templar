import io
import json

encoding = 'utf-8'

nlq_f = io.open('train.txt', 'w', encoding=encoding)
ans_f = io.open('train.ans', 'w', encoding=encoding)

with io.open('../raw/nlidb-datasets-master/SENLIDB/train.json', 'r', encoding=encoding) as f:
    data = json.load(f)
    for q in data:
        if q['description']:
            nlq_f.write(q['description'] + ' ')
        nlq_f.write(q['title'])
        for c in q['comments']:
            nlq_f.write(' ' + c)
        nlq_f.write(u'\n')
        ans_f.write(q['sql'] + '\n')

nlq_f.close()
ans_f.close()
