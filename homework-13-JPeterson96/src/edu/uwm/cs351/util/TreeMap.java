/*
 * Jamie Peterson
 * 
 * I was with the tutors working on this assignment on 12/1/2021 from 4pm - 5:30pm. They helped me fix my wellFormed because my 
 * logic was way off. I only discussed code/logic with the tutors and did not discuss any code/logic with any students.
 * 
 */

package edu.uwm.cs351.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import java.util.function.Supplier;

import edu.uwm.cs.junit.LockedTestCase;

public class TreeMap<K,V>  extends AbstractMap<K,V> {

	// Here is the data structure to use.
	
	private static class Node<K,V> extends DefaultEntry<K,V> {
		Node<K,V> left, right;
		Node(K k, V v) {
			super(k,v);
			left = right = null;
		}
	}
	
	private Comparator<K> comparator;
	private Node<K,V> root;
	private int numItems = 0;
	private int version = 0;
	
	
	/// Invariant checks:
	
	private static boolean doReport = true;
	
	private boolean report(String message) {
		if (doReport) System.err.println("Invariant error: " + message);
		return false;
	}
	
	/**
	 * Return true if keys in this subtree are never null and are correctly sorted
	 * and are all in the range between the lower and upper (both exclusive).
	 * If either bound is null, then that means that there is no limit at this side.
	 * @param node root of subtree to examine
	 * @param lower value that all nodes must be greater than.  If null, then
	 * there is no lower bound.
	 * @param upper value that all nodes must be less than. If null,
	 * then there is no upper bound.
	 * @return true if the subtree is fine.  If false is returned, a problem
	 * should have already been reported.
	 */
	private boolean checkInRange(Node<K,V> node, K lower, K upper) {
		if (node == null) return true;
		if (node.key == null) return report("Found null data in tree");
		if (lower != null && comparator.compare(lower, node.key) >= 0) return report("Found out of order data: " + node.key);
		if (upper != null && comparator.compare(node.key,upper) >= 0) return report("Found out of order data: " + node.key);
		return checkInRange(node.left,lower,node.key) && 
		       checkInRange(node.right,node.key,upper);
	}
	
	/**
	 * Return the number of nodes in the subtree rooted at n.
	 * This operation counts nodes; it does not use {@link #numItems}.
	 * @param n reference to subtree in which to count nodes.
	 * @return number of nodes in subtree.
	 */
	private int count(Node<K,V> n) {
		if (n == null) return 0;
		return count(n.left)+ 1 + count(n.right); 
	}
	
	/**
	 * Check the invariant, printing a message if not satisfied.
	 * @return whether invariant is correct
	 */
	private boolean wellFormed() {
		if (comparator == null) return report("null comparator");
		if (!checkInRange(root,null,null)) return false;
		int count = count(root);
		if (count != numItems) return report("count wrong: " + numItems + ", should be " + count);
		return true;
	}
	
	
	/// constructors
	
	private TreeMap(boolean ignored) {} // do not change this.
	
	@SuppressWarnings({ "unchecked" })
	/**
	 * Create an empty tree map, assuming that the type is comparable.
	 * If it is not comparable, then errors will happen as pairs
	 * are added to the tree.
	 */
	public TreeMap() {
		comparator = new Comparator<K>() {
			public int compare(K arg0, K arg1) {
				return ((Comparable<K>)arg0).compareTo(arg1);
			}
		};
		assert wellFormed() : "invariant broken after constructor()";
	}
	
	/**
	 * Create an empty tree map using the given comparator.
	 * @param c comparator to order elements, must not be null
	 * @throws IllegalArgumentException if the comparator is null
	 */
	public TreeMap(Comparator<K> c) {
		if (c == null) throw new IllegalArgumentException("comparator must not be null");
		comparator = c;
		assert wellFormed() : "invariant broken after constructor(Comparator)";
	}

	// The following is a useful private method to check that the
	// given object is a key in the tree.  If it is not the correct type
	// (or if the tree is empty) this method returns null.
	@SuppressWarnings("unchecked")
	private K asKey(Object x) {
		if (root == null || x == null) return null;
		try {
			comparator.compare(root.key,(K)x);
			comparator.compare((K)x,root.key);
			return (K)x;
		} catch (ClassCastException ex) {
			return null;
		}
	}
	
	private Node<K,V> getNode(Object o) {
		K x = asKey(o);
		if (x == null) return null;
		Node<K,V> r = root;
		while (r != null) {
			int c = comparator.compare(x,r.key);
			if (c == 0) return r;
			if (c < 0) r = r.left;
			else r = r.right;
		}
		return null;
	}

	@Override // efficiency (and make independent of iterators)
	public boolean containsKey(Object o) {
		assert wellFormed() : "invariant broken at start of contains";
		return getNode(o) != null;
	}
	
	@Override // efficiency (and make independent of iterators)
	public V get(Object o) {
		assert wellFormed() : "invariant broken at start of get";
		Node<K,V> n = getNode(o);
		if (n == null) return null;
		return n.value;
	}
	
	
	
	/// mutation:

	/**
	 * Add a binding to the map.
	 * @param key to add, must not be null
	 * @throws NullPointerException if the key is null.
	 */
	@Override // implementation
	public V put(K key, V value) throws IllegalArgumentException {
		assert wellFormed() : "invariant broken at beginning of put";
		if (key == null) throw new NullPointerException("Cannot use null as a key");
		V res = do_put(root,key,value,null,false);
		assert wellFormed() : "invariant broken at end of put";
		return res;
	}

	private void connect(Node<K,V> parent, boolean isr, Node<K,V> child) {
		if (parent == null) root = child;
		else if (isr) parent.right = child;
		else parent.left = child;
	}
	
	private V do_put(Node<K,V> n, K key, V value, Node<K,V> parent, boolean isr) {
		if (n == null) {
			++numItems;
			n = new Node<K,V>(key,value);
			connect(parent,isr,n);
			++version;
			return null;
		}
		int c = comparator.compare(key, n.key);
		if (c == 0) {
			V res = n.value;
			n.value = value;
			return res;
		}
		if (c < 0) {
			return do_put(n.left,key,value,n,false);
		} else {
			return do_put(n.right,key,value,n,true);
		}
	}
	
	@Override // implementation
	public V remove(Object o) {
		assert wellFormed() : "invariant broken at beginning of remove";
		V result;
		K x = asKey(o);		
		if (x == null) result = null;
		else result = do_remove(root,x,null,false);
		assert wellFormed() : "invariant broken at end of remove";
		return result;
	}

	private V do_remove(Node<K,V> n, K key, Node<K,V> parent, boolean isr) {
		if (n == null)  return null;
		int c = comparator.compare(key, n.key);
		if (c == 0) {
			return do_remove_here(n, parent, isr);
		} else if (c < 0) {
			return do_remove(n.left,key,n, false);
		} else {
			return do_remove(n.right,key,n, true);
		}
	}

	private V do_remove_here(Node<K, V> n, Node<K, V> parent, boolean isr) {
		++version;
		if (n.left == null) {
			//System.out.println("Case A: " + n);
			connect(parent,isr,n.right);
			-- numItems;
			return n.value;
		}
		Node<K,V> t = n.left;
		if (t.right == null) {
			//System.out.println("Case B: " + n);
			connect(t,true,n.right);
			connect(parent, isr, t);
			--numItems;
			return n.value;
		} else {
			//System.out.println("Case C: " + n);
			V saved = n.value;
			Node<K,V> prev = null;
			while (t.right != null) {
				prev = t;
				t = t.right;
			}
			n.key = t.key;
			n.value = do_remove_here(t,prev,true);
			return saved;
		}
	}

	private volatile Set<Entry<K,V>> entrySet;
	
	@Override // required
	public Set<Entry<K, V>> entrySet() {
		assert wellFormed() : "invariant broken at beginning of entrySet";
		if (entrySet == null) {
			entrySet = new EntrySet();
		}
		return entrySet;
	}

	/**
	 * The "backing" set for this map.
	 * In other words, this set doesn't have its own data structure:
	 * it uses the data structure of the map.
	 */
	private class EntrySet extends AbstractSet<Entry<K,V>> {

		@Override // required
		public int size() {
			assert wellFormed() : "invariant broken in size()";
			return numItems;
		}

		@Override // required
		public Iterator<Entry<K, V>> iterator() {
			assert wellFormed() : "invariant broken in iterator()";
			return new MyIterator();
		}
		
		// You don't need to override "add".  Why not?
		
		@Override // efficiency
		public boolean contains(Object o) {
			assert wellFormed() : "Invariant broken at start of EntrySet.contains";
			if (!(o instanceof Entry<?,?>)) return false;
			Entry<?,?> e = (Entry<?,?>)o;
			Node<K,V> node = getNode(e.getKey());
			if (node == null) return false;
			return node.equals(o);
		}

