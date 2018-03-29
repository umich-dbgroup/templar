"""
Cleans several common problems the the SQL queries that make tokenizing/training difficult. Also prints all table.field combinations found in the queries, for use in ensuring a complete schema.
"""

import argparse
import json
import os
import re
import sys

def clean_all(input_path, output_path):
    schema = set()
    # Find all the jsons
    jsons = [f for f in os.listdir(input_path) if f.endswith(".json")]

    for fname in jsons:
        # Clean it.
        corrected = clean_one(os.path.join(input_path,fname))
        
        # Add fields to schema
        fields = get_fields(corrected)
        if fields:
            for field in fields:
                schema.add(field)

        # Write it.
        output_fname = os.path.join(output_path, fname)
        with open(output_fname, 'w') as f:
            json.dump(corrected, f, indent=1, sort_keys=True)
    schema = list(schema)
    schema.sort()
    for s in schema:
        print s

def get_fields(dictionary):
    try:
        return re.findall("\S+\.\S+", dictionary["sql"])
    except:
        return None
        
def clean_one(fname):
    with open(fname, 'r') as f:
        dictionary = json.load(f)

    # Don't change the file if we can't find the sql query.
    if not "sql" in dictionary:
        print "sql not found in %s" %fname
        return dictionary

    query = dictionary["sql"].strip()

    # End all queries with " ;"
    if not query.endswith(";"):
        query += ";"
    query = re.sub(";", ' ;', query)
    
    # Add spaces around punctuation
    query = re.sub("([<>=\(\)'\",/])", r' \1 ', query)

    # Now clean up >= and <= 
    query = query.replace(">  =", ">=")
    query = query.replace("<  =", "<=")
    
    # Remove extra spaces
    query = ' '.join(query.split())

    dictionary["sql"] = query
    return dictionary
    

if __name__=="__main__":
    parser = argparse.ArgumentParser(description='Clean SQL queries')
    parser.add_argument('path', help='Path to the directory of jsons to clean')
    parser.add_argument('--overwrite',action='store_true', help='Use this flag to overwrite the jsons the system reads in; otherwise, a new "cleaned" directory containing the updated files will be added as a subdirectory of path.')

    args = parser.parse_args()

    # Confirm input path
    if not os.path.isdir(args.path):
        print "%s is not a valid path." % args.path
        sys.exit(0)

    # Set output path
    output_path = os.path.join(args.path, "cleaned")
    if args.overwrite:
        output_path = args.path
    if not os.path.isdir(output_path):
        os.makedirs(output_path)
    
    clean_all(args.path, output_path)
