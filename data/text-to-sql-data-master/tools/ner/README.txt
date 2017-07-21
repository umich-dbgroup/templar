Named Entity Resolver tools for NL-DB

===resolver.py===

Description:
A named entity resolver written by Harry Zhang for research purposes in the NL-DB team. The code is based on the spaCy NER library and the demo in https://github.com/explosion/spaCy/blob/master/examples/training/train_ner.py. There are two options: leave-one-out (loo) mode and train-and-test (tnt) mode. The resolver currently handles these entities: 'number', 'department', 'year', 'semester', 'topic', 'instructor'.
Require:
spaCy
A directory of .json files that have fields "sentence" and "variables" validly formatted. Check sample.json for example.
Misc:
There is a save_model() function in the script that I have not yet figured out how to use.

---leave-one-out mode---
Usage:
python resolver.py loo data_path
e.g.
python resolver.py loo ~/Documents/data
Explanation:
When loo mode is selected, it does N-fold cross validation on the data specified by the data_path, where N is the number of .json files in that directory. 
Output:
The testing result for each file, and the contents of prediction and annotation if the prediction is not exactly correct.
The accuracy of the resolver, with only exactly correct predictions being counted as correct. The entity-level micro precision and recall.

---train-and-test---
Usage:
python resolver.py tnt data_path test_file_path
e.g.
python resolver.py tnt ~/Documents/data ./test.lst
Notice:
The test_file_path is either abosolute, or relative to the data_path. The test file should have one sentence each line.
Explanation:
When tnt mode is selected, the model is trained on all .json files in the directory specified by data_path. Then, for each line in the test file, it predicts the entities. 
Output:
Each senetence in the test file followed by the predicted entity types and contents. 

===resolver_legacy.py===

Description:
A legacy version of resolver.py that explictly uses designated data to train and test.
Usage:
python resolver_legacy.py train_data_path test_data_path
Output:
Similar to the output of resolver.py loo mode, plus the statistics breakdown of entities "number", "department" and "instructor". 

===ant2vars.py===

Description:
Parses the disgnated annations enclosing entity tokens in the "sentence" field of the .json, generates variables accordingly, and delete the annotations. It only supports recoginizing number and department. The number should be annotated as <|595|>, the department should be <[EECS]>.
Usage:
python ant2vars.py
Input:
All the .json files in the same directory as the script
Output:
Modifies the .json files to have variables and strips annotations.
Note:
This code was first run, and then I manually added variables for other entities. I believe there are cases when it fails when handling multiple occurences of the same department.