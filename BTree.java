import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
/*
import cs321.btree.Cache;
import cs321.btree.E;
*/
public class BTree {
	public BTreeNode root;
	public int t, k, offset;
	public int numOfNodes;
	public Cache cache;
	public int currentPointer;
	//Metadata that might be helpful: 
	//Where is the root? What is k? What is t?
	//Metadata should be at the start of the file
	
	BTree(int t, int k) throws IOException{
		this.t = t;
		this.root = new BTreeNode(t);
		this.k = k;
		this.numOfNodes = 0;
		this.currentPointer = 0;
		diskWrite(root);
		offset = 12 * t + 1;
		
		//cacheSize is needed when creating a new Btree object
		int cacheSize;
		/*
		if(cacheSize > 0)
		{
			cache = new Cache(cacheSize);
		}
		*/
	}
	
	/*
	 * B-Tree-Create(t){
	 * BTreeNode x  new BTreeNode(t);
	 * x.isLeaf = true;
	 * x.n = 0;
	 * Disk-Write(x)
	 * T.root = x;
	 */
	
	public void insert(long key) throws IOException {
		BTreeNode r = this.root;
		if(r.n == t - 1) {
			BTreeNode s = new BTreeNode(t);
			this.root = s;
			s.isLeaf = false;
			s.n = 0;
			s.setChildPointerAtIndex(1, r.location);
			split(s, 1 ,r);
			insertNonFull(s, key);
		} else {
			insertNonFull(r, key);
		}
	}
	
	public void insertNonFull(BTreeNode x, long k) throws IOException {
		int i = x.n;
		if(x.isLeaf) {
			while(i >= 1 && k < x.getLKey(i)) {
				x.setKey(i + 1, x.getLKey(i));
				i--;
			}
			
			x.setKey(i + 1, k);
			x.n++;
			diskWrite(x);
			//cache.addNode(x);
			numOfNodes++;
		} else {
			while(i >= 1 && k < x.getLKey(i)) {
				i--;
			}
			i++;
			//Disk-Read(x.child[i])
			BTreeNode btn = diskRead(x.getChild(i));
			if(btn.n == 2 * t - 1) {
				split(x, i, btn);
				if(k >= x.getLKey(i)) {
					i++;
				}
			}
			insertNonFull(btn, k);
			numOfNodes++;
		}
	}
	
	public void split(BTreeNode x, int i, BTreeNode y) throws IOException { //x.child(i) = y is full
		BTreeNode z = new BTreeNode(t);
		z.isLeaf = y.isLeaf;
		z.n = t - 1;
		for(int j = 1; j < t - 1; j++) {
			z.setKey(j, y.getLKey(j + t));
		}
		if(!y.isLeaf) {
			for(int j = 1; j <= t; j++) {
				z.setChildPointerAtIndex(j, y.getChild(j + t));
			}
		}
		y.n = t - 1;
		for(int j = x.n + 1; j >= i + 1; j--) {
			x.setChildPointerAtIndex(j + 1, x.getChild(j));
		}
		x.setChildPointerAtIndex(i + 1, z.location);
		for(int j = x.n; j >= i; j--) {
			x.setKey(j + 1, x.getLKey(j));
		}
		x.setKey(i, y.getLKey(t - 1));
		x.n++;
		diskWrite(y);
		diskWrite(z);
		diskWrite(x);
	}
		/*
		if(cache!= null)
		{
			cache.addNode(x);
			cache.addNode(y);
			cache.addNode(z);
		}
		numOfNodes++;
	}
	*/
	public void diskWrite(BTreeNode node) throws IOException {
		RandomAccessFile ram = new RandomAccessFile("file.binary", "rw");
		
		node.location = offset;
		//First write location, n, and isLeaf
		ram.writeInt(node.location);
		ram.writeInt(node.n);
		ram.writeBoolean(node.isLeaf);
		//then write keys

		for(int i = 1; i <= node.n; i++) {
			ram.writeLong(node.getLKey(i));
		}
		//then write child pointers
		for(int i = 1; i <= node.n + 1; i++) {
			ram.writeInt(node.getChild(i));
		}
		offset += (12 * t + 1);
	}
	
	public BTreeNode diskRead(int byteLocation) throws IOException {
		RandomAccessFile ram = new RandomAccessFile("file.binary", "rw");
		BTreeNode newNode = new BTreeNode(t);
		ram.seek(byteLocation);
		newNode.location = ram.readInt();
		newNode.n = ram.readInt();
		newNode.isLeaf = ram.readBoolean();
		for(int i = 1; i <= newNode.n; i++) {
			newNode.setKey(i, ram.readLong());
		}
		for(int i = 1; i <= newNode.n + 1; i++) {
			newNode.setChildPointerAtIndex(i, ram.readInt());
		}
		return newNode;
		
	}
	
	public TreeObject search(BTreeNode x, int key) throws IOException{
		int i = 1;
		while(i <= x.n && k > x.getKey(i).getDNASDubstring()) {
			i++;
		}
		if(i <= x.n && k == x.getKey(i).getDNASDubstring()) {
			return x.treeKeys[i];
		} else if(x.isLeaf) {
			return null;
		} else {
			return search(diskRead(x.childPointers[i]), key);
		}
	}	
	public byte[] longToBytes(long x) {		
		ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
		buf.putLong(x);
		return buf.array();	
	}
	
	public long bytesToLong(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
		buf.put(bytes);
		buf.flip();
		return buf.getLong();
	}
	public static byte[] intToBytes(int x) {
		ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
		buf.putInt(x);
		return buf.array();
	}
	public static int bytesToInt(byte[] bytes) {
		ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
		buf.put(bytes);
		buf.flip();
		return buf.getInt();
	}
	
	/**
	 * @author Javier Trejo
	 * Returns the number of nodes the BTree contains.
	 * @return the number of nodes of the BTree.
	 */
	public int getNumberOfNodes()
	{
		return numOfNodes;
	}
	
	/**
	 * @author Javier Trejo
	 * Returns the pointer of the currently active node the 
	 * BTree is using.
	 * @return the pointer of the currently active node.
	 */
	public int getCurrentPointer()
	{
		return currentPointer;
	}
	
	/**
	 * @author Javier Trejo
	 * Returns an array of the contents of the node currently loaded
	 * into memory.
	 * @return an array of type E[] of the contents of the currently loaded
	 * node
	 * @throws IOException if an error occurs while reading or writing from the binary file.
	 * @throws ClassNotFoundException if the class type of E cannot be found.
	 
	public E[] getCurrentNodeData() throws ClassNotFoundException, IOException 
	{
		return diskRead(currentPointer).toArray();
	}
	
	/**
	 * @author Javier Trejo
	 * Closes the binary file and writes the BTreeData to the binary file. 
	 * @throws IOException If the file cannot be closed or if some other kind of error occurs when
	 * interacting with the binary file.
	 
	public void closeFile() throws IOException 
	{
		if(cache != null) {
			cache.clearCache();
		}
		diskWrite();
		ram.close();
	}
	*/
}
