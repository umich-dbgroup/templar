package edu.umich.templar.db;

import edu.umich.templar.baseline.BaselineParams;
import net.sf.jsqlparser.expression.StringValue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.sql.*;
import java.util.*;

/**
 * Created by cjbaik on 1/31/18.
 */
public class Database {
    String name;

    Connection connection;
    Set<Relation> relations;

    Set<Attribute> textAttributes;
    Set<Attribute> numericAttributes;

    Map<Attribute, Attribute> fkpk;
    Map<Attribute, List<Attribute>> pkfk;

    public Database(String host, int port, String user, String pw, String dbName,
                    String fkpkFile, String mainAttrsFile) {
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName;
        this.name = dbName;

        System.out.println("Connecting to database: <" + url + ">");
        try {
            Class.forName("com.mysql.jdbc.Driver");
            this.connection = DriverManager.getConnection(url, user, pw);
            System.out.println("Database connected!");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot connect the database!", e);
        }

        this.textAttributes = new HashSet<>();
        this.numericAttributes = new HashSet<>();
        this.loadRelations();

        this.fkpk = new HashMap<>();
        this.pkfk = new HashMap<>();
        this.loadFKPK(fkpkFile);

        this.loadMainAttrs(mainAttrsFile);
    }

    public ResultSet executeSQL(String sql) throws SQLException {
        Statement stmt = this.connection.createStatement();
        return stmt.executeQuery(sql);
    }

    public String escapeSQL(String str) {
        return str.replaceAll("'", "''");
    }

    public StringValue getStringValue(String str) {
        return new StringValue(this.escapeSQL(str));
    }

    public Relation getRelationByName(String relName) {
        for (Relation rel : this.relations) {
            if (rel.getName().toLowerCase().equals(relName.toLowerCase())) return rel;
        }
        return null;
    }