		@Override // efficiency
		public boolean remove(Object x) {
			assert wellFormed() : "Invariant broken at start of EntrySet.remove";
			if (!contains(x)) return false;
			TreeMap.this.remove(((Entry<?,?>)x).getKey());
			// the following check is redundant because TreeMap.remove already called it: 
			assert wellFormed() : "Invariant broken at end of EntrySet.remove";
			return true;
		}
				
		@Override // efficiency, also to make independent of "remove"
		public void clear() {
			assert wellFormed() : "invariant broken at beginning of clear";
			if (root == null) return;
			root = null;
			numItems = 0;
			++version;
			assert wellFormed() : "invariant broken at end of clear";
		}
	}

	
	/**
	 * Iterator over the map.
	 * We use a stack of nodes.
	 */
	private class MyIterator implements Iterator<Entry<K,V>> {
		final Stack<Node<K,V>> pending = new Stack<Node<K,V>>();
		Node<K,V> current = null;
		int myVersion = version;
		
		boolean wellFormed() {
			if (!TreeMap.this.wellFormed()) return false;
			if (version != myVersion) return true;
			if (pending == null) return report("pending is null");
			Node<K,V> node = root;
			Node<K,V> pre = null;
			for (Node<K,V> n : pending) {
				if (n == null) return report("null in stack");
				while (n != node) {
					if (node == null) return report("unexpected node in stack: " + n);
					pre = node;
					node = node.right;
				}
				node = node.left;
			}
			if (current != null) {
				if (node == null) {
					if (current != pre) return report("current is " + current + ", but expected " + pre);
				} else {
					while (node.right != null) node = node.right;
					if (current != node) return report("current is " + current + ", not " + node);
				}
			}
			return true;
		}
		
		MyIterator() {
			Node<K,V> n = root;
			while (n != null) {
				pending.push(n);
				n = n.left;
			}
			assert wellFormed() : "invariant broken after iterator constructor";
		}
		
		public void checkVersion() {
			if (version != myVersion) throw new ConcurrentModificationException("stale iterator");
		}
		
		@Override // required
		public boolean hasNext() {
			assert wellFormed() : "invariant broken before hasNext()";
			checkVersion();
			return !pending.isEmpty();
		}

		@Override // required
		public Entry<K, V> next() {
			assert wellFormed() : "invariant broken at start of next()";
			if (!hasNext()) throw new NoSuchElementException("at end of map");
			current = pending.pop();
			for (Node<K,V> n = current.right; n != null; n = n.left) {
				pending.push(n);
			}
			assert wellFormed() : "invariant broken at end of next()";
			return current; 
		}

		@Override // implementation
		public void remove() {
			assert wellFormed() : "invariant broken at start of iterator.remove()";
			checkVersion();
			if (current == null) throw new IllegalStateException("cannot remove now");
			TreeMap.this.remove(current.key);
			current = null;
			myVersion = ++version;
			assert wellFormed() : "invariant broken at end of iterator.remove()";
		}
		
	}
	
	
	/// MergeSort Assignment
	
	// In order to handle adding large numbers of entries
	// at once, we will use a system of converting trees to
	// sorted cyclic doubly linked lists, sorting unsorted lists, and 
	// merging sorted lists.  We will use the existing Nodes and interpret
	// "left" as "prev" and "right" as "next".
	
	/**
	 * Return true if the given head is either null (empty) or
	 * a cyclic DLL list.  No null values are permitted as keys.
	 * If there is a problem, it should be reported.
	 * @param head start of supposed cyclic DLL, or null
	 * @return true if it is indeed a well-formed cyclic DLL without null keys.
	 */
	private boolean wellFormed(Node<K,V> head) {
		// TODO: loop permitted
		if (head == null)
			return true;
		
		if (head.right == null || head.left == null)
			return report("List isnt cyclic");
		
		Node<K,V> lag = head.left;
		
		// this while loop refused to work so I changed it to a for loop and it magically worked after I
		// added the break point in the loop.
		//while (node != null) 
		for (Node<K,V> node = head; head != null;)
		{
			// checks for null nodes
			if (node.key == null || lag.key == null)
				return report("Key " + node.key + " cant be null");
			
			// checks for bad pointers between nodes
			if (lag.right != node || node.left != lag)
				return report("bad pointer found betweem " + lag + node);
			
			if (node == head.left)
				break;
			
			lag = lag.right;
			node = node.right;
		} 
		
		return true;
	}
	
	/**
	 * Return true if the given head of a cyclic DLL is strictly sorted
	 * in ascending order according to the comparator (that is, without duplicates).
	 * The list is assumed to be well formed.  This method
	 * works as an invariant check: it calls doReport with if problems are found.
	 * @param head start of a cyclic DLL list (possibly null)
	 * @return whether the list is sorted.  If false is returned, a problem will have been reported.
	 */
	private boolean isSorted(Node<K,V> head) {
		if (!wellFormed(head)) throw new IllegalArgumentException("not a wellFormed cyclic DLL");
		// TODO: loop permitted
		if (comparator == null)
			return report("comparator is null");
		
		if (head == null)
			return true;
		
		Node<K,V> n = head;
		while (n != null)
		{
			if (n == head.left)
				break;
			
			if (comparator.compare(n.key, n.right.key) >= 0)
				return report("list isnt correctly sorted");
			
			n = n.right;
		}
		
		return true;
	}
	
	/**
	 * Count the length of a cyclic DLL.
	 * The list is unchanged.
	 * @param l a possibly empty cyclic DLL.
	 * @return number of nodes in the cycle
	 */
	private int length(Node<K,V> l) {
		assert wellFormed(l);
		int count = 1;
		// TODO: loop permitted
		if (l == null)
			return 0;
		
		Node<K,V> n = l;
		
		// iterate through until you reach the end counting up each time
		while (n != l.left)
		{
			++count;
			n = n.right;
		}
		
		return count;
	}
	
	/**
	 * Add a node to the end of a possibly empty DLL cyclic list.
	 * @param l possibly empty cyclic DLL to add to
	 * @param n node to add (not null), must not be in the list already
	 * @return list with new element added to end
	 */
	private Node<K,V> add(Node<K,V> l, Node<K,V> n) {
		assert wellFormed(l);
		// TODO: One "if" only, no loops
		if (l == null)
		{
			// since l is null, l = n then points to itself
			l = n;
			l.right = l;
			l.left = l;
		}
		
		else
		{
			// sets n at the end of the linked list
			n.left = l.left;
			l.left.right = n;
			
			// connects the head to the end and visa versa
			n.right = l;
			l.left = n;
		}
		
		assert wellFormed(l);
		return l;
	}
	
	/**
	 * Add the second list to the end of the first list.
	 * @param l1 possibly empty cyclic DLL to append to
	 * @param l2 possibly empty cyclic DLL to append to first list
	 * @return appended list
	 */
	private Node<K,V> append(Node<K,V> l1, Node<K,V> l2) {
		assert wellFormed(l1) && wellFormed(l2);
		// TODO: Two "if"'s, no loops
		
		// these 2 counting variables used to get length more efficiently
		int count1 = length(l1);
		int count2 = length(l2);
		
		if (count1 == 0 && count2 != 0)
			return l2;
		
		if (count1 != 0 && count2 == 0)
			return l1;
		
		/*
		 * Bennet helped me with this method, he helped straighten out my logic
		 */
		
		// temp variable to keep access to end that would have otherwise 
		// been lost
		Node<K,V> l1end = l1.left;
		
		// set outer pointers
		l1.left = l2.left;
		l2.left.right = l1;
		
		// sets inner pointers
		l2.left = l1end;
		l1end.right = l2;
		
		assert wellFormed(l1);
		return l1;
	}
	
	/**
	 * Convert a subtree rooted at r into a sorted cyclic DLL ith all the same nodes.
	 * All the old nodes are re-purposed as DLL nodes.
	 * @param r subtree, may be null
	 * @return resulting sorted cyclic DLL, may be null
	 */
	private Node<K,V> toList(Node<K,V> r) {
		assert checkInRange(r, null, null);
		Node<K,V> result = r;
		// TODO: use recursion
		
		/*
		 * Luke helped guide me in the for this method, I did not discuss any logic or code with
		 * any other students
		 */
		
		// base case of r == root and there's only root
		if (r == null) 
			return null;
		
		// adds node with no children
		if (r.left == null && r.right == null)
			return add(null, r);
		
		// recurses through each tree
		Node<K,V> leftTree = toList(r.left);
		Node<K,V> rightTree = toList(r.right);
		
		// adds root to the end of the left tree
		leftTree = add(leftTree, r);
		
		// appends the 2 lists together
		result = append(leftTree, rightTree);
		
		assert wellFormed(result) && isSorted(result);
		return result;
	}
	
