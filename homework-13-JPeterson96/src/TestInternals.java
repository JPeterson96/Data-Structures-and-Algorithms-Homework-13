import edu.uwm.cs351.util.TreeMap;


public class TestInternals extends TreeMap.TestSuite {
	// all tests are inherited.  Just run this test case.

	TreeMap<String,Integer> t;
	
	@Override
	protected void setUp() {
		super.setUp();
		try {
			assert t.get(null) == t.size();
			assertTrue("Assertions not enabled.  Add -ea to VM Args Pane in Arguments tab of Run Configuration",false);
		} catch (NullPointerException ex) {
			// OK!
		}
		t = new TreeMap<>();
	}

}
