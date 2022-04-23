import java.util.LinkedHashMap;
import java.util.Map;

import edu.uwm.cs351.util.TreeMap;
import junit.framework.TestCase;


public class TestEfficiency extends TestCase {

	private TreeMap<Integer,Integer> tree;
	private Map<Integer,Integer> large;
	
	private static final int POWER = 21; // 1 million entries
	private static final int TESTS = 100000;
	private static final int ODD = 777;
	private static final int ODD_VALUE = -55;
	
	protected void setUp() throws Exception {
		super.setUp();
		try {
			assert tree.size() == TESTS : "cannot run test with assertions enabled";
		} catch (NullPointerException ex) {
			throw new IllegalStateException("Cannot run test with assertions enabled");
		}
		tree = new TreeMap<Integer,Integer>(TestUtil.<Integer>defaultComparator());
		int max = (1 << (POWER)); // 2^(POWER) = 2 million
		for (int power = POWER; power > 1; --power) {
			int incr = 1 << power;
			for (int i=1 << (power-1); i < max; i += incr) {
				tree.put(i, power);
			}
		}
		large = new LinkedHashMap<>();
		large.putAll(tree);
		large.put(ODD, ODD_VALUE);
		tree.clear();
	}
		
	
	@Override
	protected void tearDown() throws Exception {
		tree = null;
		large = null;
		super.tearDown();
	}
	
	public void test() {
		int max = 1 << (POWER-1);
		assertEquals(max,large.size());
		tree.putAll(large);
		assertEquals(max,tree.size());
		assertEquals(POWER-1, tree.get(max/2).intValue());
		assertEquals(ODD_VALUE, tree.get(ODD).intValue());
	}

}
