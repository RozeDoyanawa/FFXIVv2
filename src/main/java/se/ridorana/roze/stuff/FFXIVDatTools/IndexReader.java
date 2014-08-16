package se.ridorana.roze.stuff.FFXIVDatTools;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

public class IndexReader {


	private static ConcurrentHashMap<String, IndexReaderWrapper> tables = new ConcurrentHashMap<>();

	public static void setDefaultSourceFolder(String defaultSourceFolder) {
		IndexReader.defaultSourceFolder = defaultSourceFolder;
	}

	private static class PathInfo {
		String index;
		String path;
		String name;

		private PathInfo(String index, String path, String name) {
			this.index = index;
			this.path = path;
			this.name = name;
		}
	}

	private static PathInfo splitPath(String searchpath) {
		String index;
		String _path;
		String _name;
		int atIndex = searchpath.indexOf("@");
		String separator;
		int sepTest;
		String sepTestStr;
		int separatorIndex = -1;
		if ((sepTest = searchpath.indexOf(sepTestStr = "/")) >= 0) {
			separator = sepTestStr;
			separatorIndex = sepTest;
		} else if ((sepTest = searchpath.indexOf(sepTestStr = ".")) >= 0) {
			separator = sepTestStr;
			separatorIndex = sepTest;
		} else {
			throw new RuntimeException("No path");
		}
		if (atIndex >= 0) {
			if (atIndex > separatorIndex) {
				index = searchpath.substring(atIndex + 1);
				searchpath = searchpath.substring(0, atIndex);
				separatorIndex = searchpath.indexOf(separator);
			} else {
				index = searchpath.substring(0, atIndex);
				searchpath = searchpath.substring(atIndex + 1);
				separatorIndex = searchpath.indexOf(separator);
			}
		} else {
			index = "000000";
		}
		_path = searchpath.substring(0, separatorIndex);
		_name = searchpath.substring(separatorIndex + 1);
		return new PathInfo(index, _path, _name);
	}

	/**
	 * @param searchpath
	 * @return
	 * @throws java.io.IOException
	 */
	public static FileBlockWrapper pathToSegment(String searchpath) throws IOException, DatSegment.HandlerException {
		PathInfo pi = splitPath(searchpath);
		long path = Long.valueOf(pi.path);
		long name = Long.valueOf(pi.name);
		return pathToSegment(path, name, pi.index);
	}

