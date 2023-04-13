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
		// remove all this code when implementing compress
		int[] counts = getCounts(in);
		HuffNode tree = makeTree(counts);

		in.reset();
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeTree(tree, out);

		String[] encodStrings = new String[ALPH_SIZE + 1];
		makeEncodings(tree, "", encodStrings);
		while (true) {
			int bits = in.readBits(BITS_PER_WORD);
			if (bits == -1) {break;}

		String code = encodStrings[bits];
		if (code != null) {
			out.writeBits(code.length(), Integer.parseInt(code, 2));
		}
		}

		String code = encodStrings[PSEUDO_EOF];
		out.writeBits(code.length(), Integer.parseInt(code, 2));
	out.close();
	}

	private int[] getCounts(BitInputStream in) {
		int[] counts = new int[ALPH_SIZE + 1];

		while (true) {
			int bits = in.readBits(BITS_PER_WORD);

			if (bits == -1) {break;}
			counts[bits]++;
		}

		counts[PSEUDO_EOF] = 1;
		return counts;
		
	}

	private HuffNode makeTree(int [] counts) {
		PriorityQueue<HuffNode> queue = new PriorityQueue<>();

		int num = 0;
		for (int i : counts) {
			if (i > 0) {
				queue.add(new HuffNode(num, i, null, null));
			num++;
			}
		}
		queue.add(new HuffNode(PSEUDO_EOF, 1, null, null));

		while (queue.size() > 1) {
			HuffNode left = queue.remove();
			HuffNode right = queue.remove();
			HuffNode t = new HuffNode(0, left.weight + right.weight, left, right);
			queue.add(t);
		}

		HuffNode node = queue.remove();
		return node;
	}

	private void makeEncodings(HuffNode node, String path, String[] encodings) {
		//base case
		if (node == null) {return;}

		//leaf case
		if (isLeaf(node)) {
			encodings[node.value] = path;
			return;
		}

		//recurse (preorder)
		else {
			makeEncodings(node.left, path + "0", encodings);
			makeEncodings(node.right, path + "1", encodings);
		}
	}

	private void writeTree(HuffNode root, BitOutputStream out) {
		//base case
		if (root == null) {return;}
		
		//leaf case
		if (isLeaf(root)) {
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1, root.value);
		}
		//else recurse
		else {
			out.writeBits(1, 0);

			writeTree(root.left, out);
			writeTree(root.right, out);
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
	public void decompress(BitInputStream in, BitOutputStream out){

		// remove all code when implementing decompress
		HuffNode root = readTree(in);
		HuffNode current = root;

		while (true){
			int val = in.readBits(1);
			if (val == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF");
			}
			else {
				if (val == 0) {current = current.left;}
				else {current = current.right;}

				//break from loop if current is a leaf and == end of file
				if (isLeaf(current)) {
					if (current.value == PSEUDO_EOF) {
						break;
					}
					else {
						//start back after leaf
						out.writeBits(BITS_PER_WORD, current.value);
						current = root;
					}
				}
			}
			//out.writeBits(BITS_PER_WORD, val);
		}
		out.close();
	}

	private HuffNode readTree(BitInputStream in) {
		int bit = in.readBits(1);

		if (bit == -1) {
			throw new HuffException("readBits failed");
		}

		if (bit == 0) {
			HuffNode left = readTree(in);
			HuffNode right = readTree(in);
			return new HuffNode(0, 0, left, right);
		}

		else {
			int value = in.readBits(BITS_PER_WORD + 1);
			return new HuffNode(value, 0);
		}

	}

	private boolean isLeaf(HuffNode node) {
		if (node.left == null && node.right == null) {
			return true;
		}
		return false;
	}
}