package se.ridorana.roze.stuff.FFXIVDatTools;

import java.util.List;


public interface ISearchable {
	public List<Long> find(final DatSegment segment, byte[] searcher);
}
