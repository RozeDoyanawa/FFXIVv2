## FFXIVDatTools
This is just a simple library for reading FFXIV data files.
Code is provided as is for anyone who feel it would be useful.

FFXIV data files are divided into two groups, index and dat-files.
The dat-files can be read, or rather walked, without the index files, but with indexes its far easier.

The index files is basically just CRC32-hashed path and files name-pairs associated with an entry in the dat-file.
Which means, to access a given file, you need to know the original file-name, hash it and request it by the index.

I have yet to find a table-of-contents file in the archives, so no paths are included.
Recent discoveries suggest the game does not actually store the path-names, but rather access them by printf-sytax, ex. models/%s/h%04d.scm


This library includes functionality to decompress and render most texture files found in the archives, see below for how to access the rendered content.


###Example usage of the library:
First, set the directory where the data files can be found:
```IndexReader.setDefaultSourceFolder(String gamepath);```
Where gamepath is the path to the games data folder, ex: "C:/games/SquareEnix/FINAL FANTASY XIV - A Realm Reborn/game/sqpack/ffxiv"

Any index-string is at the form xx0000, where x = 00,01,02,03,04,05,06,07,08,0a,0b,0c,12 or 13
```IndexReader.getSegmentByPathname(String pathname);```
The pathnames should follow the syntax:  [index]:[path]/[file]
Ex: 060000:ui/uld/BuddyChocobo.tex

```
IndexReader.pathToSegment(long pathHash, long fileHash, String index);
The pathHash should be the [path] (without trailing /) hashed with SE's CRC32 algorithm (```see FFCRC.ComputeCRC(byte[] bytes```))
The nameHash should be the [file] (including extension) hashed with SE's CRC32 algorithm (```see  FFCRC.ComputeCRC(byte[] bytes```))
The index should be as stated above
```

FileBlockWrapper fbw = IndexReader.getSegmentByPathname(ftd);
DatSegment segment = fbw.getSegment(true, true);
BufferedImage image = Type4Handler.TextureData.decode(segment);
String outputPath = "temp.png"; //Path to dump file
ImageIO.write(bi, "png", new File(outputPath));







I reserve myself for possible errors in above readme.