	/**
	 * Split a cyclic DLL into two segments; 
	 * the first n elements remaining in the original
	 * list and the remaining elements in a possibly empty new cyclic DLL
	 * that is returned
	 * @param l non-empty cyclic list
	 * @param n positive length of elements to keep, must be less or equal to the length of n
	 * @return possibly empty list of remaining elements
	 */
	private Node<K,V> split(Node<K,V> l, int n) {
		assert wellFormed(l) && length(l) >= n;
		if (n <= 0) throw new IllegalArgumentException("split needs to take a positive number");
		Node<K,V> result = l;
		// TODO: loop n times permitted (split(_,1) should be constant time)
		int size = length(l);
		
		if (l == null)
			return null;
		
		// nothing to split
		if (size == 1)
			return null;
		
		// n is bigger than the size of the list
		if (size <= n)
			return null;
		
		// lag helps us set the value of the end of the first list
		Node<K,V> lag = null;
		
		// finds where to split
		for (int i = 0; i < n; ++i)
		{
			lag = result;
			result = result.right;
		}
		
		// lets new lists pointers
		result.left = l.left;
		result.left.right = result;
		
		// sets pointers at the end of the first list
		l.left = lag;
		lag.right = l;
		
		assert wellFormed(l) && wellFormed(result) && length(l) == n;
		return result;
	}
	
	private Node<K,V> merge(Node<K,V> l1, Node<K,V> l2) {
        assert wellFormed(l1) && isSorted(l1);
        assert wellFormed(l2) && isSorted(l2);
        Node<K,V> result = null;
        // TODO: loop permitted
        if (l1 == null && l2 == null)
            return null;

        if (l1 == null && l2 != null)
            return l2;

        if (l1 != null && l2 == null)
            return l1;

        // temp variable to help with pointers
        Node<K,V> temp = null;

        // counter variables to get the lengths of each list
        int count1 = length(l1);
        int count2 = length(l2);

        // counter variables to check against the length variables to break out
        // of the loop
        int i1 = 0;
        int i2 = 0;

        while(i1 < count1 && i2 < count2)
        {
            if (comparator.compare(l1.key, l2.key) == 0)
            {
                // discard the node from l1
                temp = l1;

                l1 = l1.right;

                // set pointers back to itself so it doesn't get thrown away by java
                temp.right = temp;
                temp.left = temp;
                
                ++i1;
            }

            else if (comparator.compare(l1.key, l2.key) < 0)
            {
                // add node from l1 to result
            	Node<K,V> x = l1.right;
                result = add(result, l1);
                l1 = x;
                
                ++i1;
            }

            else
            {
                // add node from l2 to result
            	Node<K,V> x = l2.right;
                result = add(result, l2);
                l2 = x;
                
                ++i2;
            }
        }

        // adds the remaining nodes of a list when one is completely added
        // I also set the temp variables because the pointers werent updating
        if (i1 == count1)
        {
        	while(i2 < count2)
        	{
            	Node<K,V> x = l2.right;
                result = add(result, l2);
                l2 = x;
                
                ++i2;
        	}
        }
        
        else
        {
        	while(i1 < count1)
        	{
            	Node<K,V> x = l1.right;
                result = add(result, l1);
                l1 = x;
                
                ++i1;
        	}
        }

        assert wellFormed(result) && isSorted(result);
        return result;
    }
	
	/**
	 * Sort the given cyclic DLL list and return the result.
	 * @param l a possibly empty cyclic DLL
	 * @param size length of this list.
	 * @return sorted cyclic list
	 */
	private Node<K,V> sort(Node<K,V> l, int size) {
		assert wellFormed(l) && length(l) == size;
		Node<K,V> result = l;
		// TODO: merge sort: use recursion
		if (l == null)
			return null;
		
		if (size <= 1)
			return l;
		
		// gets the mid point to split
		int mid = size/2;
		
		// finds the 2nd half of the list
		Node<K,V> last = split(result, mid);
		
		// split the current list into 2 parts and recursively go down
		Node<K,V> firstHalf = sort(result, mid);
        Node<K,V> secondHalf = sort(last, size - mid);
        
        // combine and sort the 2 halves
        result = merge(firstHalf, secondHalf);
		
		assert wellFormed(result) && isSorted(result);
		return result;
	}
	
	/**
	 * Convert a sorted cyclic DLL to a balanced BST
	 * using the existing nodes.
	 * @param l possibly empty cyclic DLL
	 * @param size number of elements in the cyclic DLL
	 * @return well formed tree of all nodes formerly in cyclic DLL.
	 */
	private Node<K,V> toTree(Node<K,V> l, int size) {
		assert wellFormed(l) && isSorted(l) && length(l) == size;
		Node<K,V> r = l;
		// TODO: use recursion
		
		// break out conditions
		if (l == null)
			return null;
		
		// when the size is either 1 or 0, you set both of its child pointers to null
		// since there is nothing there
		if (size <= 1)
		{
			l.left = null;
			l.right = null;
			return l;
		}
		
		// Luke the tutor suggested that I check for this case. In this case you put the
		// first element as the root then the next one is on the right
		if (size == 2)
		{
			r.right.left = null;
			r.left.right = null;
			r.left = null;
			return r;
		}
		
		// to find the middle you add the one after diving to prevent the number from becoming negative
		int mid = size/2 + 1;
		
		// once you split the right tree you can call split again on the left tree because it has already been split
		// and the -1 will isolate the node that you are going to add the children to
		Node<K,V> rightTree = split(l, mid);
		Node<K,V> midNode = split(l, mid - 1);
		
		// middle node to make tree balanced
		r = midNode;
		
		// recursively branches out and adds the children 
		r.left = toTree(l, mid-1);
		r.right = toTree(rightTree, size - mid);
		
		assert checkInRange(r,null,null) && count(r) == size;
		return r;
	}
	
	@Override // efficiency
	public void putAll(Map<? extends K, ? extends V> m) {
		assert wellFormed() : "invariant broken before putAll";
		if (m.size() <= this.size()) {
			super.putAll(m); // more efficient to simply put one by one
			return; 
		}
		// TODO
		// 1. Create a cyclic DLL of entries from m
		
		// head of the list being created
		Node<K,V> n = null;
		
		for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
		{	
			Node<K,V> p = new Node<K,V>(e.getKey(), e.getValue());
				
			// check if it is null so you can update the head pointer
			if (n == null)
				n = add(n, p);
			// otherwise just add it to the list
			add(n, p);
		}
		
		// 2. Sort the new list
		n = sort(n, m.size());
		
		// 3. Convert existing tree to a (sorted) list
		Node<K,V> treeToList = toList(root);
		
		// 4. Merge the two lists and convert the result back to a tree
		Node<K,V> result = merge(n,treeToList);
		numItems = length(result); // This resulted in a couple hours of troubleshooting because I didnt have this...
		
		// Once tree is formed, must update the head
		result = toTree(result, numItems);
		root = result;
		
		// 5. increment version (our iterator cannot cope with possibly new nodes
		//    even if the size didn't change.)
		++version;
		
		assert wellFormed() : "invariant broken in putAll";
	}
	
	
	
	/// Junit test case of private internal structure.

	public static class TestSuite extends LockedTestCase {
		private Comparator<Integer> normal = new Comparator<Integer>() {
			public int compare(Integer arg0, Integer arg1) {
				return arg0 - arg1;
			}
		};
		private Comparator<Integer> backward = new Comparator<Integer>() {
			public int compare(Integer arg0, Integer arg1) {
				return arg1 - arg0;
			}
		};
		private Comparator<Integer> strange = new Comparator<Integer>() {
			public int compare(Integer arg0, Integer arg1) {
				return 0;
			}
		};
		private TreeMap<Integer,String> tree;
		private Node<Integer,String> n1, n2, n3, n4, n5, n3a, n3b;
		private Node<Integer,String> n6, n7, n8, n9;
		
		protected static final String THREE_ORIGINAL = "three";
		protected static final String THREE_CHANGED = "not three";
		
		@Override
		protected void setUp() {
			tree = new TreeMap<Integer,String>(false);
			tree.root = null;
			tree.numItems = 0;
			n1 = new Node<Integer,String>(1,"one");
			n2 = new Node<Integer,String>(2,"two");
			n3 = new Node<Integer,String>(3,THREE_ORIGINAL);
			n4 = new Node<Integer,String>(4,"four");
			n5 = new Node<Integer,String>(5,"five");
			n3a = new Node<Integer,String>(3,THREE_ORIGINAL);
			n3b = new Node<Integer,String>(3,THREE_CHANGED);
			n6 = new Node<Integer,String>(6,"six");
			n7 = new Node<Integer,String>(7,"seven");
			n8 = new Node<Integer,String>(8,"eight");
			n9 = new Node<Integer,String>(9,"nine");
		}
	
		public <K,V> void assertWellFormed(boolean expected, Node<Integer,String> l) {
			doReport = expected;
			assertEquals(expected, tree.wellFormed(l));
			doReport = true;
		}
		
		public void assertIsSorted(boolean expected, Node<Integer,String> l) {
			doReport = expected;
			assertEquals(expected, tree.isSorted(l));
			doReport = true;
		}
		
		protected static final int ERROR = -1;
		protected static final int NONE_ABOVE = -2;
		
