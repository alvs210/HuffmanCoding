import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Ow	en Astrachan
 *
 * Revise
 */

 //comment//
public class HuffProcessor {

	private class HuffNode implements Comparable<HuffNode> {
		HuffNode left;
		HuffNode right;
		int value;
		int weight;

		public HuffNode(int val, int count) {
			value = val;
			weight = count;
		}
		public HuffNode(int val, int count, HuffNode ltree, HuffNode rtree) {
			value = val;
			weight = count;
			left = ltree;
			right = rtree;
		}

		public int compareTo(HuffNode o) {
			return weight - o.weight;
		}
	}

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private boolean myDebugging = false;
	
	public HuffProcessor() {
		this(false);
	}
	
	public HuffProcessor(boolean debug) {
		myDebugging = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void compress(BitInputStream in, BitOutputStream out){

		int[] counts = getCounts(in);
		HuffNode root = makeTree(counts);
		in.reset();
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeTree(root, out);

		String[] encodings = new String[1+ALPH_SIZE];
		makeEncodings(root, "", encodings);

		// in.reset();
		while(true){
			int noobies = in.readBits(BITS_PER_WORD);
			if (noobies == -1){
				break;
			}
			String cd = encodings[noobies];
			if(cd != null){
				out.writeBits(cd.length(),Integer.parseInt(cd,2));
			}
		}
		String ps = encodings[PSEUDO_EOF];
		out.writeBits(ps.length(),Integer.parseInt(ps,2));

	
		// Read the file again and write the encoding 
		//for each eight-bit chunk, followed by the encoding for PSEUDO_EOF, 
		//then close the file being written (not shown).
		out.close();
	}
		// private int[] getCounts (BitInputStream in) {
			
		// 	int[] vals = new int[1 +ALPH_SIZE];
			
		// 	int bits = in.readBits(BITS_PER_WORD);
			
		// 	while (bits != -1) {
		// 	vals[bits] += 1;
		// 	bits = in.readBits(BITS_PER_WORD);
		// 	}
			
		// 	return vals;
		// 	}

			private HuffNode makeTree(int[] counts) {
				PriorityQueue<HuffNode> pq = new PriorityQueue<>();
				for(int j = 0; j < counts.length; j ++){
					if (counts[j] > 0){
						pq.add(new HuffNode(j, counts[j], null, null));
					}
					// else {
					// 	continue;
					// }
				}
				pq.add(new HuffNode(PSEUDO_EOF, 1, null, null));
				while (pq.size() > 1){
					HuffNode left = pq.remove();
					HuffNode right = pq.remove();
					HuffNode x = new HuffNode(0, left.weight + right.weight, left, right);
					pq.add(x);
				}
				HuffNode root = pq.remove();
				return root;
		
			}
		private void makeEncodings (HuffNode root, String path, String[] x ) {
			
		if (root.left == null && root.right == null) {
        x[root.value] = path;
        return;
        }  
		
		// if (root.left != null) {
		// 	path = path + "0";
			// String[] encodings = new String[ALPH_SIZE + 1];
    		makeEncodings(root.left, path+"0", x);
		// }

		// if (root.right != null) {
		// 	path = path + "1";
		// 	String[] encodings = new String[ALPH_SIZE + 1];
    		makeEncodings(root.right, path+"1", x);
		// }
		
		}

		private void writeTree(HuffNode root, BitOutputStream out){
			if (root.value == -1){
				throw new HuffException("root == -1");
			}
			if (root.left != null || root.right != null){
				out.writeBits(1,0);
				writeTree(root.left, out);
				writeTree(root.right, out);
			}
			else {
				out.writeBits(1, 1);
				out.writeBits(BITS_PER_WORD + 1, root.value);
			
		}
	
	}
		

	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
	public void decompress(BitInputStream in, BitOutputStream out) {

		int bts = in.readBits(BITS_PER_INT);
		
 		if (bts != HUFF_TREE) {
			throw new HuffException("invalid magic number"+bts);
		}

		if (bts == -1){
			throw new IllegalStateException("bit is out of range (-1)");
		}
		HuffNode root = readTree(in);


		HuffNode current = root;
		
		while (true) {
			int bits = in.readBits(1);
			if (bits == -1) {
				throw new HuffException ("bad input, no PSUEDO_EOF");
			}
			else {
				if (bits == 0) { current = current.left; }
				else if (bits != 0) {current = current.right;}
				if (current.left == null && current.right == null) {
					if (current.value == PSEUDO_EOF) {
						break; }
					else {
						out.writeBits(BITS_PER_WORD, current.value);
						current = root;
					}
				}

			}
		}
		out.close();
	}

	private int[] getCounts(BitInputStream in){
		int[] counts = new int[ALPH_SIZE];
		int bits = in.readBits(BITS_PER_WORD);
		if (bits == -1){
			throw new IllegalStateException("bit is out of range (-1)");
		}
		while (bits  >= 0){
			
			counts[bits] += 1;
			 bits = in.readBits(BITS_PER_WORD);
		}
		return counts;
	}

	private HuffNode readTree(BitInputStream in){
	int bit = in.readBits(1);
	if (bit == -1){
		throw new IllegalStateException("bit is out of range (-1)");
	}
	// if (bit != HUFF_TREE) {
	// 	throw new HuffException("invalid magic number"+bit);
	// }

	if (bit == 0){
		HuffNode left = readTree(in);
		HuffNode right = readTree(in);
		return new HuffNode(0,0, left, right);
	}
	else {
		int value = in.readBits(BITS_PER_WORD + 1);
		return new HuffNode(value, 0, null, null);
	}

	}

	
}