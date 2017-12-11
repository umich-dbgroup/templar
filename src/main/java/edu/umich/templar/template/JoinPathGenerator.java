package edu.umich.templar.template;

import com.esotericsoftware.minlog.Log;
import edu.umich.templar.rdbms.*;
import edu.umich.templar.util.Utils;
import net.sf.jsqlparser.statement.select.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by cjbaik on 6/30/17.
 */
public class JoinPathGenerator {
    RDBMS db;

    int joinLevel;

    Map<String, Relation> relations;
    Map<Attribute, Set<Attribute>> fkpkEdges;
    Map<Attribute, Set<Attribute>> pkfkEdges;

    // keeps track of join paths already generated so we don't have duplicates
    Set<JoinPath> generatedJoinPaths;

    public JoinPathGenerator(RDBMS db, int joinLevel) {
        this.joinLevel = joinLevel;
        this.relations = db.schemaGraph.relations;
        TemplateRoot.relations = this.relations; // Kind of a hack, but want to store globally for TemplateRoot fns

        this.fkpkEdges = db.schemaGraph.fkpkEdges;
        this.pkfkEdges = db.schemaGraph.pkfkEdges;
        this.db = db;

        this.generatedJoinPaths = new HashSet<>();
    }

    public Map<String, Relation> getRelations() {
        return relations;
    }

    public Set<Set<Attribute>> topAttributesAllRelations(List<Relation> relations, Integer chooseTop, Integer maxSize) {
        List<Attribute> attributes = new ArrayList<>();

        for (Relation r : relations) {
            // Assume we don't want any primary/foreign keys
            r.getRankedAttributes().stream().filter(attr -> !attr.isFk() && !attr.isPk()).forEach(attributes::add);
        }

        // Select top few only to do power sets (from all relations combined, using entropy)
        if (chooseTop != null) {
            attributes.sort((a, b) -> b.getEntropy().compareTo(a.getEntropy()));
            attributes = attributes.subList(0, Math.min(attributes.size(), chooseTop));
        }

        if (maxSize == null) {
            maxSize = attributes.size();
        }

        return Utils.powerSet(attributes, maxSize);
    }

    public Set<Set<Attribute>> topAttributesEachRelation(List<Relation> relations, Integer chooseTop, Integer maxSize) {
        List<Attribute> attributes = new ArrayList<>();

        for (Relation r : relations) {
            // Assume we don't want any primary/foreign keys
            List<Attribute> rankedAttr = r.getRankedAttributes().stream()
                    .filter(attr -> !attr.isFk() && !attr.isPk())
                    .collect(Collectors.toList());
            if (chooseTop == null) {
                rankedAttr.stream().forEach(attributes::add);
            } else {
                rankedAttr.subList(0, Math.min(rankedAttr.size(), chooseTop))
                        .stream().forEach(attributes::add);
            }
        }

        if (maxSize == null){
            maxSize = attributes.size();
        }

        return Utils.powerSet(attributes, maxSize);
    }

    public Set<Set<Attribute>> guessProjections(List<Relation> relations, Integer chooseTop, Integer maxSize, boolean each) {
        if (each) return this.topAttributesEachRelation(relations, chooseTop, maxSize);
        else return this.topAttributesAllRelations(relations, chooseTop, maxSize);
    }

    public Set<Set<Attribute>> guessPredicateAttributes(List<Relation> relations, Integer chooseTop, Integer maxSize, boolean each) {
        if (each) return this.topAttributesEachRelation(relations, chooseTop, maxSize);
        else return this.topAttributesAllRelations(relations, chooseTop, maxSize);
    }

    public Set<Template> exploreJoinPathForAttr(Attribute attr, Attribute otherAttr, Select select, List<Relation> relations,
                                                   JoinPath joinPath, int joinLevelsLeft) {
        int aliasInt = 0;
        for (Relation curRel : relations) {
            if (curRel.getName().equals(otherAttr.getRelation().getName())) {
                aliasInt++;
            }
        }

        if (aliasInt > 0) {
            Attribute newOtherAttr = new Attribute(otherAttr);
            Relation newRelation = new Relation(otherAttr.getRelation());
            newRelation.setAliasInt(aliasInt);
            newOtherAttr.setRelation(newRelation);
            otherAttr = newOtherAttr;
        }

        // Don't go beyond 3 tables (for our computer's sake...)
        if (aliasInt > 2) return new HashSet<>();

        JoinPath newJoinPath = new JoinPath(joinPath);

        JoinEdge edge = new JoinEdge(attr, otherAttr);

        // If we couldn't add the new join edge, skip!
        if (!newJoinPath.add(edge)) return new HashSet<>();

        Join join = Utils.addJoin(select, attr, otherAttr);
        List<Relation> newRelations = new ArrayList<>(relations);
        newRelations.add(otherAttr.getRelation());

        Set<Template> templates = this.getTemplatesForRelationsRecursive(select, newRelations, newJoinPath, joinLevelsLeft - 1);

        // Reset Select to original state after recursion
        ((PlainSelect) select.getSelectBody()).getJoins().remove(join);
        return templates;
    }