		protected int ix(Supplier<Node<Integer,String>> s) {
			try {
				Node<Integer,String> n = s.get();
				if (n == null) return 0;
				if (n == n1) return 1;
				else if (n == n2) return 2;
				else if (n == n3) return 3;
				else if (n == n4) return 4;
				else if (n == n5) return 5;
				else if (n == n6) return 6;
				else if (n == n7) return 7;
				else if (n == n8) return 8;
				else if (n == n9) return 9;
			} catch (RuntimeException ex) {
				return ERROR;
			}
			return NONE_ABOVE;
		}
		
		
		/// Locked tests
		
		public void test() {
			n1.left = n1.right = null;
			n2.left = n2.right = n2;
			n4.left = n4.right = n8;
			n8.left = n8.right = n4;
			
			int total = 0; doReport = false;
			if (tree.wellFormed(n1)) total += 1;
			if (tree.wellFormed(n2)) total += 2;
			if (tree.wellFormed(n4)) total += 4;
			if (tree.wellFormed(n8)) total += 8;
			// total is the sum of numbers of all wellFormed cyclic DLLs
			assertEquals(Ti(896639901), total); // which nodes are valid cyclic DLLs ?
			
			// we adjust n1
			n1.left = n1.right = n3;
			n3.left = n3.right = n1;
			total = 0;
			tree.comparator = normal; // 1 < 2 < 3 < 4 < ... < 8
			if (tree.isSorted(n1)) total += 1;
			if (tree.isSorted(n2)) total += 2;
			if (tree.isSorted(n4)) total += 4;
			if (tree.isSorted(n8)) total += 8;
			// total is the sum of numbers of all sorted cyclic DLLs
			assertEquals(Ti(2051810374), total); // which nodes are sorted cyclic DLLs ?
			doReport = true;
			test1(false);
		}
		
		private void test1(boolean ignored) {
			assertEquals(Tb(661557853), tree.wellFormed(null));
			tree.comparator = backward;
			assertEquals(Tb(1428343703), tree.isSorted(null));
			test2(false);
		}
		
		private void test2(boolean ignored) {
			n9.right = n8; n8.left = n9;
			n8.right = n7; n7.left = n8;
			n7.right = n6; n6.left = n7;
			n6.right = n9; n9.left = n6;
			tree.comparator = backward;
			assertTrue(tree.wellFormed(n9));
			assertTrue(tree.isSorted(n9));
			
			// What does "split" do ?
			// Use 6 for n6, 7 for n7, 8 for n8, 9 for n9
			// Use 0 for null, -1 for error, or -2 for "none of the above"
			assertEquals(Ti(376688604), ix(() -> tree.split(n9, 0))); 
			assertEquals(Ti(490156049), ix(() -> tree.split(n9, 4))); 
			// n9 hasn't been changed (yet)
			assertEquals(Ti(583090247), ix(() -> tree.split(n9, 3)));
			assertEquals(Ti(1874812294), tree.length(n9));
		}
		
		public void test3() {
			tree.comparator = normal;
			n3.left = n2;
			n3.right = n5;
			assertTrue(tree.checkInRange(n3,null,null));
			
			// What does "toList" do ?
			// Use 1 for n1, 2 for n2, 3 for n3, 4 for n4, 5 for n5
			// Use 0 for null, -1 for error, or -2 for "none of the above"
			assertEquals(0, ix(() -> tree.toList(null)));
			assertEquals(2, ix(() -> tree.toList(n3)));
			assertEquals(3, tree.length(n2));
			
			test4(false);
		}
		
		private void test4(boolean ignored) {
			n1.left = n1.right = n3;
			n3.left = n3.right = n1;
			
			n2.left = n2.right = n3b; // n3b has same key as n3, but different value
			n3b.left = n3b.right = n2;
			
			tree.comparator = normal;
			assertTrue(tree.wellFormed(n1) && tree.isSorted(n1));
			assertTrue(tree.wellFormed(n2) && tree.isSorted(n2));
			assertEquals(2, tree.length(n1));
			assertEquals(2, tree.length(n2));
			
			Node<Integer,String> l = tree.merge(n1, n2);
			assertEquals(Ti(1198622474), tree.length(l));
			// Use 1 for n1, 2 for n2, 3 for n3, 4 for n4, 5 for n5
			// Use 0 for null, -1 for error, or -2 for "none of the above"
			assertEquals(Ti(450094719), ix(() -> l));
			assertEquals(Ti(1666424655), ix(() -> l.right));
			assertEquals(Ti(1083913735), ix(() -> l.left));
		}
		
		/// test 0x: tests of wellFormed(l)
		
		public void test00() {
			Node<Integer,String> n = null;
			assertWellFormed(true, n);
			tree.root = n1; // should be irrelevant
			assertWellFormed(true, n);
		}
		
		public void test01() {
			n2.left = n2.right = n2;
			assertWellFormed(true, n2);
			tree.root = n2; // irrelevant
			assertWellFormed(true, n2);
			tree.root = n1; // irrelevant
			assertWellFormed(true, n2);
		}
		
		public void test02() {
			assertWellFormed(false,(n3));
			n3.left = n3; n3.right = null;
			assertWellFormed(false,(n3));
			n3.left = null; n3.right = n3;
			tree.root = n3; // irrelevant
			assertWellFormed(false,(n3));
		}
		
		public void test03() {
			n4.left = n5; 
			n4.right = n5;
			n5.left = n4;
			n5.right = n4;
			assertWellFormed(true, n4);
			assertWellFormed(true, n5);
			tree.root = n3b; // irrelevant
			assertWellFormed(true, n4);
			tree.root = n4; // irrelevant
			assertWellFormed(true, n4);
			assertWellFormed(true, n5);
		}
		
		public void test04() {
			n3.left = n3;
			n3.right = n3a;
			n3a.left = n3;
			n3a.right = n3;
			assertWellFormed(false,(n3));
			assertWellFormed(false,(n3a));
			n3a.right = n3a;
			assertWellFormed(false,(n3));
			assertWellFormed(false,(n3a));
			n3.right = n3;
			assertWellFormed(false,(n3a));
		}
		
		public void test05() {
			n1.right = n2; n2.right = n3; n3.right = n1;
			n1.left = n3; n3.left = n2; n2.left = n1;
			assertWellFormed(true, n1);
			assertWellFormed(true, n2);
			assertWellFormed(true, n3);
			tree.root = n3b; // irrelevant
			assertWellFormed(true, n1);
			tree.root = n1; // irrelevant
			assertWellFormed(true, n1);
			assertWellFormed(true, n2);
			assertWellFormed(true, n3);
		}
		
		public void test06() {
			n1.right = n2; n2.right = n3; n3.right = n1;
			n1.left = n3; n3.left = n2; n2.left = n1;
			n3a.left = n2; n3a.right = n1;
			assertWellFormed(true, n1);
			n1.left = n3a;
			assertWellFormed(false,(n1));
			assertWellFormed(false,(n2));
			assertWellFormed(false,(n3));
			assertWellFormed(false,(n3a));
		}
		
		public void test07() {
			n1.right = n2; n2.right = n3; n3.right = n1;
			n1.left = n3; n3.left = n2; n2.left = n1;
			assertWellFormed(true, n1);
			n3.right = n3;
			assertWellFormed(false,(n1));
			assertWellFormed(false,(n2));
			assertWellFormed(false,(n3));
			n3.right = n2;
			assertWellFormed(false,(n1));
			assertWellFormed(false,(n2));
			assertWellFormed(false,(n3));
		}
		
		public void test08() {
			n1.left = n2; n1.right = n5;
			n2.left = n3; n2.right = n1;
			n3.left = n4; n3.right = n2;
			n4.left = n5; n4.right = n3;
			n5.left = n1; n5.right = n4;
			assertWellFormed(true, n1);
			assertWellFormed(true, n2);
			assertWellFormed(true, n3);
			assertWellFormed(true, n4);
			assertWellFormed(true, n5);
			tree.root = n3b; // irrelevant
			assertWellFormed(true, n1);
			assertWellFormed(true, n2);
			assertWellFormed(true, n3);
			assertWellFormed(true, n4);
			assertWellFormed(true, n5);
			tree.root = n1; // irrelevant
			assertWellFormed(true, n1);
			assertWellFormed(true, n2);
			assertWellFormed(true, n3);
			assertWellFormed(true, n4);
			assertWellFormed(true, n5);
		}
		
		public void test09() {
			n1.left = n2; n1.right = n5;
			n2.left = n3; n2.right = n1;
			n3.left = n4; n3.right = n2;
			n4.left = n5; n4.right = n3;
			n5.left = n1; n5.right = n4;
			n3a.left = n4;
			n3a.right = n2;
			assertWellFormed(true, n1);
			n2.left = n3a;
			assertWellFormed(false,(n1));
			assertWellFormed(false,(n2));
			assertWellFormed(false,(n3));
			assertWellFormed(false,(n4));
			assertWellFormed(false,(n5));
			assertWellFormed(false,(n3a));
			n2.left = n3;
			n4.right = n3a;
			assertWellFormed(false,(n1));
			assertWellFormed(false,(n2));
			assertWellFormed(false,(n3));
			assertWellFormed(false,(n4));
			assertWellFormed(false,(n5));
			assertWellFormed(false,(n3a));	
			n2.left = n3a;
			n4.right = n3a;
			tree.root = n5; // irrelevant
			assertWellFormed(true, n1);
			assertWellFormed(true, n2);
			assertWellFormed(true, n3a);
			assertWellFormed(true, n4);
			assertWellFormed(true, n5);
			assertWellFormed(false,(n3));
		}
		
		
		/// test1x: tests of isSorted
		
