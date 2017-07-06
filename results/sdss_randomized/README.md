# SDSS Randomized

Results for generating templates from both the schema (with varying degrees of join levels) and the log (with varying log template types used and amounts of each log used).

Filename example:

```
bestdr7_0.05.join1.pred_proj.25.out
```
* `bestdr7_0.05`: name of database/schema data set
* `join1`: using 1 level of joins
* `pred_proj`: using the predicate/projection template generation from query logs
* `25`: use 25% of the query log for template generation, and the remainder (75%) for testing
