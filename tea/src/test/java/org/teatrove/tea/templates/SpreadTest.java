package org.teatrove.tea.templates;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class SpreadTest extends AbstractTemplateTest {

    @Before
    public void setup() {
        addContext("SpreadApplication", new SpreadContext());
    }

    @Test
    public void testSpread() throws Exception {
        assertEquals("43", executeSource(TEST_SOURCE_1));
        assertEquals(ArrayList.class.getName(), executeSource(TEST_SOURCE_2));
        assertEquals("460", executeSource(TEST_SOURCE_3));
        assertEquals(int[].class.getName(), executeSource(TEST_SOURCE_4));
        assertEquals("[null, Minnesota, null, null, null]", executeSource(TEST_SOURCE_5));
    }

    protected static final String TEST_SOURCE_1 =
        "a = createArrayList();" +
        "void = a.add('john');" +
        "void = a.add('doe');" +
        "foreach (d in a*.length()) {" +
            "d" +
        "}";

    protected static final String TEST_SOURCE_2 =
        "a = createArrayList();" +
        "void = a.add('john');" +
        "void = a.add('doe');" +
        "d = a*.length();" +
        "d.class.name";

    protected static final String TEST_SOURCE_3 =
        "b = #('some', 'person', null);" +
        "d = b*.length();" +
        "foreach (c in d) {" +
            "c" +
        "}";

    protected static final String TEST_SOURCE_4 =
        "b = #('some', 'person', null);" +
        "t = b*.length()" +
        "t.class.name";

    protected static final String TEST_SOURCE_5 =
        "venues = getGames()*.team*.venue*.name;" +
        "venues";

    public static class Game {
        private Team team;
        public Game(Team team) {
            this.team = team;
        }

        public Team getTeam() { return this.team; }
    }

    public static class Team {
        private Venue venue;
        public Team(Venue venue) {
            this.venue = venue;
        }

        public Venue getVenue() { return this.venue; }
    }

    public static class Venue {
        private String name;
        public Venue(String name) {
            this.name = name;
        }

        public String getName() { return this.name; }
    }

    public static class SpreadContext {
        public List<String> createArrayList() {
            return new ArrayList<String>();
        }

        public List<Game> getGames() {
            return Arrays.asList
            (
                new Game(null),
                new Game(new Team(new Venue("Minnesota"))),
                null,
                new Game(new Team(null)),
                new Game(new Team(new Venue(null)))
            );
        }
    }
}
