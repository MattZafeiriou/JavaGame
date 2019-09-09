package GameState.Entities;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import GameState.Camera;
import GameState.World;
import GameState.Animations.Animation;
import GameState.Tiles.Tile;
import Input.Keyboard;
import Utils.Assets;
import Utils.Screen;

public class Player {
	
	private enum Keys {
		LEFT,
		RIGHT
	}
	
	private enum State {
		WALKING,
		RUNNING,
		STILL
	}
	
	// useful enums variables
	private Keys lastKey = Keys.RIGHT;
	private State imagesState = State.STILL;

	// useful objects
	private List<BufferedImage> images = new ArrayList<>();
	private List<Dimension> dimensions = new ArrayList<>();
	private Animation animation;
	private Keyboard keyboard;
	private World world;
	private Camera camera;
	private JFrame frame;
	
	// gravity variables
	private boolean falling = false, jumping = false;
	private float kinetic = 20, dynamic = 11.4f;
	// player variables
	private int walkSpeed = 4, runSpeed = 6, imagesWalkSpeed = 5, imagesRunSpeed = 3;
	
	// player dimension variables
	private int x, y, width, height, speed = 4;
	
	public Player(Screen screen, int x, int y, int width, int height, World world, Camera camera) {
		init();
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.frame = screen.getFrame();
		this.keyboard = screen.getKeyboard();
		this.world = world;
		this.camera = camera;
	}
	
	private void init() {
		for (List<BufferedImage> i : Assets.still.keySet()) {
			images = i;
		}
		for (List<Dimension> i : Assets.still.values()) {
			dimensions = i;
		}
		animation = new Animation(images, dimensions, (imagesState == State.RUNNING) ? imagesRunSpeed : imagesWalkSpeed, 2);
	}
	
	public void render(Graphics g) {
		animation.render(g, x - camera.getX(), y - camera.getY(), width, height, lastKey == Keys.LEFT);
	}
	
	public void update(double latency) {
		int speed = (int) (this.speed * latency); // fixed bug left was faster than right when you add the number that time
		
		/*
		 * If both buttons are pressed dont do anything
		 */
		if (!(keyboard.pressed[KeyEvent.VK_D] && keyboard.pressed[KeyEvent.VK_A]) && (keyboard.pressed[KeyEvent.VK_D]
				|| keyboard.pressed[KeyEvent.VK_A]) || keyboard.pressed[KeyEvent.VK_SHIFT]) {
			/*
			 * LEFT or RIGHT or SHIFT are pressed but not both left and right
			 */
			if (keyboard.pressed[KeyEvent.VK_SHIFT])
				changeState(State.RUNNING);
			else
				changeState(State.WALKING);
			if (keyboard.pressed[KeyEvent.VK_D]) {
				lastKey = Keys.RIGHT;
				moveRight(speed);
			}
			if (keyboard.pressed[KeyEvent.VK_A]) {
				lastKey = Keys.LEFT;
				moveLeft(speed);
			}
		} else
			changeState(State.STILL);

		if (keyboard.pressed[KeyEvent.VK_W] && !jumping && !falling) {
			jumping = true;
			kinetic = 35;
			dynamic = 2;
		}
		
		
		if (jumping)
			jump();
		
		updateGravity();
		
		animation.update();
	}
	
	private void changeState(State state) {
		if (imagesState == state)
			return;
		
		else {
			imagesState = state;
			if (state == State.STILL) {
				for (List<BufferedImage> i : Assets.still.keySet()) {
					images = i;
				}
				for (List<Dimension> i : Assets.still.values()) {
					dimensions = i;
				}
				animation.setImages(images, dimensions);
				speed = walkSpeed;
			} else if (state == State.RUNNING) {
				for (List<BufferedImage> i : Assets.walk.keySet()) {
					images = i;
				}
				for (List<Dimension> i : Assets.walk.values()) {
					dimensions = i;
				}
				animation.setImages(images, dimensions);
				speed = runSpeed;
			} else {
				for (List<BufferedImage> i : Assets.walk.keySet()) {
					images = i;
				}
				for (List<Dimension> i : Assets.walk.values()) {
					dimensions = i;
				}
				animation.setImages(images, dimensions);
				speed = walkSpeed;
			}
		}
	}

