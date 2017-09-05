import json
import re

for x in range(10):
	tables_count = {}
	variables = []
	filename = "data/atis/quesion000" + str(x) + '.json'
	with open(filename) as f:
		data = json.load(f)
		sql_data = data['sql']
		sql_with_vars = sql_data
		# print(sql_data)
		matches = re.findall(r"\'(.+?)\'", sql_data)
		# print(matches)
		for word in matches:
			word_idx = sql_data.find(word)
			sql_split = sql_data[:word_idx].split(".")
			table_name = sql_split[-2].split(" ")[-1]
			# print(table_name)
			if table_name in tables_count:
				tables_count[table_name] += 1
			else:
				tables_count[table_name] = 0
			var_name = table_name + str(tables_count[table_name])
			variables.append({'name': var_name, "sql-value": word})
			sql_with_vars = sql_with_vars.replace(word, var_name)
		print("Variables: ", variables)
		print("SQL with vars:", sql_with_vars)
		data['variables'] = variables
		data['sql_with_vars'] = sql_with_vars
