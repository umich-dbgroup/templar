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
    relation['name'] = table_soup.h1.contents[1].strip().lower()
    if view:
        relation['type'] = 'view'
        relation['view_of'] = table_soup.h2.contents[1].strip().lower()
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
            attr_name = tr.contents[0].contents[0].lower()
            attr_type = tr.contents[1].contents[0].lower()
            relation['attributes'][attr_name] = {
                'name': attr_name,
                'type': attr_type
            }

    relations[relation['name']] = relation

def read_function_info(relations, edges, edges_dupl, pks, function_url):
    relation = {}

    print "Loading " + function_url + "..."
    function_r = requests.get(function_url)
    print "Parsing " + function_url + "..."
    function_soup = BeautifulSoup(function_r.text, 'html.parser')

    # get function name
    relation['name'] = function_soup.h1.contents[1].strip().lower()
    relation['type'] = 'function'

    # get attributes
    relation['attributes'] = {}
    relation['inputs'] = {}
    for tr in function_soup.find_all('tr'):
        # for output attributes
        if tr.contents[0]['class'][0] == 'o':
            attr_name = tr.contents[0].contents[0].lower()
            attr_type = tr.contents[1].contents[0].lower()
            attr_index = tr.contents[4].contents[0]
            attribute = {
                'name': attr_name,
                'type': attr_type,
                'index': attr_index
            }
            relation['attributes'][attr_name] = attribute

            # add fk-pk relationships if any to relations and views
            if attr_name in pks:
                for rel in pks[attr_name]:
                    add_foreign_key(edges, edges_dupl, relation['name'], rel, attr_name)

        # for input attributes
        if tr.contents[0]['class'][0] == 'v':
            attr_name = tr.contents[0].contents[0].lower()
            attr_type = tr.contents[1].contents[0].lower()
            attr_index = tr.contents[4].contents[0]
            attribute = {
                'name': attr_name,
                'type': attr_type,
                'index': attr_index
            }
            relation['inputs'][attr_name] = attribute

    relations[relation['name']] = relation

def add_foreign_key(edges, edges_dupl, relation, primary_table_name, attr_name):
    edges_hash = hash(primary_table_name.lower() + relation.lower() + attr_name.lower())

    if edges_hash not in edges_dupl:
        edges_dupl.add(edges_hash)
        edges.append({
            'primaryRelation': primary_table_name.lower(),
            'primaryAttribute': attr_name.lower(),
            'foreignRelation': relation.lower(),
            'foreignAttribute': attr_name.lower()
        })

def set_view_foreign_keys_recursive(view_map, edges, edges_dupl, relation, primary_table_name, attr_name):
    if relation in view_map:
        for view in view_map[relation]:
            if attr_name in view['attributes']:
                add_foreign_key(edges, edges_dupl, relation, primary_table_name, attr_name)
                set_view_foreign_keys_recursive(view_map, edges, edges_dupl, view['name'], primary_table_name, attr_name)

def set_view_primary_key_recursive(view_map, pks, table_name, key_name):
    # check if any views with the same pk
    if table_name in view_map:
        for view in view_map[table_name]:
            if key_name in view['attributes']:
                pks[key_name].append(view['name'])
                set_view_primary_key_recursive(view_map, pks, view['name'], key_name)

def main():
    # final result
    relations = {}    # relations in schema
    edges = []        # edges (FKPK joins) in schema

    # temp
    view_map = {}       # store view relationships (relation -> views)
    pks = {}            # store primary keys (names of PKs -> tables)
    edges_dupl = set()  # stores hashed foreign key edges, prevent duplicates

    main_page_prefix = "http://skyserver.sdss.org/" + DR + "/en/help/browser/"

    # load schema table page
    table_info_url = main_page_prefix + "dbinfo.asp?i=1"
    print "Loading " + table_info_url + "..."
    r = requests.get(table_info_url)
    print "Parsing " + table_info_url + "..."
    soup = BeautifulSoup(r.text, 'html.parser')

    for a in soup.find_all('a', class_='ddrop'):
        if 't=U' in a['href']:
            table_url = main_page_prefix + a['href']
            read_table_info(relations, view_map, table_url)

    print str(len(relations)) + " tables found.\n"

    # load schema view page
    view_info_url = main_page_prefix + "dbinfo.asp?i=2"
    print "Loading " + view_info_url + "..."
    r = requests.get(view_info_url)
    print "Parsing " + view_info_url + "..."
    soup = BeautifulSoup(r.text, 'html.parser')

    for a in soup.find_all('a', class_='ddrop'):
        if 't=V' in a['href']:
            view_url = main_page_prefix + a['href']
            read_table_info(relations, view_map, view_url, view=True)

    print str(len(relations)) + " tables and views found.\n"

    # load schema indices page
    print "Adding explicit FK-PK relationships..."
    indices_url = main_page_prefix + "shortdescr.asp?n=Indices&t=I"
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
                        foreign_table_name = child.contents[0].lower()
                    elif i == 1:
                        index_type = child.contents[0].lower()
                    elif i == 2:
                        if index_type == 'foreign key':
                            primary_table_name = child.contents[0].split('(')[0].lower()

                            attr_name = child.contents[0].split('(')[1].replace(')', '').lower()
                        elif index_type == 'primary key':
                            for pk in child.contents[0].split(','):
                                pk = pk.strip().lower()
                                if pk not in pks:
                                    pks[pk] = [foreign_table_name]
                                else:
                                    pks[pk].append(foreign_table_name)

                                # check if any views with the same pk
                                set_view_primary_key_recursive(view_map, pks, foreign_table_name, pk)
                    i += 1

            if attr_name is not None:
                add_foreign_key(edges, edges_dupl, foreign_table_name, primary_table_name, attr_name)
                set_view_foreign_keys_recursive(view_map, edges, edges_dupl, foreign_table_name, primary_table_name, attr_name)

    print str(len(edges)) + " FK-PK relationships found.\n"

    print "Adding implicit FK-PK relationships..."
    for foreign_rel_name, foreign_rel in relations.iteritems():
        for attr in foreign_rel['attributes']:
            if attr in pks:
                for primary_rel_name in pks[attr]:
                    if foreign_rel_name != primary_rel_name:
                        add_foreign_key(edges, edges_dupl, foreign_rel_name, primary_rel_name, attr)
    print str(len(edges)) + " FK-PK relationships found.\n"

    # load schema function page
    function_short_url = main_page_prefix + "shortdescr.asp?n=Functions&t=F"
    print "Loading " + function_short_url + "..."
    r = requests.get(function_short_url)
    print "Parsing " + function_short_url + "..."
    soup = BeautifulSoup(r.text, 'html.parser')

    for td in soup.find_all('td', class_='v'):
        if td.contents is not None:
            if td.contents[0].name == 'a':
                function_url = main_page_prefix + td.contents[0]['href']
                read_function_info(relations, edges, edges_dupl, pks, function_url)

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