		public void test10() {
			tree.comparator = null;
			assertIsSorted(false,(null));
			tree.comparator = normal;
			assertIsSorted(true, null);
			tree.comparator = backward;
			assertIsSorted(true, null);
			tree.comparator = strange;
			assertIsSorted(true, null);
		}
		
		public void test11() {
			n1.left = n1.right = n1;
			tree.comparator = null;
			assertIsSorted(false,(n1));
			tree.comparator = normal;
			assertIsSorted(true, n1);
			tree.comparator = backward;
			assertIsSorted(true, n1);
			tree.comparator = strange;
			assertIsSorted(true, n1);
		}
		
		public void test12() {
			n1.left = n2; n1.right = n2;
			n2.left = n1; n2.right = n1;
			tree.comparator = normal;
			assertIsSorted(true, n1);
			tree.comparator = backward;
			assertIsSorted(true, n2);
		}
		
		public void test13() {
			n1.left = n2; n1.right = n2;
			n2.left = n1; n2.right = n1;
			tree.comparator = null;
			assertIsSorted(false,(n1));
			tree.comparator = normal;
			assertIsSorted(false,(n2));
			tree.comparator = backward;
			assertIsSorted(false,(n1));
			tree.comparator = strange;
			assertIsSorted(false,(n1));
			assertIsSorted(false,(n2));
		}
		
		public void test14() {
			n1.left = n3; n1.right = n2;
			n2.left = n1; n2.right = n3;
			n3.left = n2; n3.right = n1;
			tree.comparator = normal;
			assertIsSorted(true, n1);			
		}
		
		public void test15() {
			n1.left = n3; n1.right = n2;
			n2.left = n1; n2.right = n3;
			n3.left = n2; n3.right = n1;
			tree.comparator = null;
			assertIsSorted(false,(n1));
			assertIsSorted(false,(n2));
			assertIsSorted(false,(n3));
			tree.comparator = normal;
			assertIsSorted(false,(n2));
			assertIsSorted(false,(n3));
			tree.comparator = backward;
			assertIsSorted(false,(n1));
			assertIsSorted(false,(n2));
			assertIsSorted(false,(n3));
			tree.comparator = strange;
			assertIsSorted(false,(n1));
			assertIsSorted(false,(n2));
			assertIsSorted(false,(n3));
		}
		
		public void test16() {
			n1.left = n2; n1.right = n5;
			n2.left = n3; n2.right = n1;
			n3.left = n4; n3.right = n2;
			n4.left = n5; n4.right = n3;
			n5.left = n1; n5.right = n4;
			tree.comparator = backward;
			assertIsSorted(true, n5);
		}
		
		public void test17() {
			n1.left = n2; n1.right = n5;
			n2.left = n3; n2.right = n1;
			n3.left = n4; n3.right = n2;
			n4.left = n5; n4.right = n3;
			n5.left = n1; n5.right = n4;
			tree.comparator = null;
			assertIsSorted(false,(n1));
			assertIsSorted(false,(n2));
			assertIsSorted(false,(n3));
			assertIsSorted(false,(n4));
			assertIsSorted(false,(n5));
			tree.comparator = normal;
			assertIsSorted(false,(n1));
			assertIsSorted(false,(n2));
			assertIsSorted(false,(n3));
			assertIsSorted(false,(n4));
			assertIsSorted(false,(n5));
			tree.comparator = backward;
			assertIsSorted(false,(n1));
			assertIsSorted(false,(n2));
			assertIsSorted(false,(n3));
			assertIsSorted(false,(n4));
			tree.comparator = strange;
			assertIsSorted(false,(n1));
			assertIsSorted(false,(n2));
			assertIsSorted(false,(n3));
			assertIsSorted(false,(n4));
			assertIsSorted(false,(n5));
		}
		
		public void test18() {
			n1.right = n2; n1.left = n4;
			n2.right = n3; n2.left = n1;
			n3.right = n5; n3.left = n2;
			n5.right = n4; n5.left = n3;
			n4.right = n1; n4.left = n5;	
			tree.comparator = normal;
			assertIsSorted(false,(n1));
		}
		
		public void test19() {
			n1.right = n2; n1.left = n5;
			n2.right = n4; n2.left = n1;
			n4.right = n3; n4.left = n2;
			n3.right = n5; n3.left = n4;
			n5.right = n1; n5.left = n3;	
			tree.comparator = normal;	
			assertIsSorted(false,(n1));
		}
		
		
		/// test 2x: test of length
		
		public void test20() {
			assertEquals(0, tree.length(null));
			tree.root = n1; // irrelevant
			tree.numItems = 17; // irrelevant
			tree.comparator = normal; // irrelevant
			assertEquals(0, tree.length(null));
		}
		
		public void test21() {
			n1.left = n1.right = n1;
			assertEquals(1, tree.length(n1));
			tree.root = n1; // irrelevant
			tree.numItems = 17; // irrelevant
			tree.comparator = normal; // irrelevant
			assertEquals(1, tree.length(n1));
			tree.root = n2; // irrelevant
			assertEquals(1, tree.length(n1));
		}
		
		public void test22() {
			n1.left = n2; n1.right = n2;
			n2.left = n1; n2.right = n1;
			assertEquals(2, tree.length(n1));
			tree.root = n1; // irrelevant
			tree.numItems = 1; // irrelevant
			tree.comparator = normal; // irrelevant
			assertEquals(2, tree.length(n2));
		}
		
		public void test23() {
			n1.left = n3; n3.right= n1;
			n3.left = n2; n2.right = n3;
			n2.left = n1; n1.right = n2;
			assertEquals(3, tree.length(n1));
			assertEquals(3, tree.length(n2));
			assertEquals(3, tree.length(n3));
		}
		
		public void test24() {
			n1.right = n3; n3.left = n1;
			n3.right = n2; n2.left = n3;
			n2.right = n4; n4.left = n2;
			n4.right = n1; n1.left = n4;
			assertEquals(4, tree.length(n1));
			assertEquals(4, tree.length(n2));
			assertEquals(4, tree.length(n3));
			assertEquals(4, tree.length(n4));			
		}
		
		public void test25() {
			n1.right = n2; n1.left = n4;
			n2.right = n3; n2.left = n1;
			n3.right = n5; n3.left = n2;
			n5.right = n4; n5.left = n3;
			n4.right = n1; n4.left = n5;	
			tree.root = n1;
			tree.comparator = normal;
			assertEquals(5, tree.length(n1));
			assertEquals(5, tree.length(n2));
			assertEquals(5, tree.length(n3));
			assertEquals(5, tree.length(n4));			
			assertEquals(5, tree.length(n5));			
		}
		
		public void test26() {
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n3a; n3a.left = n3;
			n3a.right = n4; n4.left = n3a;
			n4.right = n5; n5.left = n4;
			n5.right = n1; n1.left = n5;
			assertEquals(6, tree.length(n1));
			assertEquals(6, tree.length(n2));
			assertEquals(6, tree.length(n3));
			assertEquals(6, tree.length(n4));			
			assertEquals(6, tree.length(n5));	
			assertEquals(6, tree.length(n3a));
		}
		
		public void test27() {
			n1.left = n2; n2.right = n1;
			n2.left = n3; n3.right = n2;
			n3.left = n3a; n3a.right = n3;
			n3a.left = n3b; n3b.right = n3a;
			n3b.left = n4; n4.right = n3b;
			n4.left = n5; n5.right = n4;
			n5.left = n1; n1.right = n5;
			assertEquals(7, tree.length(n1));
			assertEquals(7, tree.length(n2));
			assertEquals(7, tree.length(n3));
			assertEquals(7, tree.length(n4));			
			assertEquals(7, tree.length(n5));	
			assertEquals(7, tree.length(n3a));
			assertEquals(7, tree.length(n3b));			
		}
		
		
		/// test3x: tests of add
		
		public void test30() {
			Node<Integer,String> l = tree.add(null, n1);
			assertEquals(1, tree.length(l));
		}
		
		public void test31() {
			Node<Integer,String> l = tree.add(null, n2);
			assertSame(l, n2);
		}
		
		public void test32() {
			n4.left = n3;
			n4.right = n5;
			Node<Integer,String> l = tree.add(null, n4);
			assertEquals(1, tree.length(l));
			assertSame(l, n4);
		}
		
		public void test33() {
			n1.left = n1.right = n1;
			Node<Integer,String> l = tree.add(n1, n2);
			assertEquals(2, tree.length(l));
		}
		
