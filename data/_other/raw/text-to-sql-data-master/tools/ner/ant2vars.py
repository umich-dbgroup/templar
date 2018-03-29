import sys
import os
import re
import json

def main():
    gen = (filename for filename in os.listdir(os.getcwd()) if filename[-4:] == "json")
    for filename in gen: 
        f = open(filename)
        fileStr = f.read()
        parsed_json = json.loads(fileStr)
        sentence = parsed_json["sentence"]
        #sql = parsed_json["sql"]
        #assumption that for now only cares about sentences
        numbers = re.findall('<\|\d*\|>', sentence)
        numbers = [number.replace("<|", "").replace("|>", "") for number in numbers]
        parsed_json["sentence"] = re.sub('<\|\d*\|>', 'course_numx', parsed_json["sentence"])
        count = 0
        while ('course_numx' in parsed_json["sentence"]):
            parsed_json["sentence"] = parsed_json["sentence"].replace("course_numx", "course_num" + str(count), 1)
            count += 1
        dept = re.findall('<\[[A-Z]*\]>', sentence)
        dept = [d.replace("<[", "").replace("]>", "") for d in dept]
        parsed_json["sentence"] = re.sub('<\[[A-Z]*\]>', 'course_deptx', parsed_json["sentence"])
        count = 0
        while ('course_deptx' in parsed_json["sentence"]):
            parsed_json["sentence"] = parsed_json["sentence"].replace("course_deptx", "course_dept" + str(count), 1)
            count += 1
        count = 0
        for n in numbers:
            s = "course_num" + str(count) +" = " + str(n)
            parsed_json["variables"].append(s)
            count += 1
        count = 0
        for d in dept:
            s = "course_dept" + str(count) +" = " + str(d)
            parsed_json["variables"].append(s)
            count += 1
        json_new = json.dumps(parsed_json, sort_keys=True, 
                              indent=4, separators=(',', ': '))
        print json_new
        f.close()
        print
        print
        f = open(filename, 'w')
        f.write(json_new)
        

if __name__ == "__main__":
    sys.exit(int(main() or 0))