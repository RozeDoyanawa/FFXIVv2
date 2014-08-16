package se.ridorana.roze.stuff.FFXIVDatTools.examples;

import se.ridorana.roze.stuff.FFXIVDatTools.*;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.IOException;

/**
 * Created by Roze on 2014-08-16.
 */
public class BasicTextureRead {
	public static void main(String[] vargs) throws IOException, ImageDecoding.ImageDecodingException {
		String sourceFolder = "C:/games/SquareEnix/FINAL FANTASY XIV - A Realm Reborn/game/sqpack/ffxiv";
		IndexReader.setDefaultSourceFolder(sourceFolder);
		try {
			FileBlockWrapper fbw = IndexReader.pathToSegment("3375652952.1885359672@060000");
			DatSegment segment = fbw.getSegment(true, true);
			if (segment.getType() == Type4Handler.TYPE) {
				ImageIcon ic = new ImageIcon(Type4Handler.TextureData.decode(segment));
				JLabel jl = new JLabel(ic);
				JFrame jf = new JFrame();
				jf.getContentPane().setLayout(new BorderLayout());
				jf.getContentPane().add(jl, BorderLayout.CENTER);
				jf.setResizable(false);
				jf.pack();
				Toolkit tk = Toolkit.getDefaultToolkit();
				Dimension screen = tk.getScreenSize();
				Dimension size = jf.getSize();

				int lx = (int) (screen.getWidth() / 2 - size.getWidth() / 2);
				int ly = (int) (screen.getHeight() / 2 - size.getHeight() / 2);
				jf.setLocation(lx, ly);
				jf.setTitle(IndexReader.FileBlock.getSearchPath(fbw.getFileBlock()));
				jf.setVisible(true);
			}


		} catch (DatSegment.HandlerException e) {
			e.printStackTrace();
		}
		//IndexReader.pathToSegment("");
		//ir.read();
	}
}
