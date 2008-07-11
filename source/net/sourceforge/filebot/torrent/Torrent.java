
package net.sourceforge.filebot.torrent;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Torrent {
	
	private final String name;
	private final String encoding;
	private final String createdBy;
	private final String announce;
	private final String comment;
	private final Long creationDate;
	private final Long pieceLength;
	
	private final List<Entry> files;
	
	private final boolean singleFileTorrent;
	
	
	public Torrent(File torrent) throws IOException {
		this(new FileInputStream(torrent));
	}
	

	/**
	 * Load torrent data from an <code>InputStream</code>. The given stream will be closed
	 * after data has been read.
	 */
	public Torrent(InputStream inputStream) throws IOException {
		this(decodeTorrent(inputStream));
	}
	

	public Torrent(Map<?, ?> torrentMap) {
		
		Charset charset = Charset.forName("UTF-8");
		
		encoding = decodeString(torrentMap.get("encoding"), charset);
		
		try {
			charset = Charset.forName(encoding);
		} catch (IllegalArgumentException e) {
			// invalid encoding, just keep using UTF-8
			Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.WARNING, e.getMessage());
		}
		
		createdBy = decodeString(torrentMap.get("created by"), charset);
		announce = decodeString(torrentMap.get("announce"), charset);
		comment = decodeString(torrentMap.get("comment"), charset);
		creationDate = decodeLong(torrentMap.get("creation date"));
		
		Map<?, ?> infoMap = (Map<?, ?>) torrentMap.get("info");
		
		name = decodeString(infoMap.get("name"), charset);
		pieceLength = (Long) infoMap.get("piece length");
		
		if (infoMap.containsKey("files")) {
			// torrent contains multiple entries
			singleFileTorrent = false;
			
			List<Entry> entries = new ArrayList<Entry>();
			
			for (Object fileMapObject : (List<?>) infoMap.get("files")) {
				Map<?, ?> fileMap = (Map<?, ?>) fileMapObject;
				List<?> pathList = (List<?>) fileMap.get("path");
				
				StringBuilder pathBuilder = new StringBuilder();
				String entryName = null;
				
				Iterator<?> iterator = pathList.iterator();
				
				while (iterator.hasNext()) {
					String pathElement = decodeString(iterator.next(), charset);
					
					if (iterator.hasNext()) {
						pathBuilder.append(pathElement);
						pathBuilder.append("/");
					} else {
						// the last element in the path list is the filename
						entryName = pathElement;
					}
				}
				
				Long length = decodeLong(fileMap.get("length"));
				
				entries.add(new Entry(entryName, length, pathBuilder.toString()));
			}
			
			files = Collections.unmodifiableList(entries);
		} else {
			// single file torrent
			singleFileTorrent = true;
			
			Long length = decodeLong(infoMap.get("length"));
			
			files = Collections.singletonList(new Entry(name, length, ""));
		}
	}
	

	private String decodeString(Object byteArray, Charset charset) {
		if (byteArray == null)
			return null;
		
		return new String((byte[]) byteArray, charset);
	}
	

	private Long decodeLong(Object number) {
		if (number == null)
			return null;
		
		return (Long) number;
	}
	

	public String getAnnounce() {
		return announce;
	}
	

	public String getComment() {
		return comment;
	}
	

	public String getCreatedBy() {
		return createdBy;
	}
	

	public Long getCreationDate() {
		return creationDate;
	}
	

	public String getEncoding() {
		return encoding;
	}
	

	public List<Entry> getFiles() {
		return files;
	}
	

	public String getName() {
		return name;
	}
	

	public Long getPieceLength() {
		return pieceLength;
	}
	

	public boolean isSingleFileTorrent() {
		return singleFileTorrent;
	}
	

	private static Map<?, ?> decodeTorrent(InputStream torrent) throws IOException {
		BufferedInputStream in = new BufferedInputStream(torrent);
		
		try {
			return BDecoder.decode(in);
		} finally {
			in.close();
		}
	}
	
	
	public static class Entry {
		
		private final String name;
		private final Long length;
		private final String path;
		
		
		public Entry(String name, long length, String path) {
			this.name = name;
			this.length = length;
			this.path = path;
		}
		

		public Long getLength() {
			return length;
		}
		

		public String getName() {
			return name;
		}
		

		public String getPath() {
			return path;
		}
	}
	
}