	/**
	 * Taken from stackoverflow  ( http://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java )
	 *
	 * @param s
	 * @return
	 */
	private static byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}


	/**
	 * Untested, I only use longs
	 *
	 * @param searchpath
	 * @return
	 */
	private static FileBlockWrapper pathToSegmentHex(String searchpath) throws IOException, DatSegment.HandlerException {
		PathInfo pi = splitPath(searchpath);
		if (pi.path.length() != 6 || pi.name.length() != 6) {
			throw new RuntimeException("Invalid hash length, both path and filename must be 6");
		}
		ByteBuffer pathBuffer = ByteBuffer.wrap(hexStringToByteArray(pi.path));
		pathBuffer.order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer nameBuffer = ByteBuffer.wrap(hexStringToByteArray(pi.name));
		nameBuffer.order(ByteOrder.LITTLE_ENDIAN);
		long path = pathBuffer.getLong();
		long name = nameBuffer.getLong();
		return pathToSegment(path, name, pi.index);
	}

	/**
	 * @param path
	 * @param file
	 * @param index
	 * @return
	 * @throws IOException
	 * @throws DatSegment.HandlerException
	 */
	private static FileBlockWrapper pathToSegment(long path, long file, String index) throws IOException, DatSegment.HandlerException {
		final IndexReaderWrapper irw;
		if (tables.containsKey(index)) {
			irw = tables.get(index);
		} else {
			IndexReader ir = new IndexReader();
			ir.setSourceFile(index);
			ir.read();
			irw = new IndexReaderWrapper(ir);
			tables.put(index, irw);
		}
		FileBlock fb = irw.getReader().getFile((int) path, (int) file);
		if (fb == null) {
			throw new NullPointerException(
					"No such file: " + FileBlock.getLongString(path) + " . " + FileBlock.getLongString(
							file
					) + " in " + index
			);
		}
		return new FileBlockWrapper(irw, fb);
	}

	public static final class FileBlock {
		/**
		 * @return
		 */
		public static long getName(final FileBlock block) {
			return block.name;
		}

		/**
		 * @return
		 */
		public static long getOffset(final FileBlock block) {
			return block.offset;
		}

		/**
		 * @return
		 */
		public static PathBlock getParent(final FileBlock block) {
			return block.parent;
		}

		/**
		 * @param block
		 * @return
		 */
		public static String getSearchPath(final FileBlock block) {
			return getLongString(getPath(block)) + "." + getLongString(getName(block));
		}

		/**
		 * @return
		 */
		public static long getPath(final FileBlock block) {
			return block.path;
		}

		private final long name;

		private final long path;

		private final long offset;

		private final byte something;

		private PathBlock parent = null;

		/**
		 * @throws IOException
		 */
		public FileBlock(final ByteBuffer map) throws IOException {
			this.name = map.getInt(); // raf.readInt32LE();
			this.path = map.getInt(); // raf.readInt32LE();
			int offset = map.getInt();
			this.offset = (offset >> 4) * 0x80;
			this.something = (byte) ((offset & 0xF) >> 1);
			map.position(map.position() + 4); // skipBytes(0x8);
		}

		public static String getLongString(long value) {
			return Long.toString(value & 0xffffffffl);
		}

		public void setParent(final PathBlock parent) {
			this.parent = parent;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("Segment[");
			sb.append("offset=" + offset);
			sb.append(", path=" + intToHex((int) path));
			sb.append(", name=" + intToHex((int) name));
			sb.append(", something=" + something);
			sb.append("]");
			return sb.toString();
		}

		public static int getSomething(FileBlock block) {
			return block.something;
		}
	}

	private static final class PathBlock {
		private final int path;
		private final int entryType;
		private final int firstFileOffset;
		private final Hashtable<Integer, FileBlock> files;

		public PathBlock(final ByteBuffer map) throws IOException {
			files = new Hashtable<>();
			path = map.getInt();
			firstFileOffset = map.getInt();
			entryType = map.getInt();
			// raf.skipBytes(4);
			map.position(map.position() + 4); // skipBytes(0x8);
		}


		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("Segment[");
			sb.append("path=" + intToHex(path));
			sb.append(", childType=" + entryType);
			sb.append(", childCount=" + files.size());
			sb.append(", offsetToFirstChild=" + firstFileOffset);
			sb.append("]");
			return sb.toString();
		}
	}

	private static class SegmentInfo {
		private final long offset;
		private final long length;
		private final String hash;

		public SegmentInfo(final ByteBuffer map) throws IOException {
			map.position(map.position() + 8); // skipBytes(0x8);
			offset = map.getInt(); // map.readInt32LE();
			length = map.getInt(); //
			//raf.skipBytes(0x2);
			final byte[] temp = new byte[20];
			map.get(temp);
			hash = new String(temp);
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("Segment[");
			sb.append("offset=" + offset);
			sb.append(", length=" + length);
			try {
				sb.append(", hash=" + byteArrayToHexString(hash.getBytes("ASCII")) + ")");
			} catch (final UnsupportedEncodingException e) {
				sb.append(", hash=<CharsetFault<" + hash + ">>]");
			}
			return sb.toString();
		}
	}

	static final char digits[] = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k',
			'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'
	};

	public static String byteArrayToHexString(final byte[] b) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < b.length; i++) {
			sb.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	public static final int getUnsignedByte(final ByteBuffer buffer) {
		final int t = buffer.get();
		if (t < 0) {
			return 256 + t;
		}
		return t;
	}

	private static final String intToHex(int i) {
		final char ac[] = new char[32];
		final int j = 4;
		int k = 0;
		final int l = 1 << j;
		final int i1 = l - 1;
		do {
			ac[((k % 2) == 0 ? k + 1 : k - 1)] = digits[i & i1];
			i >>>= j;
			k++;
		} while (i != 0);
		for (; k < (j * 2); k++) {
			ac[((k % 2) == 0 ? k + 1 : k - 1)] = '0';
		}
		return new String(ac, 0, k);
	}

	public static String toSHA1(final byte[] convertme) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (final NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return new String(md.digest(convertme));
	}

	private static String defaultSourceFolder = "C:/games/SquareEnix/FINAL FANTASY XIV - A Realm Reborn/game/sqpack/ffxiv";

	private String sourceFolder;


	private String sourceFile;

	private final Hashtable<Integer, PathBlock> pathtable;

	private boolean loaded = false;

	/**
	 *
	 */
	public IndexReader() {
		this.pathtable = new Hashtable<>();
		this.sourceFolder = defaultSourceFolder;
	}

	/**
	 * @param sourceFolder
	 */
	public IndexReader(String sourceFolder) {
		this.pathtable = new Hashtable<>();
		this.sourceFolder = sourceFolder;
	}

	/**
	 * @param path
	 * @param file
	 * @return
	 */
	public FileBlock getFile(final int path, final int file) {
		if (pathtable.containsKey(path)) {
			if (pathtable.get(path).files.containsKey(file)) {
				return pathtable.get(path).files.get(file);
			}
		}
		return null;
	}

	/**
	 * @param path
	 * @return
	 */
	public FileBlock[] getFiles(final int path) {
		if (pathtable.containsKey(path)) {
			final PathBlock pb = pathtable.get(path);
			final FileBlock[] blocks = new FileBlock[pb.files.size()];
			// C<Integer> e = pb.files.values();
			int index = 0;
			for (final FileBlock block : pb.files.values()) {
				blocks[index++] = block;
			}
			return blocks;
		}
		return null;
	}

	/**
	 * @return
	 */
	public int[] getPaths() {
		final int[] paths = new int[pathtable.size()];
		int i = 0;
		for (final PathBlock block : pathtable.values()) {
			paths[i++] = block.path;
		}
		return paths;
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public String getSourceFolder() {
		return sourceFolder;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public static class LittleEndianRandomAccessFile extends RandomAccessFile {

		/**
		 * @param file
		 * @param mode
		 * @throws FileNotFoundException
		 */
		public LittleEndianRandomAccessFile(final File file, final String mode) throws FileNotFoundException {
			super(file, mode);
		}

		/**
		 * @param file
		 * @param mode
		 * @throws FileNotFoundException
		 */
		public LittleEndianRandomAccessFile(final String file, final String mode) throws FileNotFoundException {
			super(file, mode);
		}

		/**
		 * @return
		 * @throws IOException
		 */
		public final int readInt32LE() throws IOException {
			final int i = read();
			final int j = read();
			final int k = read();
			final int l = read();
			if ((i | j | k | l) < 0) {
				throw new EOFException();
			} else {
				return (l << 24) + (k << 16) + (j << 8) + (i << 0);
			}
		}

		/**
		 * @return
		 * @throws IOException
		 */
		public final long readInt64LE() throws IOException {
			final long i = (long) readInt() & 4294967295L;
			final long j = (long) readInt();
			return (j << (32 + i));
		}

		/**
		 * @return
		 * @throws IOException
		 */
		public short readShortLE() throws IOException {
			final int i = read();
			final int j = read();
			if ((i | j) < 0) {
				throw new EOFException();
			} else {
				return (short) ((j << 8) + (i << 0));
			}
		}
	}

	public void read() throws IOException {
		if ((sourceFile == null) || (sourceFolder == null)) {
			throw new NullPointerException("Source cannot be null");
		}
		final long startedAt = System.currentTimeMillis();
		String source = sourceFolder;
		if (source.charAt(source.length() - 1) != '/') {
			source += "/";
		}
		source += sourceFile + ".win32.index";
		final File inputFile = new File(source);
		if (!inputFile.exists()) {
			throw new FileNotFoundException("Could not find fileBlock file: " + source);
		}
		// final ByteBuffer map = MappedByteBuffer.allocate((int)
		// inputFile.length());

		final LittleEndianRandomAccessFile raf = new LittleEndianRandomAccessFile(inputFile, "r");
		final MappedByteBuffer map = raf.getChannel().map(MapMode.READ_ONLY, 0, inputFile.length());
		map.order(ByteOrder.LITTLE_ENDIAN);
		map.position(0x400);
		final int headerLength = map.getInt(); // raf.readInt32LE();
		System.out.println(headerLength);
		final byte[] header = new byte[960];
		map.get(header);
		final String sha1_gen = toSHA1(header);
		map.position(0x400);
		final SegmentInfo segment1 = new SegmentInfo(map);
		map.position(0x400 + 76);
		final SegmentInfo segment2 = new SegmentInfo(map);
		map.position(0x400 + 76 + 72);
		final SegmentInfo segment3 = new SegmentInfo(map);
		map.position(0x400 + 76 + 72 + 72);
		final SegmentInfo segment4 = new SegmentInfo(map);
		// System.out.println(segment1.toString());
		// System.out.println(segment2.toString());
		// System.out.println(segment3.toString());
		// System.out.println(segment4.toString());
		map.position((int) segment1.offset); // fseek($fp, $segment1['offset'],
		// SEEK_SET);
		final int fileCount = (int) (segment1.length / 16l);
		final FileBlock[] files = new FileBlock[fileCount];
		for (int i = 0; i < fileCount; i++) {
			files[i] = new FileBlock(map);
			// System.out.println(blocks[i].toString());
		}
		map.position((int) segment4.offset);
		final int folderCount = (int) (segment4.length / 16l);
		final PathBlock[] paths = new PathBlock[fileCount];
		pathtable.clear();
		for (int i = 0; i < folderCount; i++) {
			paths[i] = new PathBlock(map);
			pathtable.put(paths[i].path, paths[i]);
		}
		for (int i = 0; i < fileCount; i++) {
			final PathBlock path = pathtable.get((int) files[i].path);
			path.files.put((int) files[i].name, files[i]);
			files[i].setParent(path);
		}

		final long endedAt = System.currentTimeMillis();
		System.out.println("Listing succeeded in " + (((double) endedAt - (double) startedAt) / 1000) + " seconds");
		setLoaded(true);
		// System.out.println(pathtable.toString());
		// System.out.println(Arrays.toString(blocks));
	}

	/**
	 *
	 */
	public void reset() {
		setLoaded(false);
		this.pathtable.clear();
	}

	private final void setLoaded(final boolean loaded) {
		this.loaded = loaded;
	}

	public void setSourceFile(final String sourceFile) {
		this.sourceFile = sourceFile;
		this.reset();
	}


	public void setSourceFolder(final String sourceFolder) {
		this.sourceFolder = sourceFolder;
	}

}
