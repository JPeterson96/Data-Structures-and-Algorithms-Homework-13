import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;

import edu.uwm.cs.junit.LockedTestCase;
import edu.uwm.cs351.test.ImmutableMap;
import edu.uwm.cs351.util.DefaultEntry;
import edu.uwm.cs351.util.TreeMap;

public class TestPutAll extends LockedTestCase {
	protected TreeMap<String,Integer> t;

    protected static void assertException(Class<? extends Throwable> c, Runnable r) {
    	try {
    		r.run();
    		assertFalse("Exception should have been thrown",true);
        } catch (RuntimeException ex) {
        	assertTrue("should throw exception of " + c + ", not of " + ex.getClass(), c.isInstance(ex));
        }	
    }	

	@Override
	protected void setUp() {
		try {
			assert t.get(null) == t.size();
			assertTrue("Assertions not enabled.  Add -ea to VM Args Pane in Arguments tab of Run Configuration",false);
		} catch (NullPointerException ex) {
			// OK!
		}
		t = new TreeMap<>();
	}

	protected <K,V> Map.Entry<K, V> e(K k, V v) {
		return new DefaultEntry<>(k,v);
	}
	
	@SafeVarargs
	protected final <K,V> Map<K,V> m(Map.Entry<K, V>... e) {
		return new ImmutableMap<>(e);
	}
	
	
	/// locked tests
	
	public void test() {
		// t is empty, and then:
		t.put("apples", 3);
		t.put("bananas", 0);
		t.putAll(m(e("oranges",3), e("apples", 8)));
		assertEquals(Ti(1587707329), t.size());
		assertEquals(Ti(1497208), t.get("apples").intValue());
	}
	
	
	/// Regular tests
	
	public void testA() {
		t.putAll(m());
		assertTrue(t.isEmpty());
	}
	
	public void testB() {
		t.put("apples", 10);
		t.putAll(m());
		assertEquals(1, t.size());
		assertEquals(10, t.get("apples").intValue());
	}
	
	public void testC() {
		t.putAll(m(e("apples",3)));
		assertEquals(1, t.size());
		assertEquals(3, t.get("apples").intValue());
	}
	
	public void testD() {
		t.put("apples", 8);
		t.putAll(m(e("apples", 4), e("oranges", 10)));
		assertEquals(2, t.size());
		assertEquals(4, t.get("apples").intValue());
		assertEquals(10, t.get("oranges").intValue());
	}
	
	public void testE() {
		t.put("apples", 5);
		t.put("bananas", 10);
		
		t.putAll(m(e("cherries",20),e("bananas",0),e("oranges",15)));
		
		assertEquals(4, t.size());
		assertEquals(0, t.get("bananas").intValue());
		assertEquals(20, t.get("cherries").intValue());
	}
	
	public void testF() {
		t.put("apples", 1);
		t.put("bananas", 2);
		t.put("cherries", 3);
		
		t.putAll(m(e("lemons",7), e("bananas",4), e("cherries",5), e("apples",6)));

		assertEquals(4, t.size());
		assertEquals(6, t.get("apples").intValue());
		assertEquals(4, t.get("bananas").intValue());
		assertEquals(5, t.get("cherries").intValue());
		assertEquals(7, t.get("lemons").intValue());
	}
	
	public void testG() {
		t = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		t.put("Apples",	5);
		t.put("bananas", 33);
		
		t.putAll(m(e("apples", 4), e("APPLES", 8), e("aPPles", 1), e("Apples", 2), e("AppleS", 3)));
		
		assertEquals(2, t.size());
		assertEquals(3, t.get("apples").intValue());
		assertEquals(33, t.get("Bananas").intValue());
	}
	
	public void testH() {
		t.put("one", 1);
		t.put("two", 2);
		t.put("three", 3);
		t.put("four", 4);
		
		Iterator<Map.Entry<String,Integer>> it = t.entrySet().iterator();
		it.next();
		
		t.putAll(m(e("five",5), e("six",6), e("seven",7), e("eight",8), e("three", -3)));
		assertEquals(8, t.size());
		assertEquals(1, t.get("one").intValue());
		assertEquals(2, t.get("two").intValue());
		assertEquals(-3, t.get("three").intValue());
		assertEquals(4, t.get("four").intValue());
		assertEquals(5, t.get("five").intValue());
		assertEquals(6, t.get("six").intValue());
		assertEquals(7, t.get("seven").intValue());
		assertEquals(8, t.get("eight").intValue());	
		
		assertException(ConcurrentModificationException.class, () -> it.hasNext());
	}
	
	public void testI() {
		t = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		t.put("one", 1);
		t.put("two", 2);
		t.put("three", 3);
		t.put("four", 4);
		t.put("five", 5);
		
		t.putAll(m(e("FOUR", -4), e("Four", 400), e("FIVE", -5), e("SIX", -6), e("six", 6), e("Seven", 700)));

		assertEquals(7, t.size());
		assertEquals(1, t.get("one").intValue());
		assertEquals(2, t.get("two").intValue());
		assertEquals(3, t.get("three").intValue());
		assertEquals(400, t.get("four").intValue());
		assertEquals(-5, t.get("five").intValue());
		assertEquals(6, t.get("six").intValue());
		assertEquals(700, t.get("seven").intValue());
	}
}