    public Set<Template> getTemplatesForRelationsRecursive(Select select, List<Relation> relations,
                                                           JoinPath joinPath, int joinLevelsLeft) {

        // Pruning for optimization
        int maxTerminals = 3;
        if (joinPath.getTerminals().size() > maxTerminals) {
            return new HashSet<>();
        }

        // Check that we haven't already explored this join path
        if (!joinPath.isEmpty()) {
            if (this.generatedJoinPaths.contains(joinPath)) {
                return new HashSet<>();
            }
            this.generatedJoinPaths.add(joinPath);
        }

        Set<Template> templates = new HashSet<>();
        PlainSelect ps = (PlainSelect) select.getSelectBody();

        if (joinLevelsLeft > 0) {
            for (Relation rel : relations) {
                for (Map.Entry<String, Attribute> attrEntry : rel.getAttributes().entrySet()) {
                    Attribute attr = attrEntry.getValue();

                    // If this is the PK, add self-joins
                    /*
                    if (attr.isPk()) {
                        int aliasInt = 0;
                        for (Relation curRel : relations) {
                            if (curRel.getName().equals(rel.getName())) {
                                aliasInt++;
                            }
                        }
                        // Only do this self-join once
                        if (aliasInt == 1) {
                            Relation newRelation = new Relation(rel);
                            newRelation.setAliasInt(aliasInt);
                            Attribute newAttribute = new Attribute(attr);
                            newAttribute.setRelation(newRelation);

                            JoinPath newJoinPath = new JoinPath(joinPath);

                            if (newJoinPath.add(new JoinEdge(attr, newAttribute))) {
                                Join join = Utils.addJoin(select, attr, newAttribute);
                                List<Relation> newRelations = new ArrayList<>(relations);
                                newRelations.add(newRelation);

                                templates.addAll(this.getTemplatesForRelationsRecursive(select, newRelations, newJoinPath, joinLevelsLeft - 1));

                                // Reset Select to original state after recursion
                                ps.getJoins().remove(join);
                            }
                        }
                    }*/

                    // HACK: Need this because we're creating new attributes on the fly that might not be found in the
                    // original edges maps
                    Attribute checkAttr = new Attribute(attr);
                    checkAttr.setRelation(this.relations.get(attr.getRelation().getName()));

                    Set<Attribute> fks = this.pkfkEdges.get(checkAttr);
                    if (fks != null) {
                        for (Attribute otherFk : fks) {
                            templates.addAll(this.exploreJoinPathForAttr(attr, otherFk, select, relations, joinPath, joinLevelsLeft));
                        }
                    }

                    Set<Attribute> pks = this.fkpkEdges.get(checkAttr);
                    if (pks != null) {
                        for (Attribute otherPk : pks) {
                            templates.addAll(this.exploreJoinPathForAttr(attr, otherPk, select, relations, joinPath, joinLevelsLeft));
                        }
                    }
                }
            }
        }

        // if (!joinPath.passesSelfJoinCheck()) return templates;

        /*
         * Generate templates
         */

        TemplateRoot tr = new TemplateRoot(select);

        // Determines max size of projections. If null, use all.
        Integer chooseTopProjection = 4;
        Integer maxProjectionSize = 1;
        Integer chooseTopPredicate = 5;
        Integer maxPredicateSize = 3;

        // If true, chooses top n from each relation
        // If false, chooses top n from all relations

        /*
        boolean chooseTopEachRelationProjection = false;
        boolean chooseTopEachRelationPredicate = false;

        Set<Set<Attribute>> projections = this.guessProjections(relations, chooseTopProjection,
                maxProjectionSize, chooseTopEachRelationProjection);
        Set<Set<Attribute>> predicateAttributes = this.guessPredicateAttributes(relations, chooseTopPredicate,
                maxPredicateSize, chooseTopEachRelationPredicate);
                */

        templates.addAll(tr.generateNoPredicateProjectionTemplates(relations, joinPath));
        // templates.addAll(tr.generateNoAttributeConstantTemplates(maxProjectionSize, maxPredicateSize, relations, joinPath));
        // templates.addAll(tr.generateNoPredicateTemplates(projections, relations, joinPath));
        // templates.addAll(tr.generateNoComparisonProjectionTemplates(predicateAttributes, relations, joinPath));
        // templates.addAll(tr.generateNoComparisonTemplates(projections, predicateAttributes, relations, joinPath));
        // templates.addAll(tr.generateNoConstantProjectionTemplates(predicateAttributes, relations, joinPath));
        // templates.addAll(tr.generateNoConstantTemplates(projections, predicateAttributes, relations, joinPath));

        // TODO: for each hypothesized full query template (guess attributes, operators, constants, and projections)

        return templates;
    }

