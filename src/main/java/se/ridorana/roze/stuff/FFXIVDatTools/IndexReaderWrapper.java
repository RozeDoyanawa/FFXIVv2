package se.ridorana.roze.stuff.FFXIVDatTools;


import java.io.File;
import java.io.FileNotFoundException;
import java.nio.channels.FileChannel;

/**
 * Created by Roze on 2014-08-16.
 */
public class IndexReaderWrapper {

	private final FileChannel[] readChannel;
	private IndexReader reader;

	public FileChannel getReadChannel() {
		return readChannel[0];
	}

	public FileChannel getReadChannel(IndexReader.FileBlock block) {
		return readChannel[IndexReader.FileBlock.getSomething(block)];
	}

	public IndexReader getReader() {
		return reader;
	}

	public IndexReaderWrapper(IndexReader reader) throws FileNotFoundException {
		this.reader = reader;
		String dat = reader.getSourceFolder() + "/" + reader.getSourceFile() + ".win32.";
		String dat0 = dat + "dat0";
		String dat1 = dat + "dat1";
		String dat2 = dat + "dat2";
		this.readChannel = new FileChannel[3];
		final IndexReader.LittleEndianRandomAccessFile raf = new IndexReader.LittleEndianRandomAccessFile(dat0, "r");
		this.readChannel[0] = raf.getChannel();
		if (new File(dat1).exists()) {
			final IndexReader.LittleEndianRandomAccessFile raf1 = new IndexReader.LittleEndianRandomAccessFile(
					dat1, "r"
			);
			this.readChannel[1] = raf1.getChannel();
			if (new File(dat2).exists()) {
				final IndexReader.LittleEndianRandomAccessFile raf2 = new IndexReader.LittleEndianRandomAccessFile(
						dat2, "r"
				);
				this.readChannel[2] = raf2.getChannel();
			}
		}
	}

}