		public void test34() {
			n3.left = n3.right = n3;
			n4.left = n3; n4.right = n5;
			Node<Integer,String> l = tree.add(n3, n4);
			assertSame(l, n3);
			assertSame(n4, l.right);
			assertSame(l, l.right.right);
		}
		
		public void test35() {
			n1.left = n2; n1.right = n2;
			n2.left = n1; n2.right = n1;
			Node<Integer,String> l = tree.add(n1, n3);
			assertEquals(3, tree.length(l));
		}
		
		public void test36() {
			n3.left = n4; n3.right = n4;
			n4.left = n3; n4.right = n3;
			n5.left = n3a;
			tree.root = n3; // irrelevant
			tree.comparator = normal; // irrelevant
			tree.numItems = 2; // irrelevant
			Node<Integer,String> l = tree.add(n3, n5);
			assertSame(n3, l);
			assertSame(n4, l.right);
			assertSame(n5, l.right.right);
			assertSame(l, l.right.right.right);
		}
		
		public void test37() {
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n1; n1.left = n3;
			Node<Integer,String> l = tree.add(n1, n4);
			assertSame(n1, l);
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			assertSame(n1, n4.right);
		}
		
		public void test38() {
			n1.right = n3; n3.left = n1;
			n3.right = n2; n2.left = n3;
			n2.right = n4; n4.left = n2;
			n4.right = n1; n1.left = n4;
			Node<Integer,String> l = tree.add(n1, n5);
			assertSame(n1, l);
			assertSame(n3, n1.right);
			assertSame(n2, n3.right);
			assertSame(n4, n2.right);
			assertSame(n5, n4.right);
			assertSame(n1, n5.right);
		}
		
		public void test39() {
			n5.right = n4; n4.left = n5;
			n4.right = n3; n3.left = n4;
			n3.right = n2; n2.left = n3;
			n2.right = n1; n1.left = n2;
			n1.right = n5; n5.left = n1;
			Node<Integer,String> l = tree.add(n5, n3a);
			assertSame(n5, l);
			assertSame(n4, n5.right);
			assertSame(n3, n4.right);
			assertSame(n2, n3.right);
			assertSame(n1, n2.right);
			assertSame(n3a, n1.right);
			assertSame(n5, n3a.right);		
		}
		
		
		/// test4x: tests of append
		
		public void test40() {
			n1.left = n1.right = n1;
			assertSame(n1, tree.append(n1, null));
			assertEquals(1, tree.length(n1));
			assertSame(n1, tree.append(null, n1));
		}
		
		public void test41() {
			n4.left = n4.right = n4;
			n1.left = n1.right = n1;
			assertSame(n4, tree.append(n4, n1));
			assertEquals(2, tree.length(n1));
			assertSame(n1, n4.right);
			assertSame(n4, n1.right);
			assertSame(n1, tree.append(null, n1));
		}
		
		public void test42() {
			n4.left = n4.right = n3;
			n3.left = n3.right = n4;
			n2.left = n2.right = n2;
			tree.append(n4, n2);
			assertEquals(3, tree.length(n4));
			assertSame(n3, n4.right);
			assertSame(n2, n3.right);
		}
		
		public void test43() {
			n4.left = n4.right = n4;
			n3.left = n3.right = n2;
			n2.left = n2.right = n3;
			tree.append(n4, n2);
			assertEquals(3, tree.length(n4));
			assertSame(n2, n4.right);
			assertSame(n3, n2.right);
		}
		
		public void test44() {
			n1.left = n1.right = n4;
			n4.left = n4.right = n1;
			n2.left = n2.right = n3;
			n3.left = n3.right = n2;
			tree.append(n2, n4);
			assertEquals(4, tree.length(n4));
			assertSame(n4, n3.right);
			assertSame(n2, n1.right);
		}
		
		public void test45() {
			n1.left = n3; n1.right = n2;
			n2.left = n1; n2.right = n3;
			n3.left = n2; n3.right = n1;
			n4.left = n4.right = n5;
			n5.left = n5.right = n4;
			tree.append(n3, n5);
			assertEquals(5, tree.length(n1));
			assertSame(n5, n2.right);
			assertSame(n3, n4.right);
		}
		
		public void test46() {
			n1.left = n3; n1.right = n2;
			n2.left = n1; n2.right = n3;
			n3.left = n2; n3.right = n1;
			n3a.left = n3a.right = n3a;
			tree.append(n2, n3a);
			assertEquals(4, tree.length(n3a));
			assertSame(n3a, n1.right);
			assertSame(n2, n3a.right);
		}
		
		public void test47() {
			n3a.left = n3a.right = n3b;
			n3b.left = n3b.right = n3a;

			n1.left = n2; n1.right = n5;
			n2.left = n3; n2.right = n1;
			n3.left = n4; n3.right = n2;
			n4.left = n5; n4.right = n3;
			n5.left = n1; n5.right = n4;
			
			tree.append(n3a, n3);
			
			assertSame(n3b, n3a.right);
			assertSame(n3, n3b.right);
			assertSame(n2, n3.right);
			assertSame(n1, n2.right);
			assertSame(n5, n1.right);
			assertSame(n4, n5.right);
			assertSame(n3a, n4.right);
		}
		
		public void test48() {
			n1.left = n2; n1.right = n5;
			n2.left = n3; n2.right = n1;
			n3.left = n4; n3.right = n2;
			n4.left = n5; n4.right = n3;
			n5.left = n1; n5.right = n4;
			
			tree.append(n5, null);
			
			assertEquals(5, tree.length(n1));
		}
		
		public void test49() {
			n3.left = n3a; n3a.right = n3;
			n3a.left = n3b; n3b.right = n3a;
			n3b.left = n3; n3.right = n3b;
			
			n1.right = n5; n5.left = n1;
			n5.right = n2; n2.left = n5;
			n2.right = n4; n4.left = n2;
			n4.right = n1; n1.left = n4;
			
			tree.append(n1, n3);
			
			assertSame(n1, n3a.right);
			assertSame(n5, n1.right);
			assertSame(n2, n5.right);
			assertSame(n4, n2.right);
			assertSame(n3, n4.right);
			assertSame(n3b, n3.right);
			assertSame(n3a, n3b.right);
		}
		
		
		/// test5x: tests of toList
		
		public void test50() {
			tree.comparator = strange;
			assertNull(tree.toList(null));
		}
		
		public void test51() {
			tree.comparator = strange;
			assertSame(n1, tree.toList(n1));
			assertSame(n1, n1.left);
			assertSame(n1, n1.right);
		}
		
		public void test52() {
			tree.comparator = normal;
			n2.left = n1;
			assertSame(n1, tree.toList(n2));
			assertSame(n2, n1.right);
			assertSame(n1, n2.right);
		}
		
		public void test53() {
			tree.comparator = backward;
			n2.left = n5;
			n5.right = n4;
			assertSame(n5, tree.toList(n2));
			assertSame(n4, n5.right);
			assertSame(n2, n4.right);
			assertSame(n5, n2.right);
		}
		
		public void test54() {
			tree.comparator = normal;
			n3.left = n2;
			n3.right = n4;
			n4.right = n5;
			assertSame(n2, tree.toList(n3));
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			assertSame(n5, n4.right);
			assertSame(n2, n5.right);
		}
		
		public void test55() {
			tree.comparator = normal;
			n1.right = n2;
			n2.right = n3;
			n3.right = n4;
			n4.right = n5;
			assertSame(n1, tree.toList(n1));
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			assertSame(n5, n4.right);
			assertSame(n1, n5.right);
		}
		
		public void test56() {
			tree.comparator = backward;
			n1.left = n2;
			n2.left = n3;
			n3.left = n4;
			n4.left = n5;
			assertSame(n5, tree.toList(n1));
			assertSame(n4, n5.right);
			assertSame(n3, n4.right);
			assertSame(n2, n3.right);
			assertSame(n1, n2.right);
			assertSame(n5, n1.right);
		}
		
		public void test57() {
			tree.comparator = normal;
			n3.left = n1;
			n1.right = n2;
			n3.right = n5;
			n5.left = n4;
			assertSame(n1, tree.toList(n3));
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			assertSame(n5, n4.right);
			assertSame(n1, n5.right);
		}
		
		public void test58() {
			tree.comparator = backward;
			n3.left = n4;
			n4.left = n5;
			n3.right = n2;
			n2.right = n1;
			assertSame(n5, tree.toList(n3));
			assertSame(n4, n5.right);
			assertSame(n3, n4.right);
			assertSame(n2, n3.right);
			assertSame(n1, n2.right);
			assertSame(n5, n1.right);
		}
		
		public void test59() {
			tree.comparator = normal;
			n4.left = n2;
			n4.right = n6;
			n2.left = n1;
			n2.right = n3;
			n6.left = n5;
			n6.right = n7;
			assertSame(n1, tree.toList(n4));
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			assertSame(n5, n4.right);
			assertSame(n6, n5.right);
			assertSame(n7, n6.right);
			assertSame(n1, n7.right);
		}
		
		
		/// test6x: Tests of split
		
