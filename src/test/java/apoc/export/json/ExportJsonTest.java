package apoc.export.json;

import apoc.graph.Graphs;
import apoc.util.JsonUtil;
import apoc.util.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.io.File;
import java.util.Map;

import static apoc.util.MapUtil.map;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExportJsonTest {

    private static GraphDatabaseService db;
    private static File directory = new File("target/import");
    private static File directoryExpected = new File("docs/asciidoc/data/exportJSON");

    static { //noinspection ResultOfMethodCallIgnored
        directory.mkdirs();
    }

    @Before
    public void setUp() throws Exception {
        db = new TestGraphDatabaseFactory()
                .newImpermanentDatabaseBuilder()
                .setConfig(GraphDatabaseSettings.load_csv_file_url_root, directory.getAbsolutePath())
                .setConfig("apoc.export.file.enabled", "true")
                .newGraphDatabase();
        TestUtil.registerProcedure(db, ExportJson.class, Graphs.class);
        db.execute("CREATE (f:User {name:'Adam',age:42,male:true,kids:['Sam','Anna','Grace'], born:localdatetime('2015185T19:32:24'), place:point({latitude: 13.1, longitude: 33.46789})})-[:KNOWS {since: 1993}]->(b:User {name:'Jim',age:42}),(c:User {age:12})").close();
    }

    @After
    public void tearDown() {
        db.shutdown();
    }

    @Test
    public void testExportAllJson() throws Exception {
        String filename = "all.json";
        TestUtil.testCall(db, "CALL apoc.export.json.all({file},null)",
                map("file", filename),
                (r) -> {
                    assertResults(filename, r, "database");
                }
        );
        assertFileEquals(filename);
    }

    @Test
    public void testExportPointMapDatetimeJson() throws Exception {
        String filename = "mapPointDatetime.json";
        String query = "return {data: 1, value: {age: 12, name:'Mike', data: {number: [1,3,5], born: date('2018-10-29'), place: point({latitude: 13.1, longitude: 33.46789})}}} as map, " +
                "datetime('2015-06-24T12:50:35.556+0100') AS theDateTime, " +
                "localdatetime('2015185T19:32:24') AS theLocalDateTime," +
                "point({latitude: 13.1, longitude: 33.46789}) as point," +
                "date('+2015-W13-4') as date," +
                "time('125035.556+0100') as time," +
                "localTime('12:50:35.556') as localTime";
        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})",
                map("file", filename, "query", query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(7)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportListNode() throws Exception {
        String filename = "listNode.json";

        String query = "MATCH (u:User) RETURN COLLECT(u) as list";

        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})",
                map("file", filename, "query", query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(1)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportListRel() throws Exception {
        String filename = "listRel.json";

        String query = "MATCH (u:User)-[rel:KNOWS]->(u2:User) RETURN COLLECT(rel) as list";

        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})", map("file", filename,"query",query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(1)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportListPath() throws Exception {
        String filename = "listPath.json";

        String query = "MATCH p = (u:User)-[rel]->(u2:User) RETURN COLLECT(p) as list";

        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})",
                map("file", filename, "query", query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(1)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportMap() throws Exception {
        String filename = "MapNode.json";

        String query = "MATCH (u:User)-[r:KNOWS]->(d:User) RETURN u {.*}, d {.*}, r {.*}";

        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})", map("file", filename, "query", query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(3)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportMapPath() throws Exception {
        db.execute("CREATE (f:User {name:'Mike',age:78,male:true})-[:KNOWS {since: 1850}]->(b:User {name:'John',age:18}),(c:User {age:39})").close();
        String filename = "MapPath.json";

        String query = "MATCH path = (u:User)-[rel:KNOWS]->(u2:User) RETURN {key:path} as map, 'Kate' as name";

        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})",
                map("file", filename, "query", query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(2)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportMapRel() throws Exception {
        String filename = "MapRel.json";

        String query = "MATCH p = (u:User)-[rel:KNOWS]->(u2:User) RETURN rel {.*}";

        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})",
                map("file", filename,"query",query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(1)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportMapComplex() throws Exception {
        String filename = "MapComplex.json";

        String query = "RETURN {value:1, data:[10,'car',null, point({ longitude: 56.7, latitude: 12.78 }), point({ longitude: 56.7, latitude: 12.78, height: 8 }), point({ x: 2.3, y: 4.5 }), point({ x: 2.3, y: 4.5, z: 2 }),date('2018-10-10'), datetime('2018-10-18T14:21:40.004Z'), localdatetime({ year:1984, week:10, dayOfWeek:3, hour:12, minute:31, second:14, millisecond: 645 }), {x:1, y:[1,2,3,{age:10}]}]} as key";

        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})",
                map("file", filename, "query", query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(1)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportGraphJson() throws Exception {
        String filename = "graph.json";
        TestUtil.testCall(db, "CALL apoc.graph.fromDB('test',{}) yield graph " +
                        "CALL apoc.export.json.graph(graph, {file}) " +
                        "YIELD nodes, relationships, properties, file, source,format, time " +
                        "RETURN *", map("file", filename),
                (r) -> assertResults(filename, r, "graph"));
        assertFileEquals(filename);
    }

    @Test
    public void testExportQueryJson() throws Exception {
        String filename = "query.json";
        String query = "MATCH (u:User) return u.age, u.name, u.male, u.kids, labels(u)";
        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})",
                map("file", filename, "query", query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(5)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportQueryNodesJson() throws Exception {
        String filename = "query_nodes.json";
        String query = "MATCH (u:User) return u";
        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})",
                map("file", filename,"query",query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(1)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportQueryTwoNodesJson() throws Exception {
        String filename = "query_two_nodes.json";
        String query = "MATCH (u:User{name:'Adam'}), (l:User{name:'Jim'}) return u, l";
        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})", map("file", filename, "query", query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(2)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });

        assertFileEquals(filename);
    }

    @Test
    public void testExportQueryNodesJsonParams() throws Exception {
        String filename = "query_nodes_param.json";
        String query = "MATCH (u:User) WHERE u.age > {age} return u";
        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file},{params:{age:10}})",
                map("file", filename, "query", query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(1)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportQueryNodesJsonCount() throws Exception {
        String filename = "query_nodes_count.json";
        String query = "MATCH (n) return count(n)";
        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})",
                map("file", filename, "query", query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(1)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportData() throws Exception {
        String filename = "data.json";
        TestUtil.testCall(db, "MATCH (nod:User) " +
                        "MATCH ()-[reels:KNOWS]->() " +
                        "WITH collect(nod) as node, collect(reels) as rels "+
                        "CALL apoc.export.json.data(node, rels, {file}, null) " +
                        "YIELD nodes, relationships, properties, file, source,format, time " +
                        "RETURN *",
                map("file", filename),
                (r) -> {
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportDataPath() throws Exception {
        String filename = "query_nodes_path.json";
        String query = "MATCH p = (u:User)-[rel]->(u2:User) return u, rel, u2, p, u.name";
        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})",
                map("file", filename, "query", query),
                (r) -> {
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportAllWithWriteNodePropertiesJson() throws Exception {
        String filename = "writeNodeProperties.json";
        String query = "MATCH p = (u:User)-[rel:KNOWS]->(u2:User) RETURN rel";

        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file},{writeNodeProperties:true})",
                map("file", filename,"query", query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(1)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    @Test
    public void testExportQueryOrderJson() throws Exception {
        db.execute("CREATE (f:User12:User1:User0:User {name:'Alan'})").close();
        String filename = "query_node_labels.json";
        String query = "MATCH (u:User) WHERE u.name='Alan' RETURN u";

        TestUtil.testCall(db, "CALL apoc.export.json.query({query},{file})", map("file", filename,"query",query),
                (r) -> {
                    assertTrue("Should get statement",r.get("source").toString().contains("statement: cols(1)"));
                    assertEquals(filename, r.get("file"));
                    assertEquals("json", r.get("format"));
                });
        assertFileEquals(filename);
    }

    private void assertResults(String filename, Map<String, Object> r, final String source) {
        assertEquals(3L, r.get("nodes"));
        assertEquals(1L, r.get("relationships"));
        assertEquals(10L, r.get("properties"));
        assertEquals(source + ": nodes(3), rels(1)", r.get("source"));
        assertEquals(filename, r.get("file"));
        assertEquals("json", r.get("format"));
        assertTrue("Should get time greater than 0",((long) r.get("time")) >= 0);
    }

    private void assertFileEquals(String fileName) {
        String expectedText = TestUtil.readFileToString(new File(directoryExpected, fileName));
        String actualText = TestUtil.readFileToString(new File(directory, fileName));
        assertEquals(JsonUtil.parse(expectedText,null,Object.class), JsonUtil.parse(actualText,null,Object.class));
    }
}
