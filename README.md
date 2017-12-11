# Templar

## Build

We use the gradle shadowJar plugin to build the project.
```
gradle shadowJar
```

## Executable Classes

* `Templar` - Executes a single NLQ given some parameters.
* `TemplarCV` - Runs a cross-validation test on a specific dataset given some parameters.

Execute any of the above, by executing the below instruction **after building**:
```
java -cp build/libs/templar-master-all.jar edu.umich.templar.<class-name-here> <...insert-args-here...>
```

## Public API Methods

We should expose the following methods:

* `src/main/java/edu/umich/templar/Templar.java`
  * `Templar(String dbHost, int dbPort, String dbUser, String dbPassword, String datasetName, String dbName)` - initializes Templar (run once only)
  * `loadSQLLog(List<String> sqlLog)` - takes in a list of SQL strings to load into running instance of Templar
  * `translate(String nlq)` - takes in a natural language query string and returns a List<String> of SQL translations

### Dependencies

* `word2vec_server.py` needs to be running for this to work properly, and it will be hit on port 10000.
* a functioning MySQL instance with necessary data from each dataset pre-loaded. For example, have MySQL up and running, then create database name `mas` and follow instructions on the [MAS dataset](data/mas) README.
