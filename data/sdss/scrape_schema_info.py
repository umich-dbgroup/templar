import json

import requests
from bs4 import BeautifulSoup
from bs4.element import NavigableString

# configurables
DR = "dr7" # data release to look at (1-5 had errors, so try 6/7 for testing)

def read_table_info(relations, view_map, table_url, view=False):
    relation = {}

    print "Loading " + table_url + "..."
    table_r = requests.get(table_url)
    print "Parsing " + table_url + "..."
    table_soup = BeautifulSoup(table_r.text, 'html.parser')

    # get relation name
    relation['name'] = table_soup.h1.contents[1].strip()
    if view:
        relation['type'] = 'view'
        relation['view_of'] = table_soup.h2.contents[1].strip()
        if relation['view_of'] not in view_map:
            view_map[relation['view_of']] = [relation]
        else:
            view_map[relation['view_of']].append(relation)
    else:
        relation['type'] = 'relation'

    # get attributes
    relation['attributes'] = {}
    for tr in table_soup.find_all('tr'):
        if tr.contents[0]['class'][0] == 'v':
            attr_name = tr.contents[0].contents[0]
            attr_type = tr.contents[1].contents[0]
            relation['attributes'][attr_name] = {
                'name': attr_name,
                'type': attr_type
            }

    relations[relation['name']] = relation

def read_function_info(relations, edges, pks, function_url):
    relation = {}

    print "Loading " + function_url + "..."
    function_r = requests.get(function_url)
    print "Parsing " + function_url + "..."
    function_soup = BeautifulSoup(function_r.text, 'html.parser')

    # get function name
    relation['name'] = function_soup.h1.contents[1].strip()
    relation['type'] = 'function'

    # get attributes
    relation['attributes'] = {}
    relation['inputs'] = {}
    for tr in function_soup.find_all('tr'):
        # for output attributes
        if tr.contents[0]['class'][0] == 'o':
            attr_name = tr.contents[0].contents[0]
            attr_type = tr.contents[1].contents[0]
            attr_index = tr.contents[4].contents[0]
            attribute = {
                'name': attr_name,
                'type': attr_type,
                'index': attr_index
            }
            relation['attributes'][attr_name] = attribute

            # add primary key relationship if any
            if attr_name in pks:
                for rel in pks[attr_name]:
                    add_foreign_key(edges, relation['name'], rel, attr_name)

        # for input attributes
        if tr.contents[0]['class'][0] == 'v':
            attr_name = tr.contents[0].contents[0]
            attr_type = tr.contents[1].contents[0]
            attr_index = tr.contents[4].contents[0]
            attribute = {
                'name': attr_name,
                'type': attr_type,
                'index': attr_index
            }
            relation['inputs'][attr_name] = attribute

    relations[relation['name']] = relation

def add_foreign_key(edges, relation, primary_table_name, attr_name):
    edges.append({
        'primaryRelation': primary_table_name,
        'primaryAttribute': attr_name,
        'foreignRelation': relation,
        'foreignAttribute': attr_name
    })

def set_view_foreign_keys(view_map, edges, relation, primary_table_name, attr_name):
    if relation in view_map:
        for view in view_map[relation]:
            if attr_name in view['attributes']:
                add_foreign_key(edges, relation, primary_table_name, attr_name)
                set_view_foreign_keys(view_map, edges, view['name'], primary_table_name, attr_name)

def main():
    # final result
    relations = {}    # relations in schema
    edges = []        # edges (FKPK joins) in schema

    # temp
    view_map = {}       # store view relationships (relation -> views)
    pks = {}            # store primary keys (names of PKs -> tables)

    main_page_prefix = "http://skyserver.sdss.org/" + DR + "/en/help/browser/"
    shortdescr_prefix = main_page_prefix + "shortdescr.asp?n="

    # load schema table page
    short_table_url = shortdescr_prefix + "Tables&t=U"
    print "Loading " + short_table_url + "..."
    r = requests.get(short_table_url)
    print "Parsing " + short_table_url + "..."
    soup = BeautifulSoup(r.text, 'html.parser')

    for td in soup.find_all('td', class_='v'):
        if td.contents is not None:
            if td.contents[0].name == 'a':
                table_url = main_page_prefix + td.contents[0]['href']
                read_table_info(relations, view_map, table_url)

    print str(len(relations)) + " tables found."

    # load schema view page
    view_url = shortdescr_prefix + "Views&t=V"
    print "Loading " + view_url + "..."
    r = requests.get(view_url)
    print "Parsing " + view_url + "..."
    soup = BeautifulSoup(r.text, 'html.parser')

    for td in soup.find_all('td', class_='v'):
        if td.contents is not None:
            if td.contents[0].name == 'a':
                view_url = main_page_prefix + td.contents[0]['href']
                read_table_info(relations, view_map, view_url, view=True)

    print str(len(relations)) + " tables and views found."

    # load schema indices page
    indices_url = shortdescr_prefix + "Indices&t=I"
    print "Loading " + indices_url + "..."
    r = requests.get(indices_url)
    print "Parsing " + indices_url + "..."
    soup = BeautifulSoup(r.text, 'html.parser')

    for tr in soup.find_all('tr'):
        if tr.contents[0]['class'][0] == 'v':
            foreign_table_name = None
            index_type = None
            primary_table_name = None
            attr_name = None

            i = 0
            for child in tr.contents:
                if type(child) is not NavigableString and 'v' in child['class']:
                    if i == 0:
                        foreign_table_name = child.contents[0]
                    elif i == 1:
                        index_type = child.contents[0]
                    elif i == 2:
                        if index_type == 'foreign key':
                            primary_table_name = child.contents[0].split('(')[0]
                            attr_name = child.contents[0].split('(')[1].replace(')', '')
                        elif index_type == 'primary key':
                            for pk in child.contents[0].split(','):
                                pk = pk.strip()
                                if pk not in pks:
                                    pks[pk] = [foreign_table_name]
                                else:
                                    pks[pk].append(foreign_table_name)
                    i += 1

            if attr_name is not None:
                add_foreign_key(edges, foreign_table_name, primary_table_name, attr_name)
                set_view_foreign_keys(view_map, edges, foreign_table_name, primary_table_name, attr_name)

    print str(len(edges)) + " FK-PK relationships found."

    # load schema function page
    function_short_url = shortdescr_prefix + "Functions&t=F"
    print "Loading " + function_short_url + "..."
    r = requests.get(function_short_url)
    print "Parsing " + function_short_url + "..."
    soup = BeautifulSoup(r.text, 'html.parser')

    for td in soup.find_all('td', class_='v'):
        if td.contents is not None:
            if td.contents[0].name == 'a':
                function_url = main_page_prefix + td.contents[0]['href']
                read_function_info(relations, edges, pks, function_url)

    print 'After adding functions:'
    print str(len(relations)) + " tables/views/functions found."
    print str(len(edges)) + " FK-PK relationships found."

    # dump json for output
    relations_f = 'schema/best' + DR + '.relations.json'
    print 'Writing relations to ' + relations_f + '...'
    with open(relations_f, 'wb') as f:
        json.dump(relations, f)

    edges_f = 'schema/best' + DR + '.edges.json'
    print 'Writing edges to ' + edges_f + '...'
    with open('schema/best' + DR + '.edges.json', 'wb') as f:
        json.dump(edges, f)

if __name__=="__main__":
    main()
