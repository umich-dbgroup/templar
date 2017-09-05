"""
Given json files with hardcoded annotations, produce variables
and replace the named entites with them.
Reads all json files in the current directory.
Requires the json files to be annotated. After running, the annotations
will be cleared. Only deals with department and number. 
"""

import sys
import os
import re
import json

def remove_annotation(instr, sql = False):
    if sql:
        s = re.sub('(<\|)|(\|>)', '', instr)
        return re.sub('(<\[)|(\]>)|(<\+)|(\+>)', ' ', s)
    return re.sub('(<\|)|(\|>)|(<\[)|(\]>)|(<\+)|(\+>)', '', instr)

def main():
    gen = (filename for filename in os.listdir(os.getcwd()) if filename[-4:] == "json")
    for filename in gen: 
        f = open(filename)
        fileStr = f.read()
        parsed_json = json.loads(fileStr)
        sentence = parsed_json["sentence"]
        sql = ''
        if len(parsed_json["sql"]) > 0:
            sql = parsed_json["sql"][0]

        
        numbers_sent = re.findall('<\|\d*\|>', sentence)
        numbers_sent = [dept.replace("<|", "").replace("|>", "") for dept in numbers_sent]
        numbers_sql = re.findall('<\|\d*\|>', sql)
        numbers_sql = [dept.replace("<|", "").replace("|>", "") for dept in numbers_sql]
        numbers_sent = list(set(numbers_sent))
        numbers_sql = list(set(numbers_sql))
        depts_sent = re.findall('<\[[A-Z]*\]>', sentence)
        depts_sent = [dept.replace("<[", "").replace("]>", "") for dept in depts_sent]
        depts_sql = re.findall('<\[[A-Z]*\]>', sql)
        depts_sql = [dept.replace("<[", "").replace("]>", "") for dept in depts_sql]
        depts_sent = list(set(depts_sent))
        depts_sql = list(set(depts_sql))

        parsed_json["sentence"] = remove_annotation(parsed_json["sentence"])
        if len(parsed_json["sql"]) > 0:
            parsed_json["sql"][0] = remove_annotation(parsed_json["sql"][0], True)
        
        parsed_json["sentence-with-vars"] = parsed_json["sentence"]
        parsed_json["sql-with-vars"] = ""
        if len(parsed_json["sql"]) > 0:
            parsed_json["sql-with-vars"] = parsed_json["sql"][0]

        count = 0
        for num in numbers_sent:
            var = {}
            var["name"] = "number" + str(count)
            var["sentence-value"] = num
            var["sql-value"] = ""
            if num in numbers_sql:
                var["sql-value"] = num
            parsed_json["sentence-with-vars"] = re.sub(num, "number" + str(count), parsed_json["sentence-with-vars"])
            parsed_json["sql-with-vars"] = re.sub(num, "number" + str(count), parsed_json["sql-with-vars"])
            parsed_json["variables"].append(var)
            count += 1

        count = 0
        for dept in depts_sent:
            var = {}
            var["name"] = "department" + str(count)
            var["sentence-value"] = dept
            var["sql-value"] = ""
            if dept in depts_sql:
                var["sql-value"] = dept
                depts_sql.remove(dept)
            parsed_json["sentence-with-vars"] = re.sub(dept, "department" + str(count), parsed_json["sentence-with-vars"])
            parsed_json["sql-with-vars"] = re.sub(dept, "department" + str(count), parsed_json["sql-with-vars"])
            parsed_json["variables"].append(var)
            count += 1
        
        if len(depts_sql) > 0:
            for dept in depts_sql:
                var = {}
                var["name"] = "department" + str(count)
                var["sentence-value"] = ""
                var["sql-value"] = "EECS"
                parsed_json["sql-with-vars"] = re.sub(dept, "department" + str(count), parsed_json["sql-with-vars"])
                parsed_json["variables"].append(var)
                count += 1

        f.close()

        # Output
        json_new = json.dumps(parsed_json, sort_keys=True, 
                              indent=4, separators=(',', ': '))
        f = open(filename, 'w')
        f.write(json_new)
        f.close()
        

if __name__ == "__main__":
    sys.exit(int(main() or 0))