	private void updateGravity() {
		// check for falling
		try {
			boolean tileFound = false;
			for (int i = 8; i < width - 7; i++) {
				if (world.getTile((x + i) / Tile.getWidth(), getWorldY() + 2).isSolid()) {
					tileFound = true;
				}
			}
			/*
		  	 * Check if player is not jumping and has no tile under their legs
		 	 */
			if (!falling && !jumping && !tileFound) // getWorldY() + 2 because the y is on the top of player's head
				falling = true;
			
			/*
		 	 * check if player has something under their legs
		 	 */
			if (tileFound)
				falling = false;
			
			if (getWorldY() < 0 && !jumping)
				falling = true;
		} catch (NullPointerException | IndexOutOfBoundsException e) {
			/*
			 * Tile is null
			 */
			falling = true;
		}
		
		/*
		 * if player is falling change their position
		 */
		if (falling && !jumping) {
			kinetic+=3;
			if (kinetic >= 80)
				kinetic = 79;
			else
				dynamic-=.2;
			
			// player's Y if we change their position from here
			int playerFinalY = (int)(getWorldY() * Tile.getHeight() + kinetic/dynamic)/Tile.getHeight();
			// if any block found solid between player Y and playerFinalY
			boolean blockFound = false;
			// check for each x of player asset after the 8th x and 7 before
			for (int x1 = 9; x1 <= width - 7; x1++) {
				for (int y1 = 1; y1 <= playerFinalY - getWorldY() +1; y1++) {
					// check if a tile between player and final player y is solid
					if (world.getTile((x+x1)/Tile.getWidth(), (y + speed) / Tile.getHeight() + 2 +y1).isSolid()) {
						y = (getWorldY()+y1) * Tile.getHeight(); // here we dont have +2 because the Y is on the top of player's head and this time we dont check for any blocks under his legs
						falling = false;
						blockFound = true;
					}
				}
			}
			if (!blockFound)
				y+=kinetic/dynamic;
		}
		
		
		/*
		 * Change player's energies
		 */
		if (!falling && !jumping && (dynamic != 11.4 || kinetic != 20)) {
			kinetic = 20;
			dynamic = 11.4f;
		}
	}
	
	private void moveRight(int speed) {
		boolean tileFound = false;
		for (int i = 0; i < height; i++) {
			if (world.getTile((x - 7 + width + speed) / Tile.getWidth(), (y + i) / Tile.getWidth()) != Tile.air)
				tileFound = true;
		}
		if (tileFound)
			return;
		
		if (frame.getWidth() - (x - camera.getX()) <= 170)
			camera.setX(camera.getX() + speed);
		x+=speed;
	}
	
	private void moveLeft(int speed) {
		boolean tileFound = false;
		for (int i = 0; i < height; i++) {
			if (x - camera.getX() - speed + 8 <= 0) // if player - speed is < 0
				tileFound = true;
			
			if (world.getTile((x + 8 - speed) / Tile.getWidth(), (y + i) / Tile.getWidth()) != Tile.air)
				tileFound = true;
		}
		if (tileFound)
			return;
		
		if (x - camera.getX() <= 170)
			camera.setX(camera.getX() - speed);
		x-=speed;
	}
	
	private void jump() {
		if (kinetic <= 0) {
			jumping = false;
			kinetic = 20;
			dynamic = 11.4f;
			falling = true;
			return;
		}
		kinetic-=2;
		dynamic+=.3;
		y-= kinetic/dynamic;
	}
	
	
	public int getWorldX() {
		return x / Tile.getWidth();
	}
	
	public int getWorldY() {
		return y / Tile.getHeight();
	}
	
	
}