		public void test60() {
			n1.left = n1.right = n1;
			
			assertNull(tree.split(n1, 1));
			
			assertSame(n1, n1.right);
		}
		
		public void test61() {
			n1.left = n2; n1.right = n2;
			n2.left = n1; n2.right = n1;
			
			Node<Integer,String> l = tree.split(n1, 1);
			
			assertSame(n2, l);
			assertSame(n2, n2.right);
		}
		
		public void test62() {
			n1.left = n2; n1.right = n2;
			n2.left = n1; n2.right = n1;
			
			Node<Integer,String> l = tree.split(n1, 2);
			
			assertNull(l);
			assertSame(n2, n1.right);
			assertSame(n1, n2.right);
		}
		
		public void test63() {
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n1; n1.left = n3;
			
			Node<Integer,String> l = tree.split(n2, 1);
			
			assertSame(n3, l);
			assertSame(n1, n3.right);
			assertSame(n3, n1.right);
		}
		
		public void test64() {
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n1; n1.left = n3;
			
			Node<Integer,String> l = tree.split(n3, 2);
			
			assertSame(n2, l);
			assertSame(n2, n2.right);
		}

		public void test65() {
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n1; n1.left = n3;
			
			Node<Integer,String> l = tree.split(n3, 3);
			
			assertNull(l);
			assertSame(n1, n3.right);
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
		}

		public void test66() {
			n1.right = n3; n3.left = n1;
			n3.right = n2; n2.left = n3;
			n2.right = n4; n4.left = n2;
			n4.right = n1; n1.left = n4;
			
			Node<Integer,String> l = tree.split(n1, 1);
			
			assertSame(n3, l);
			assertSame(n2, n3.right);
			assertSame(n4, n2.right);
			assertSame(n3, n4.right);
		}
		
		public void test67() {
			n1.right = n3; n3.left = n1;
			n3.right = n2; n2.left = n3;
			n2.right = n4; n4.left = n2;
			n4.right = n1; n1.left = n4;
			
			Node<Integer,String> l = tree.split(n1, 2);
			
			assertSame(n2, l);
			assertSame(n4, n2.right);
			assertSame(n2, n4.right);
		}
		
		public void test68() {
			n1.right = n3; n3.left = n1;
			n3.right = n2; n2.left = n3;
			n2.right = n4; n4.left = n2;
			n4.right = n1; n1.left = n4;
			
			Node<Integer,String> l = tree.split(n1, 3);
			
			assertSame(n3, n1.right);
			assertSame(n2, n3.right);
			assertSame(n1, n2.right);
			
			assertSame(n4, l);
			assertSame(n4, n4.right);
		}
		
		public void test69() {
			n1.right = n3; n3.left = n1;
			n3.right = n2; n2.left = n3;
			n2.right = n4; n4.left = n2;
			n4.right = n1; n1.left = n4;
			
			Node<Integer,String> l = tree.split(n1, 4);
			
			assertSame(n3, n1.right);
			assertSame(n2, n3.right);
			assertSame(n4, n2.right);
			assertSame(n1, n4.right);
			
			assertNull(l);
		}

		
		/// test7x: tests of merge
		
		public void test70() {
			tree.comparator = normal;
			Node<Integer,String> l = tree.merge(null, null);
			
			assertNull(l);
		}
		
		public void test71() {
			tree.comparator = normal;
			n1.left = n1.right = n1;
			
			assertSame(n1, tree.merge(null, n1));
			assertSame(n1, tree.merge(n1,  null));
		}
		
		public void test72() {
			tree.comparator = backward;
			n1.left = n1.right = n2;
			n2.left = n2.right = n1;
			
			assertSame(n2, tree.merge(null,  n2));
			assertEquals(2, tree.length(n2));
			
			assertSame(n2, tree.merge(n2,  null));
			assertEquals(2, tree.length(n2));
			
			assertSame(n1, n2.left);
		}
		
		public void test73() {
			tree.comparator = normal;
			n1.left = n1.right = n1;
			n2.left = n2.right = n2;
			
			Node<Integer,String> l = tree.merge(n1, n2);
			assertEquals(2, tree.length(l));
			assertEquals(1, l.key.intValue());
			assertEquals("one", l.value);
			assertSame(n1,l);
			assertSame(n2, n1.right);
			assertEquals(2, n2.key.intValue());
			assertEquals("two", n2.value);
		}
		
		public void test74() {
			tree.comparator = backward;
			n1.left = n1.right = n1;
			n2.left = n2.right = n2;
			
			Node<Integer,String> l = tree.merge(n1, n2);
			assertEquals(2, tree.length(l));
			assertEquals(2, l.key.intValue());
			assertEquals("two", l.value);
			assertSame(n2,l);
			assertSame(n1, n2.right);
			assertEquals(1, n1.key.intValue());
			assertEquals("one", n1.value);
		}
		
		public void test75() {
			tree.comparator = strange;
			n1.left = n1.right = n1;
			n2.left = n2.right = n2;
			
			Node<Integer,String> l = tree.merge(n1, n2);
			assertEquals(1, tree.length(l));
			assertEquals(2, l.key.intValue());
			assertEquals("two", l.value);
			assertSame(n2,l);
			assertEquals(1, tree.length(n1));
			assertEquals(1, n1.key.intValue());
			assertEquals("one", n1.value);
		}
		
		public void test76() {
			tree.comparator = normal;
			n1.left = n1.right = n3;
			n3.left = n3.right = n1;
			n2.left = n2.right = n4;
			n4.left = n4.right = n2;
			
			Node<Integer,String> l = tree.merge(n1, n2);
			assertEquals(4, tree.length(l));
			assertSame(n1, l);
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
		}
		
		public void test77() {
			tree.comparator = normal;
			n1.left = n1.right = n4;
			n4.left = n4.right = n1;
			
			n2.right = n3; n3.left = n2;
			n3.right = n5; n5.left = n3;
			n5.right = n2; n2.left = n5; 
			
			Node<Integer,String> l = tree.merge(n2, n1);
			assertEquals(5, tree.length(l));
			assertSame(n1, l);
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			assertSame(n5, n4.right);
		}
		
		public void test78() {
			tree.comparator = normal;
			n1.right = n3b; n3b.left = n1;
			n3b.right = n4; n4.left = n3b;
			n4.right = n1; n1.left = n4;
			
			n2.right = n3; n3.left = n2;
			n3.right = n5; n5.left = n3;
			n5.right = n2; n2.left = n5; 
			
			Node<Integer,String> l = tree.merge(n1, n2);
			assertEquals(5, tree.length(l));
			assertSame(n1, l);
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			assertSame(n5, n4.right);
		}
		
		public void test79() {
			tree.comparator = normal;
			n1.right = n3b; n3b.left = n1;
			n3b.right = n4; n4.left = n3b;
			n4.right = n5; n5.left = n4;
			n5.right = n1; n1.left = n5;
			
			n2.right = n3; n3.left = n2;
			n3.right = n2; n2.left = n3;
			
			Node<Integer,String> l = tree.merge(n1, n2);
			assertEquals(5, tree.length(l));
			assertSame(n1, l);
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			assertSame(n5, n4.right);
		}
		
		
		/// test8x: tests of sort
		
		public void test80() {
			tree.comparator = normal;
			
			assertNull(tree.sort(null, 0));
		}
		
		public void test81() {
			tree.comparator = normal;
			n1.left = n1.right = n1;
			
			assertSame(n1, tree.sort(n1, 1));
			assertEquals(1, tree.length(n1));
		}
		
		public void test82() {
			tree.comparator = backward;
			n1.left = n1.right = n2;
			n2.left = n2.right = n1;
			
			assertSame(n2, tree.sort(n1, 2));
			assertEquals(2, tree.length(n1));
			assertSame(n1, n2.right);
			
			tree.comparator = strange;
			assertSame(n2, tree.sort(n1, 2));
			assertEquals(1, tree.length(n2));
		}
		
		public void test83() {
			tree.comparator = normal;
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n1; n1.left = n3;
			
			assertSame(n1, tree.sort(n2, 3));
			assertEquals(3, tree.length(n1));
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			
			tree.comparator = backward;
			assertSame(n3, tree.sort(n1, 3));
			assertEquals(3, tree.length(n3));
			assertSame(n2, n3.right);
			assertSame(n1, n2.right);
			
			tree.comparator = strange;
			assertSame(n2, tree.sort(n1, 3));
			assertEquals(1, tree.length(n2));
		}
		
		public void test84() {
			tree.comparator = normal;
			n1.right = n3; n3.left = n1;
			n3.right = n2; n2.left = n3;
			n2.right = n4; n4.left = n2;
			n4.right = n1; n1.left = n4;

			assertSame(n1, tree.sort(n2, 4));
			assertEquals(4, tree.length(n1));
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			
			assertSame(n1, tree.sort(n3, 4));
			assertEquals(4, tree.length(n1));
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			
			tree.comparator = strange;
			assertSame(n2, tree.sort(n3, 4));
			assertEquals(1, tree.length(n2));
		}
		
