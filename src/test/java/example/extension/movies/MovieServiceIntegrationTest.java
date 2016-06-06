package example.extension.movies;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.server.rest.JaxRsResponse;
import org.neo4j.server.rest.RestRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.map;

@SuppressWarnings("unchecked")
public class MovieServiceIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private RestRequest REST_REQUEST;

    private static final String SETUP = "CREATE (:Movie {title:'The Matrix', released: 1999, tagline: 'The one and only'})" +
            " <-[:ACTED_IN {roles:['Neo']}]-" +
            " (:Person {name:'Keanu Reeves',born:1964})";

    @Rule
    public Neo4jRule server = new Neo4jRule().withExtension("/movie","example.extension.movies").withFixture(SETUP);

    @Before
    public void setUp() throws Exception {
        System.err.println(server.httpURI());
        REST_REQUEST = new RestRequest(server.httpURI());
    }

    @Test
    public void testFindMovie() throws Exception {
        JaxRsResponse response = REST_REQUEST.get("movie/The%20Matrix");
        assertEquals(200, response.getStatus());
        Map movie = OBJECT_MAPPER.readValue(response.getEntity(), Map.class);
        assertEquals("The Matrix", movie.get("title"));
        List<Map> cast = (List<Map>) movie.get("cast");
        assertEquals(1, cast.size());
        Map entry = cast.get(0);
        assertEquals("Keanu Reeves", entry.get("name"));
        assertEquals("acted", entry.get("job"));
        assertEquals(Collections.singletonList("Neo"), entry.get("role"));
    }

    @Test
    public void testSearch() throws Exception {
        JaxRsResponse response = REST_REQUEST.get("movie/search?q=matr");
        assertEquals(200, response.getStatus());
        List<Map<String, Object>> result = OBJECT_MAPPER.readValue(response.getEntity(), List.class);
        Map<String, Object> movie = (Map<String, Object>) result.get(0).get("movie");
        assertEquals("The Matrix", movie.get("title"));
        assertEquals(1999, ((Number)movie.get("released")).intValue());
        assertEquals("The one and only", movie.get("tagline"));
    }

    @Test
    public void testGraph() throws Exception {
        JaxRsResponse response = REST_REQUEST.get("movie/graph?limit=10");
        assertEquals(200, response.getStatus());
        Map<String, List<Map<String, Object>>> graph = OBJECT_MAPPER.readValue(response.getEntity(), Map.class);
        List<Map<String, Object>> nodes = graph.get("nodes");
        assertEquals(asList(map("label", "movie", "title", "The Matrix"), map("label", "actor", "title", "Keanu Reeves")), nodes);
        List<Map<String, Object>> links = graph.get("links");
        assertEquals(Collections.singletonList(map("source", 1, "target", 0)), links);
    }
}
