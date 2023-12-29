package edu.ucsf.rbvi.spokeApp.internal.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Image;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.Icon;
import java.io.IOException;

public abstract class IconUtils {
	
	// stringApp Icon
	public static final String SPOKE_ICON = "\uE903";
	// stringApp Icon layers
	public static final String SPOKE_ICON_LAYER_1 = "\uE904";
	public static final String SPOKE_ICON_LAYER_2 = "\uE905";
	public static final String SPOKE_ICON_LAYER_3 = "\uE906";
	public static final String SPOKE_ICON_LAYER_4 = "\uE907";
	// Search Icons -- extra layers
	public static final String SPOKE_LAYER = "\uE90C";
	
	public static final String[] LAYERED_SPOKE_ICON = new String[] { SPOKE_ICON_LAYER_1, SPOKE_ICON_LAYER_2, SPOKE_ICON_LAYER_3 };
	public static final Color[] SPOKE_COLORS = new Color[] { new Color(163, 172, 216), Color.WHITE, Color.BLACK, Color.WHITE, Color.BLACK };
	
	public static final String[] SPOKE_LAYERS = new String[] { SPOKE_ICON_LAYER_1, SPOKE_ICON_LAYER_2, SPOKE_ICON_LAYER_3, SPOKE_ICON_LAYER_4, SPOKE_LAYER };
	
	private static Font iconFont;

	static {
		try {
			iconFont = Font.createFont(Font.TRUETYPE_FONT, IconUtils.class.getResourceAsStream("/fonts/string.ttf"));
		} catch (FontFormatException e) {
			throw new RuntimeException();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
	
	public static Font getIconFont(float size) {
		return iconFont.deriveFont(size);
	}

	public static Icon getImageIcon() {
		try {
			Image image = ImageIO.read(IconUtils.class.getResourceAsStream("/spoke.png"));
			Image scaledImage = image.getScaledInstance(32,96, Image.SCALE_SMOOTH);
			Icon icon = new ImageIcon(scaledImage);
			return icon;
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}

	private IconUtils() {
		// ...
	}
}