    public Set<Template> generate() {
        Set<Template> templates = new HashSet<>();

        Log.info("==============================");
        Log.info("Generating join paths using schema for join level: " + this.joinLevel);

        /*
        for (Map.Entry<String, Relation> e : this.relations.entrySet()) {
            // Rank attributes by entropy first
            e.getValue().rankAttributesByEntropy(this.db);
        }*/

        for (Map.Entry<String, Relation> e : this.relations.entrySet()) {
            Relation r = e.getValue();
            List<Relation> relations = new ArrayList<>();
            relations.add(r);

            Select select = new Select();
            PlainSelect ps = new PlainSelect();
            select.setSelectBody(ps);
            ps.setFromItem(r.getFromItem());

            templates.addAll(this.getTemplatesForRelationsRecursive(select, relations, new JoinPath(), this.joinLevel));
        }

        Log.info("Done generating " + templates.size() + " join paths.");

        int fullQueryCount = 0;
        int noPredProjCount = 0;
        int noPredCount = 0;
        int noAttrConstCount = 0;
        int noCompProjCount = 0;
        int noCompCount = 0;
        int noConstProjCount = 0;
        int noConstCount = 0;
        for (Template template : templates) {
            switch (template.getType()) {
                case FULL_QUERY:
                    fullQueryCount++;
                    break;
                case NO_PRED_PROJ:
                    noPredProjCount++;
                    break;
                case NO_PRED:
                    noPredCount++;
                    break;
                case NO_ATTR_CONST:
                    noAttrConstCount++;
                    break;
                case NO_CONST_OP_PROJ:
                    noCompProjCount++;
                    break;
                case NO_CONST_OP:
                    noCompCount++;
                    break;
                case NO_CONST_PROJ:
                    noConstProjCount++;
                    break;
                case NO_CONST:
                    noConstCount++;
                    break;
            }
        }

        /*
        Log.info("Full Query Count: " + fullQueryCount);
        Log.info("No Predicate/Projection: " + noPredProjCount);
        Log.info("No Predicate: " + noPredCount);
        Log.info("No Comparison/Projection: " + noCompProjCount);
        Log.info("No Comparison: " + noCompCount);
        Log.info("No Constants/Projection: " + noConstProjCount);
        Log.info("No Constants: " + noConstCount);*/

        Log.info("==============================\n");
        return templates;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: <schema-prefix> <join-level> <db-name>");
            System.err.println("Example: SchemaDataTemplateGenerator data/mas/mas 0 mas");
            System.exit(1);
        }

        String prefix = args[0];

        Integer joinLevel = Integer.valueOf(args[1]);
        String dbName = args[2];

