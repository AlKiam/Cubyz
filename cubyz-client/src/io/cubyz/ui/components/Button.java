package io.cubyz.ui.components;

import io.cubyz.client.Cubyz;
import io.cubyz.translate.TextKey;
import io.cubyz.ui.Component;
import io.cubyz.ui.NGraphics;
import io.jungle.MouseInput;
import io.jungle.Window;

public class Button extends Component {
	
	private static final int[] button = {
		156, 166, 191, // center
		166, 176, 204, // top
		160, 170, 196, // right
		145, 154, 179, // bottom
		151, 161, 186, // left
	};
	
	private static final int[] buttonPressed = {
		146, 154, 179, // center
		135, 143, 166, // top
		142, 150, 173, // right
		156, 165, 191, // bottom
		150, 159, 184, // left
	};

	private boolean pressed;
	private boolean canRepress = true;
	private Runnable run;
	private float fontSize = 12f;
	private TextKey text;
	
	public Button() {}
	
	public Button(TextKey key) {
		setText(key);
	}
	
	public Button(String text) {
		setText(text);
	}
	
	public TextKey getText() {
		return text;
	}

	public void setText(String text) {
		this.text = new TextKey(text);
	}
	
	public void setText(TextKey text) {
		this.text = text;
	}

	public void setOnAction(Runnable run) {
		this.run = run;
	}
	
	public float getFontSize() {
		return fontSize;
	}

	public void setFontSize(float fontSize) {
		this.fontSize = fontSize;
	}
	
	private void drawTexture(int[] texture) {
		NGraphics.setColor(texture[0], texture[1], texture[2]);
		NGraphics.fillRect(x+5, y+5, width-10, height-10);
		NGraphics.setColor(texture[3], texture[4], texture[5]);
		for(int i = 0; i < 5; i++)
			NGraphics.drawRect(x+i+1, y+i, width-2*i-1, 1);
		NGraphics.setColor(texture[6], texture[7], texture[8]);
		for(int i = 0; i < 5; i++)
			NGraphics.drawRect(x+width-i-1, y+i+1, 1, height-2*i-1);
		NGraphics.setColor(texture[9], texture[10], texture[11]);
		for(int i = 0; i < 5; i++)
			NGraphics.drawRect(x+i, y+height-i-1, width-2*i-1, 1);
		NGraphics.setColor(texture[12], texture[13], texture[14]);
		for(int i = 0; i < 5; i++)
			NGraphics.drawRect(x+i, y+i, 1, height-2*i-1);
	}

	@Override
	public void render(long nvg, Window src) {
		MouseInput mouse = Cubyz.mouse;
		if (mouse.isLeftButtonPressed() && canRepress && isInside(mouse.getCurrentPos())) {
			pressed = true;
			canRepress = false;
		}
		if (!canRepress && !mouse.isLeftButtonPressed()) {
			pressed = false;
			canRepress = true;
			if (isInside(mouse.getCurrentPos())) {
				if (run != null) {
					run.run();
				}
			}
		}
		drawTexture(pressed ? buttonPressed : button);
		NGraphics.setColor(255, 255, 255);
		NGraphics.setFont("Default", fontSize);
		NGraphics.drawText(x + (width / 2) - ((text.getTranslation(Cubyz.lang).length() * 5) / 2), (int) (y + (height / 2) - fontSize / 2), text.getTranslation(Cubyz.lang));
		//int ascent = NGraphics.getAscent("ahh");
	}
	
}