		public void test85() {
			n1.left = n2; n1.right = n5;
			n2.left = n3; n2.right = n1;
			n3.left = n4; n3.right = n2;
			n4.left = n5; n4.right = n3;
			n5.left = n1; n5.right = n4;
			tree.comparator = backward;
			
			assertSame(n5, tree.sort(n1, 5));
			assertEquals(5, tree.length(n5));
			assertSame(n4, n5.right);
			assertSame(n3, n4.right);
			assertSame(n2, n3.right);
			assertSame(n1, n2.right);
			
			assertSame(n5, tree.sort(n3, 5));
			assertEquals(5, tree.length(n5));
			assertSame(n4, n5.right);
			assertSame(n3, n4.right);
			assertSame(n2, n3.right);
			assertSame(n1, n2.right);
		}
		
		public void test86() {
			n1.right = n3; n3.left = n1;
			n3.right = n5; n5.left = n3;
			n5.right = n2; n2.left = n5;
			n2.right = n4; n4.left = n2;
			n4.right = n3a; n3a.left = n4;
			n3a.right = n1; n1.left = n3a;
			
			tree.comparator = normal;
			assertSame(n1, tree.sort(n2, 6));
			assertEquals(5, tree.length(n1));
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			assertSame(n5, n4.right);
		}
		
		public void test87() {
			n1.right = n3; n3.left = n1;
			n3.right = n5; n5.left = n3;
			n5.right = n3b; n3b.left = n5;
			n3b.right = n2; n2.left = n3b;
			n2.right = n4; n4.left = n2;
			n4.right = n3a; n3a.left = n4;
			n3a.right = n1; n1.left = n3a;
			
			tree.comparator = backward;
			assertSame(n5, tree.sort(n3b, 7));
			assertEquals(5, tree.length(n5));
			assertSame(n4, n5.right);
			assertSame(n3, n4.right);
			assertSame(n2, n3.right);
			assertSame(n1, n2.right);
		}
		
		public void test88() {
			Node<Integer,String> n5b = new Node<>(5, "not five");
			n1.right = n3; n3.left = n1;
			n3.right = n5; n5.left = n3;
			n5.right = n3b; n3b.left = n5;
			n3b.right = n2; n2.left = n3b;
			n2.right = n4; n4.left = n2;
			n4.right = n3a; n3a.left = n4;
			n3a.right = n5b; n5b.left = n3a;
			n5b.right = n1; n1.left = n5b;
			
			tree.comparator = normal;
			assertSame(n1, tree.sort(n3b, 8));
			assertEquals(5, tree.length(n1));
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			assertSame(n5, n4.right);
		}
		
		public void test89() {
			Node<Integer,String> n5b = new Node<>(5, "not five");
			
			n1.right = n3; n3.left = n1;
			n3.right = n5; n5.left = n3;
			n5.right = n3b; n3b.left = n5;
			n3b.right = n2; n2.left = n3b;
			n2.right = n4; n4.left = n2;
			n4.right = n3a; n3a.left = n4;
			n3a.right = n5b; n5b.left = n3a;
			n5b.right = n6; n6.left = n5b;
			n6.right = n1; n1.left = n6;
			
			tree.comparator = normal;
			assertSame(n1, tree.sort(n3b, 9));
			assertEquals(6, tree.length(n1));
			assertSame(n2, n1.right);
			assertSame(n3, n2.right);
			assertSame(n4, n3.right);
			assertSame(n5, n4.right);
			assertSame(n6, n5.right);
		}
		
		
		/// test9x: tests of toTree
		
		public void test90() {
			tree.comparator = normal;
			assertNull(tree.toTree(null, 0));
		}
		
		public void test91() {
			tree.comparator = normal;
			n1.left = n1.right = n1;
			assertSame(n1, tree.toTree(n1, 1));
			// the following should not need to be checked
			assertNull(n1.left);
			assertNull(n1.right);
		}
		
		public void test92() {
			tree.comparator = normal;
			n1.left = n1.right = n2;
			n2.left = n2.right = n1;
			
			tree.root = tree.toTree(n1, 2);
			assertTrue(tree.root == n1 || tree.root == n2);
			assertTrue(tree.root.left == n1 || tree.root.right == n2);
			
			// should not need to be checked:
			tree.numItems = 2;
			assertTrue(tree.wellFormed());			
		}
		
		public void test93() {
			tree.comparator = backward;
			n1.left = n2; n2.right = n1;
			n2.left = n3; n3.right = n2;
			n3.left = n1; n1.right = n3;

			assertSame(n2, tree.toTree(n3, 3));
			assertSame(n3, n2.left);
			assertSame(n1, n2.right);

			// should not need to be checked:
			tree.root = n2;
			tree.numItems = 3;
			assertTrue(tree.wellFormed());	
		}
		
		public void test94() {
			tree.comparator = normal;
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n4; n4.left = n3;
			n4.right = n1; n1.left = n4;
			
			tree.root = tree.toTree(n1, 4);
			assertTrue(tree.root == n2 || tree.root == n3);
			assertTrue(tree.root.left == n1 || tree.root.left == n2);
			assertTrue(tree.root.right == n3 || tree.root.right == n4);
			
			tree.numItems = 4;
			assertTrue(tree.wellFormed());
		}
		
		public void test95() {
			tree.comparator = normal;
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n4; n4.left = n3;
			n4.right = n5; n5.left = n4;
			n5.right = n1; n1.left = n5;
			
			tree.root = tree.toTree(n1, 5);
			assertTrue(n3 == tree.root);
			assertTrue(n1 == n3.left || n2 == n3.left);
			assertTrue(n1 == n3.left.left || n2 == n3.left.right);
			assertTrue(n4 == n3.right || n5 == n3.right);
			assertTrue(n4 == n3.right.left || n5 == n3.right.right);
		}
		
		public void test96() {
			tree.comparator = normal;
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n4; n4.left = n3;
			n4.right = n5; n5.left = n4;
			n5.right = n6; n6.left = n5;
			n6.right = n1; n1.left = n6;
			
			tree.root = tree.toTree(n1, 6);
			assertTrue(tree.root == n3 || tree.root == n4);
			if (tree.root == n3) {
				assertSame(n5, n3.right);
				assertSame(n4, n5.left);
				assertSame(n6, n5.right);
			} else {
				assertSame(n2, n4.left);
				assertSame(n3, n2.right);
				assertSame(n1, n2.left);
			}
			tree.numItems = 6;
			assertTrue(tree.wellFormed());
		}
		
		public void test97() {
			tree.comparator = normal;
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n4; n4.left = n3;
			n4.right = n5; n5.left = n4;
			n5.right = n6; n6.left = n5;
			n6.right = n7; n7.left = n6;
			n7.right = n1; n1.left = n7;
			
			tree.root = tree.toTree(n1, 7);
			assertTrue(tree.root == n4);
			assertSame(n2, n4.left);
			assertSame(n3, n2.right);
			assertSame(n1, n2.left);
			assertSame(n6, n4.right);
			assertSame(n5, n6.left);
			assertSame(n7, n6.right);
			
			tree.numItems = 7;
			assertTrue(tree.wellFormed());
		}
		
		public void test98() {
			tree.comparator = normal;
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n4; n4.left = n3;
			n4.right = n5; n5.left = n4;
			n5.right = n6; n6.left = n5;
			n6.right = n7; n7.left = n6;
			n7.right = n8; n8.left = n7;
			n8.right = n1; n1.left = n8;
			
			tree.root = tree.toTree(n1, 8);
			assertTrue(tree.root == n4 || tree.root == n5);
			if (tree.root == n4) {
				assertSame(n2, n4.left);
				assertSame(n3, n2.right);
				assertSame(n1, n2.left);
				
				assertTrue(n4.right == n6 || n4.right == n7);
			} else {
				assertSame(n7, n5.right);
				assertSame(n6, n7.left);
				assertSame(n8, n7.right);
				
				assertTrue(n5.left == n2 || n5.left == n3);
			}
			
			tree.numItems = 8;
			assertTrue(tree.wellFormed());
		}

		public void test99() {
			tree.comparator = normal;
			n1.right = n2; n2.left = n1;
			n2.right = n3; n3.left = n2;
			n3.right = n4; n4.left = n3;
			n4.right = n5; n5.left = n4;
			n5.right = n6; n6.left = n5;
			n6.right = n7; n7.left = n6;
			n7.right = n8; n8.left = n7;
			n8.right = n9; n9.left = n8;
			n9.right = n1; n1.left = n9;
			
			tree.root = tree.toTree(n1, 9);
			assertTrue(tree.root == n5);
			assertTrue(n5.left == n2 || n5.left == n3);
			assertTrue(n5.right == n7 || n5.right == n8);
			
			tree.numItems = 9;
			assertTrue(tree.wellFormed());
		}
	}	
}