        RDBMS db = null;
        try {
            db = new RDBMS(dbName, prefix);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JoinPathGenerator tg = new JoinPathGenerator(db, joinLevel);
        Set<Template> templates = tg.generate();

        templates.stream().map(Template::toStringDebug).forEach(System.out::println);

        /*
        String errorFileName = "template_errors.out";

        Log.info("==============================");
        Log.info("Reading templates generated from query log (i.e. test set)...");
        Map<String, Integer> testTemplateCount = new HashMap<>();
        try {
            CSVReader reader = new CSVReader(new FileReader(logTemplatesFile), '\t');
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                String testTmpl = nextLine[1].toLowerCase();
                testTemplateCount.put(testTmpl, Integer.valueOf(nextLine[0]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.info("Finished reading query log templates.");
        Log.info("==============================\n");

        int covered = 0;
        int total = 0;
        CSVWriter writer;
        try {
            writer = new CSVWriter(new FileWriter(errorFileName), '\t', CSVWriter.NO_QUOTE_CHARACTER);

            for (Map.Entry<String, Integer> e : Utils.sortByValueDesc(testTemplateCount).entrySet()) {
                total += e.getValue();
                if (templates.contains(e.getKey())) {
                    covered += e.getValue();
                } else {
                    String[] row = new String[2];
                    row[0] = e.getValue().toString();
                    row[1] = e.getKey();
                    writer.writeNext(row);
                }
            }


            Log.info("==============================");
            Log.info("Measuring coverage...");
            float predProjCoverage = (float) covered / total * 100;
            Log.info("Number of templates: " + templates.size());
            Log.info(String.format("Coverage: %d / %d (%.1f)", covered, total, predProjCoverage) + "%");
            Log.info("==============================\n");

            Log.info("==============================");
            Log.info("Templates not covered were printed to <" + errorFileName + ">.");

            writer.close();

            Log.info("==============================");

        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        // Test coverage for random string
        // String test = "SELECT journal.homepage FROM journal WHERE journal.name = 'PVLDB'";
        // String test = "SELECT j.homepage FROM journal j WHERE j.name = 'PVLDB'";
        // String test = "SELECT j.homepage FROM journal AS j WHERE j.name = 'PVLDB'";
        // String test = "SELECT homepage FROM journal AS j WHERE journal.name = 'PVLDB'";
        // String test = "SELECT a.name FROM author a, publication p, journal j, writes w WHERE j.name = 'PVLDB' AND a.aid = w.aid AND w.pid = p.pid AND p.jid = j.jid";
        // String test = "SELECT a.name FROM author AS a JOIN writes AS w ON a.aid = w.aid JOIN publication AS p ON w.pid = p.pid JOIN journal AS j ON p.jid = j.jid WHERE j.name = 'PVLDB'";
        // String test = "SELECT a.name FROM author AS a JOIN writes AS w ON a.aid = w.aid JOIN publication AS p ON w.pid = p.pid JOIN journal AS j ON p.jid = j.jid WHERE j.name = 'PVLDB'";
        // String test = "SELECT a.name FROM author AS a JOIN writes AS w ON a.aid = w.aid JOIN publication AS p ON w.pid = p.pid JOIN journal AS j ON p.jid = j.jid WHERE j.name = 'PVLDB' AND (j.name = 'THIS' OR j.name = 'OTHER')";
        // String test = "SELECT p.title FROM publication p, writes w, author a, organization o WHERE p.pid = w.pid AND w.aid = a.aid AND a.oid = o.oid AND o.name = 'University of Michigan'";
        // String test = "select publication_0.title from author as author_0, organization as organization_0, publication as publication_0, writes as writes_0 where author_0.aid = writes_0.aid and author_0.oid = organization_0.oid and organization_0.name = 'university of michigan' and publication_0.pid = writes_0.pid";
        // String test = "SELECT homepage FROM author WHERE name = 'H. V. Jagadish'";
        // String test = "SELECT DISTINCT course_0.name, course_0.number FROM (SELECT max(program_coursealias1.workload) AS derived_fieldalias0 FROM program_course AS program_coursealias1) AS derived_tablealias0, course AS coursealias0, program_course AS program_coursealias0 WHERE program_coursealias0.course_id = coursealias0.course_id AND program_coursealias0.workload = derived_tablealias0.derived_fieldalias0";
        /*
        try {
            Statement stmt = CCJSqlParserUtil.parse(test);
            System.out.println(stmt);
            System.out.println(TemplateRoot.fullQueryTemplate((Select) stmt));
            System.out.println(TemplateRoot.noConstantTemplate((Select) stmt));
            System.out.println(TemplateRoot.noConstantProjectionTemplate((Select) stmt));
            System.out.println(TemplateRoot.noComparisonTemplate((Select) stmt));
            System.out.println(TemplateRoot.noComparisonProjectionTemplate((Select) stmt));
            System.out.println(TemplateRoot.noPredicateTemplate((Select) stmt));
            System.out.println(TemplateRoot.noPredicateProjectionTemplate((Select) stmt));

            System.out.println(stmt);
        } catch (JSQLParserException e) {
            if (Log.DEBUG) e.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }*/

        /*
        String templateOutFile = "templates.out";
        Log.info("==============================");
        Log.info("Printing all templates generated to <" + templateOutFile + ">.");
        try {
            PrintWriter tmplWriter = new PrintWriter(templateOutFile, "UTF-8");
            for (Template tmpl : templates) {
                tmplWriter.println(tmpl);
            }
            tmplWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.info("==============================");*/
    }
}
