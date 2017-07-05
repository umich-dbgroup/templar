# TB-NaLIR

## Build

We use the gradle shadowJar plugin to build the project.
```
gradle shadowJar
```

## Execute

* `LogTemplateGenerator` - Generates templates given a specified query log and template generation level
* `SchemaTemplateGenerator` - Generates templates with a specified schema and join depth level
* `SchemaAndLogTemplateGenerator` - Generates templates with both a schema and a log,
with a specific % of the log used for generating templates and the remainder of the log used for testing

Execute any of the above, by executing the below instruction **after building**:
```
java -cp build/libs/tb-nalir-all.jar edu.umich.tbnalir.<class-name-here> <...insert-args-here...>
```
