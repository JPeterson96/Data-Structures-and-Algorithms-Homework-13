import edu.uwm.cs.junit.LockedTestCase;

public class UnlockTest {
	public static void main(String[] args){
		unlock("TestPutAll");
		unlock("TestInternals", "edu.uwm.cs351.util.TreeMap");
	}
	
	private static void unlock(String classname) {
		unlock(classname,classname);
	}
	private static void unlock(String infoName, String classname){
		LockedTestCase.unlockAll(infoName,classname);
		System.out.format("Tests in %s.java are unlocked.%n"
				+ "You can run it against your progam now.%n"
				+ "Remember to push %s.tst (refresh the project to show it).%n%n", classname, infoName);
	}
}