    public void loadFKPK(String filename) {
        try {
            JSONParser jsonParser = new JSONParser();
            JSONArray jsonArr = (JSONArray) jsonParser.parse(new FileReader(filename));
            for (Object obj : jsonArr) {
                JSONObject jsonObj = (JSONObject) obj;
                String foreignRelName = (String) jsonObj.get("foreignRelation");
                Relation foreignRel = this.getRelationByName(foreignRelName);
                Attribute foreignAttr = new Attribute(foreignRel, "INT", (String) jsonObj.get("foreignAttribute"));
                String primaryRelName = (String) jsonObj.get("primaryRelation");
                Relation primaryRel = this.getRelationByName(primaryRelName);
                Attribute primaryAttr = new Attribute(primaryRel, "INT", (String) jsonObj.get("primaryAttribute"));

                this.fkpk.put(foreignAttr, primaryAttr);

                // Delete foreign keys from numeric attributes
                this.numericAttributes.remove(foreignAttr);

                List<Attribute> attrs = this.pkfk.get(primaryAttr);
                if (attrs == null) attrs = new ArrayList<>();
                attrs.add(foreignAttr);
                this.pkfk.put(primaryAttr, attrs);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void loadMainAttrs(String filename) {
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObj = (JSONObject) jsonParser.parse(new FileReader(filename));
            for (Object obj : jsonObj.keySet()) {
                String relName = (String) obj;
                String mainAttrName = (String) jsonObj.get(relName);

                Relation rel = this.getRelationByName(relName);
                rel.setMainAttribute(mainAttrName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void loadRelation(String relName) {
        Relation rel = new Relation(relName.toLowerCase());
        try {
            DatabaseMetaData metadata = this.connection.getMetaData();
            ResultSet resultSet = metadata.getColumns(null, null, relName, "%");
            while (resultSet.next()) {
                String name = resultSet.getString("COLUMN_NAME").toLowerCase();
                String type = resultSet.getString("TYPE_NAME");

                Attribute attr = new Attribute(rel, type, name);
                rel.addAttribute(attr);

                if (attr.getType().equals(AttributeType.TEXT)) {
                    this.textAttributes.add(attr);

                    Integer charLength = resultSet.getInt("CHAR_OCTET_LENGTH");
                    attr.setTextLength(charLength);
                } else if (attr.getType().equals(AttributeType.INT) ||
                        attr.getType().equals(AttributeType.DOUBLE)) {
                    this.numericAttributes.add(attr);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.relations.add(rel);
    }

    private void loadRelations() {
        this.relations = new HashSet<>();
        Set<String> exclude = new HashSet<>();
        exclude.add("size");
        exclude.add("history");
        exclude.add("ids");
        exclude.add("index_number");
        exclude.add("index_string");

        try {
            DatabaseMetaData md = this.connection.getMetaData();
            String[] types = {"TABLE"};
            ResultSet rs = md.getTables(this.name, null, "%", types);
            while (rs.next()) {
                String relName = rs.getString(3);
                if (exclude.contains(relName)) continue;
                this.loadRelation(relName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Attribute> getAllAttributes() {
        Set<Attribute> all = new HashSet<>();
        for (Relation rel : this.relations) {
            all.addAll(rel.getAttributes());
        }
        return all;
    }

    public Set<Relation> getAllRelations() {
        return this.relations;
    }

    public List<TextPredicate> getSimilarValues(List<String> tokens) {
        List<TextPredicate> values = new ArrayList<>();
        for (Attribute attr : this.textAttributes) {
            try {
                // Get size of relation
                Statement sizeStmt = this.connection.createStatement();
                ResultSet sizeRs = sizeStmt.executeQuery("SELECT COUNT(*) "
                        + "FROM " + attr.getRelation().getName());
                sizeRs.next();
                int size = sizeRs.getInt(1);

                Statement stmt = this.connection.createStatement();

                String query;
                if (size <= 10000) {
                    query = "SELECT " + attr.getName() + " FROM " + attr.getRelation().getName()
                            + " WHERE ";

                    StringJoiner sj = new StringJoiner(" OR ");
                    for (String token : tokens) {
                        sj.add("(" + attr.getName() + " LIKE " + "'%" + this.escapeSQL(token) + "%'" + ")");
                    }
                    query += sj.toString();
                } else {
                    StringJoiner sj = new StringJoiner(" ");
                    for (String token : tokens) {
                        if (token.length() >= BaselineParams.MIN_FULLTEXT_TOKEN_LENGTH) {
                            // When the token matches the relation or the attribute name, make it an OR instead of AND
                            if (token.equalsIgnoreCase(attr.getName())
                                    || token.equalsIgnoreCase(attr.getRelation().getName())) {
                                sj.add(token);
                            } else {
                                sj.add("+" + token);
                            }
                        }
                    }
                    query = "SELECT " + attr.getName() + " FROM " + attr.getRelation().getName()
                            + " WHERE MATCH(" + attr.getName() + ")"
                            + " AGAINST ('" + sj.toString() + "' IN BOOLEAN MODE)";
                }

                ResultSet rs = stmt.executeQuery(query);
                while (rs.next()) {
                    String stringValue = rs.getString(1);

                    // Limit max token value in results
                    if (stringValue.length() <= BaselineParams.MAX_CHAR_LENGTH) {
                        TextPredicate val = new TextPredicate(attr, stringValue);
                        values.add(val);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return values;
    }

    public Set<Attribute> getNumericAttributes() {
        return numericAttributes;
    }

    public Set<Attribute> getTextAttributes() {
        return textAttributes;
    }

    public boolean hasJoin(Relation r1, Relation r2) {
        for (Attribute a1 : r1.getAttributes()) {
            Attribute a2 = this.fkpk.get(a1);
            if (a2 != null && a2.getRelation().equals(r2)) return true;

            List<Attribute> a2List = this.pkfk.get(a1);
            if (a2List != null) {
                for (Attribute a2fk : a2List) {
                    if (a2fk.getRelation().equals(r2)) return true;
                }
            }
        }
        return false;
    }

    public List<Relation> longestJoinPathRecursive(List<Relation> path, List<Relation> rels) {
        // Special case: start with empty path
        if (path == null || path.isEmpty()) {
            List<Relation> longestPath = new ArrayList<>();
            for (Relation curRel : rels) {
                List<Relation> newPath = new ArrayList<>();
                newPath.add(curRel);
                List<Relation> newRemaining = new ArrayList<>(rels);
                newRemaining.remove(curRel);
                List<Relation> curPath = this.longestJoinPathRecursive(newPath, newRemaining);
                if (curPath.size() > longestPath.size()) {
                    longestPath = curPath;
                }
            }
            return longestPath;
        }

        // Base case, empty rels
        if (rels.isEmpty()) return path;

        // Otherwise, start with last relation in path and see how far we can get
        Relation curRel = path.get(path.size() - 1);
        List<Relation> longestPath = path;
        for (Relation other : rels) {
            if (this.hasJoin(curRel, other)) {
                List<Relation> newPath = new ArrayList<>(path);
                newPath.add(other);
                List<Relation> newRemaining = new ArrayList<>(rels);
                newRemaining.remove(other);
                List<Relation> curPath = this.longestJoinPathRecursive(newPath, newRemaining);
                if (curPath.size() > longestPath.size()) {
                    longestPath = curPath;
                }
            }
        }
        return longestPath;
    }

    public int longestJoinPathLength(Set<Relation> rels) {
        List<Relation> relsList = new ArrayList<>(rels);
        return this.longestJoinPathRecursive(null, relsList).size();
    }

    public void close() {
        try {
            this.connection.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
