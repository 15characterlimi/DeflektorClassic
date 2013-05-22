package pro.oneredpixel.deflektorclassic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;

public class GameState extends State {

	GameState(Deflektor defl) {
		super(defl);
		// TODO Auto-generated constructor stub
	}

	final int GAMESTATE_ACCUMULATING_ENERGY =0;
	final int GAMESTATE_GAMING = 1;
	final int GAMESTATE_CALCULATING_ENERGY = 2;
	final int GAMESTATE_OVERHEAT = 3;
	final int GAMESTATE_LEVELCOMPLETED = 4;
	int gameStateId = GAMESTATE_ACCUMULATING_ENERGY;
	
	final int BEAMSTATE_NORMAL = 0;
	final int BEAMSTATE_OVERHEAT = 1;
	final int BEAMSTATE_BOMB = 2;
	final int BEAMSTATE_CONNECTED = 3;
	int beamState;
	int prevBeamState;
	
	int energy=0;
	int energySteps = 1024;
	int overheat=0;
	final int overheatSteps = 1024;
	
	boolean cursorEnabled = false;
	int cursorPhase = 0;
	final int cursorPhases = 6;
	final int cursorDisplayPhases = 3;
	int cursorX = 0;
	int cursorY = 0;
	
	void create() {
		
	};
	
	void destroy() {
		
	};
	
	//init state for showing
	void start() {
		initGame();
	};
	
	//state stoped
	void stop() {
		playContinuousOverHeatSound(false);
		playContinuousBurningSound(false);
		app.laserFillInSound.stop();
	};
	
	public void render(SpriteBatch batch) {
		//game
		prevBeamState = beamState;
		
		batch.setProjectionMatrix(app.camera.combined);
		batch.begin();
		drawField();
		drawGameInfo();
		batch.end();
		
		// check if we need to create a new raindrop
		if(TimeUtils.nanoTime() - app.lastFrameTime > 100000000) {
			animateField();
		} else return;
		app.lastFrameTime = TimeUtils.nanoTime();
		
		switch (beamState) {
		case BEAMSTATE_NORMAL:
		case BEAMSTATE_CONNECTED:
			playContinuousOverHeatSound(false);
			playContinuousBurningSound(false);
			break;
		case BEAMSTATE_OVERHEAT:
			playContinuousOverHeatSound(true);
			playContinuousBurningSound(false);
			break;
		case BEAMSTATE_BOMB:
			playContinuousOverHeatSound(false);
			playContinuousBurningSound(true);
			break;
		};
		// process user input
		//if(Gdx.input.isTouched()) {
		//	//Vector3 touchPos = new Vector3();
		//	//touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
		//	//camera.unproject(touchPos);
		//	int x = Gdx.input.getX()-winX;
		//	int y = Gdx.input.getY()-winY;
		//	if (x>=0 && x<winWidth && y>=0 && y<winHeight)
		//		touch(x/(sprSize*2)/sprScale, y/(sprSize*2)/sprScale);
		//}
		
		if(Gdx.input.isKeyPressed(Keys.BACK)) app.gotoAppState(Deflektor.APPSTATE_MENU);
	};
	
	boolean playingOverHeatSound=false;
	void playContinuousOverHeatSound(boolean play) {
		if (playingOverHeatSound!=play) {
			if (play) app.laserOverheatSound.loop();
			else app.laserOverheatSound.stop();
			playingOverHeatSound=play;
		};
	}
	
	boolean playingContinuousBurningSound=false;
	void playContinuousBurningSound(boolean play) {
		if (playingContinuousBurningSound!=play) {
			if (play) app.burnBombSound.loop();
			else app.burnBombSound.stop();
			playingContinuousBurningSound=play;
		};
	}
	
	
	//------
	//--- controlling
	//------
	public boolean pinch (Vector2 initialFirstPointer, Vector2 initialSecondPointer, Vector2 firstPointer, Vector2 secondPointer) {
		return false;
	}

	public boolean fling(float arg0, float arg1, int arg2) {
		return false;
	}

	public boolean longPress(float arg0, float arg1) {
		return false;
	}

	public boolean tap(float x, float y, int tapCount, int button) {
		setCursor((int)x,(int)y);
		x=x-app.winX;
		y=y-app.winY;
		if (x>=0 && x<app.winWidth && y>=0 && y<app.winHeight && beamState!=BEAMSTATE_CONNECTED)
			touch(((int)x)/(app.sprSize*2)/app.sprScale, ((int)y)/(app.sprSize*2)/app.sprScale);
		return false;
	}

	float touchX=0;
	float touchY=0;
	int restDelta = 0;
	
	public boolean touchDown(float x, float y, int pointer, int button) {
		touchX = x;
		touchY = y;
		restDelta = 0;
		setCursor((int)x,(int)y);
		return false;
	} 
	
	void setCursor(int x, int y) {
		int newCursorX=(x-app.winX)/(app.sprSize*2)/app.sprScale;
		int newCursorY=(y-app.winY)/(app.sprSize*2)/app.sprScale;	
		
		if (newCursorX>=0 && newCursorX<field_width && newCursorY>=0 && newCursorY<field_height) {
			int f=field[newCursorX+newCursorY*field_width];
			if ((f&0xF00)==MIRR) {
				cursorEnabled=true;
				cursorPhase=0;
				cursorX=newCursorX;
				cursorY=newCursorY;
			};
		};
	}

	public boolean pan(float x, float y, float deltaX, float deltaY) {
		if (cursorEnabled && beamState!=BEAMSTATE_CONNECTED) {
			int delta=(int)Math.sqrt((deltaX)*(deltaX)+(deltaY)*(deltaY));
			if (deltaX<(-deltaY)) delta=-delta;
			delta = delta + restDelta;
			rotateMirror( cursorX, cursorY, (delta/app.panScale)&0x1f);
			restDelta=delta-((int)(delta/app.panScale))*app.panScale;
		};
		return false;
	}
	
	public boolean zoom(float arg0, float arg1) {
		return false;
	}
	
	
	/// game ///
	public final static int field_width = 15;
	public final static int field_height = 9;
	int field[];
	
	final int NULL = 0;
	final int LASR = 0x0100;
	final int RCVR = 0x0200;
	final int MIRR = 0x0300;
	final int WARP = 0x0400;			//(teleport) warpbox will connect you with next warpbox
	final int CELL = 0x500;
	final int MINE = 0x600;				//Mine causes overload and increases overload meter
	final int WL_A = 0x700;			//reflects laser
	final int WL_B = 0x800;			//stops laser
	final int PRSM = 0x900;			//prism turns laser at random
	final int SL_A = 0xa00;			//reflects laser if angle is different - ������
	final int SL_B = 0xb00;			//if the angle is different the laser will stop - �������
	final int _EXPL = 0xc00;			//����� CELL/WALL/MINE ��� ����������-��� ���������� ������
	
	final int FLD_AUTOROTATING = 0x2000;	//autorotate
	final int EXPLODE = 0x4000;		//kill this brick when all cells burned off
	
	int packedLevels[][] = {
		//01
		{
			1024, //energySteps
			MIRR|6,NULL|3,WL_B|0x0a,CELL,CELL,WL_A|0x05,MIRR,NULL|1,SL_B|7|FLD_AUTOROTATING,CELL,CELL,WL_A|0x05,WL_A|0x0b,
			NULL|1,CELL,CELL,CELL,WL_B|0x0a,CELL,CELL,WL_A|0x05,NULL|3,CELL,NULL|2,WL_A|0x05,
			NULL|1,CELL,MIRR|FLD_AUTOROTATING,CELL,WL_B|0x0f,PRSM,WL_A|0x0c,WL_A|0x0c,NULL|4,SL_B|7,NULL|2,
			NULL|1,CELL,CELL,CELL,WL_B|0x0f,NULL|5,WARP|1,NULL|3,MIRR,
			NULL|1,WL_B|0x0c,WL_B|0x0c,WL_B|0x0c,WL_B|0x0f,NULL|2,	SL_A|4|FLD_AUTOROTATING,NULL|1,CELL,NULL|5,
			NULL|2,MIRR,NULL|11,MIRR,
			NULL|11,MINE,SL_B|7, NULL|2,
			MIRR|12,NULL|2,LASR|3,WL_A|0x05,NULL|4,WL_A|0x0d,CELL,MINE,CELL,NULL|1,WARP|1,
			NULL|2,WL_A|0x0a,RCVR|1,WL_A|0x05|EXPLODE,NULL|3,MIRR,WL_A|0x05,CELL,NULL|1,WL_A|0x03,WL_A|0x03,WL_A|0x02,

		},
		//02
		{
			1024, //energySteps
			MIRR,CELL,MINE,NULL,CELL,WL_A|4,NULL|3,CELL,WL_A|1,NULL,MINE,NULL,RCVR|2,
			NULL,WL_A|1,WL_A|4,CELL,WL_A|8,WL_A|4,WL_A|9,NULL,MIRR,NULL|3,WL_A|2,CELL,WL_A|3|EXPLODE,
			WL_A|8,NULL,WL_A|4,WL_A|8,MINE,NULL,WL_A|2,WL_A|2,NULL,WL_A|2,WL_A|8,MINE,WL_A|1,NULL|2,
			NULL,MIRR,WL_A|1,NULL,WL_A|8,WL_A|3,NULL,MINE,NULL|2,CELL,WL_A|8,CELL,WL_A|9,NULL,
			MINE,WL_A|2,CELL,NULL,WL_A|2,NULL,MIRR,NULL,WL_A|8,NULL,WL_A|9,NULL|2,MINE,NULL,
			NULL|2,WL_A|2,NULL,WL_A|4,NULL|2,WL_A|6,NULL,CELL,NULL,WL_A|1,WL_A|4,WL_A|4,WL_A|5,
			LASR|2,CELL,NULL|2,MIRR,NULL,WL_A|2,NULL,WL_A|4,NULL,MIRR,NULL,WL_A|4,NULL|2,
			NULL,WL_A|8,WL_A|6,MINE,NULL,WL_A|1,CELL,WL_A|6,NULL|2,WL_A|2,WL_A|4,MINE,WL_A|2,MIRR,
			MIRR,NULL,WL_A|1,NULL,WL_A|4,NULL,WL_A|1,NULL,MIRR,NULL|2,WL_A|9,WL_A|1,NULL,CELL,
		},
		//03
		{
			1024, //energySteps
			CELL,CELL,WL_A|11,NULL|3,MIRR,NULL,CELL,WL_A|5,NULL|2,CELL,NULL,MIRR,
			CELL,CELL,WL_A|4,WL_A|13,NULL|5,WL_A|5,NULL|3,WL_A|3,NULL,
			CELL,CELL,PRSM,NULL,MIRR,NULL|3,WL_A|3,WL_A|15,WL_A|10,NULL|2,WL_A|10,SL_A,
			CELL,CELL,WL_A|1,WL_A|7,NULL|9,WL_A|11,CELL,
			CELL,CELL,WL_A|14,NULL|4,WL_A|1,WL_A|3,WL_A|2,NULL,MIRR,NULL,WL_A|4,WL_A|12,
			WL_A|12,WL_A|12,WL_A|8,NULL,WL_A|15,WL_A|10,PRSM,WL_B|5|EXPLODE,RCVR|3,WL_A|14,WL_A|12,NULL,SL_A,NULL|2,
			WL_A|2,CELL,NULL|5,WL_A|4,WL_A|12,WL_A|10,MIRR,NULL,WL_A|3,WL_A|7,NULL,
			WL_A|10,NULL|5,SL_A,NULL|2,WL_A|13,NULL|2,WL_A|12,WL_A|12,CELL,
			WL_A|11,WL_A|3,NULL,MIRR,NULL|4,CELL,WL_A|5,LASR|0,WL_A|11,WL_A|3,NULL|2,
			
		},
		//04
		{
			1024, //energySteps
			MINE,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,MIRR,CELL,CELL,CELL,MINE,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,
			CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MINE,CELL,CELL,MINE,
			CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MINE,CELL,CELL,CELL,CELL,CELL,
			RCVR|1,WL_B|0x05|EXPLODE,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,NULL,LASR|3,
			
		},
		//05
		{
			1024, //energySteps
			//MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,
			//NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,
			MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,CELL,MIRR,NULL,RCVR|2,
			CELL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,MINE|EXPLODE,
			MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,MINE,MIRR,NULL,MIRR,CELL,MIRR,
			NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,CELL,MIRR,NULL,
			MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,
			NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,
			MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,
			NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,CELL,
			LASR,NULL,MIRR,NULL,MIRR,CELL,MIRR,MINE,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,
			
		},
		//06
		{
			1024, //energySteps
			LASR|1,NULL,MIRR,NULL,WL_A|10,MIRR,NULL,WL_A|2,MIRR,NULL|5,PRSM,
			WL_A|15,WL_A|3,WL_A|3,NULL|4,WL_A|10,NULL|2,SL_A,NULL,SL_A,CELL,NULL,
			MINE,NULL|4,WL_A|15,NULL,WL_A|10,SL_A,NULL|2,MINE,NULL|2,PRSM,
			WL_A|10,CELL,WL_A|1,NULL|2,WL_A|15,NULL,WL_A|10,CELL,NULL,SL_A,NULL,SL_A,CELL,NULL,
			WL_A|10,NULL,WL_A|5,NULL,CELL,NULL|2,WL_A|11,WL_A|3,WL_A|2,NULL|4,PRSM,
			WL_A|10,CELL,WL_A|15,NULL|2,CELL,NULL|2,CELL,WL_A|10,SL_A,CELL,SL_A,CELL,NULL,
			WL_A|10,NULL|3,WL_A|5,WL_A|12,WL_A|12,NULL|2,WL_A|10,CELL,NULL|3,PRSM,
			WL_A|10,NULL|6,MIRR,NULL,WL_A|10,SL_A,CELL,SL_A,CELL,WL_B|3|EXPLODE,
			WL_A|15,NULL,MIRR,NULL|5,MINE,WL_A|11,WL_A|3,WL_A|3,WL_A|3,WL_A|3,RCVR,
			
		},
		//07
		{
			1024, //energySteps
			CELL,NULL,MIRR,NULL,WL_A|12,WL_A|8,MIRR,NULL|3,MIRR,WL_A|5,RCVR|1,WL_B|15|EXPLODE,MIRR,
			WL_A|14,WL_A|12,NULL|2,CELL,NULL|2,WL_A|15,NULL,WL_A|13,NULL,WL_A|5,WL_A|12,WL_A|13,NULL,
			NULL,CELL,NULL|3,WL_A|3,NULL|2,CELL,WL_A|5,NULL|2,CELL,WL_A|5,NULL,
			WL_A|3,WL_A|7,WL_A|12,NULL,WL_A|5,WL_A|10,NULL,WL_A|13,WL_A|2,NULL|2,MIRR,NULL,WL_A|15,NULL,
			NULL|2,MIRR,NULL|3,CELL,WL_A|5,WL_A|14,WL_A|12,NULL|3,WL_A|12,NULL,
			NULL|2,WL_A|3,WL_A|3,CELL,NULL|3,WL_A|12,NULL,CELL,NULL,WL_A|5,NULL|2,
			MIRR,NULL|2,WL_A|4,WL_A|10,NULL,WL_A|5,NULL|2,WL_A|10,WL_A|15,NULL,WL_A|5,NULL,MIRR,
			WL_A|3,WL_A|3,NULL|2,WL_A|10,CELL,WL_A|5,NULL,MIRR,NULL|4,CELL,NULL,
			LASR|1,NULL,MIRR,NULL,WL_A|11,WL_A|3,WL_A|14,CELL,NULL|2,WL_A|3,WL_A|3,WL_A|3,WL_A|3,WL_A|15,
		},
		//08
		{
			1024, //energySteps
			LASR|2,WL_B|5,WL_B|15,WL_A|15,WL_A|12,WL_A|12,WL_A|12,WL_A|13,WL_A|10,NULL,WL_A|12,NULL,MINE,NULL,WARP|0,
			NULL,WL_B|5,RCVR|1,WL_A|5|EXPLODE,CELL,CELL,CELL,WL_A|4,WL_A|8,NULL|3,MIRR,NULL|2,
			NULL,WL_B|5,WL_B|15,WL_A|14,CELL,MINE,CELL,CELL,WL_A|13,NULL|6,
			NULL|2,WL_A|15,WL_A|10,MINE,MINE,MINE,CELL,WL_A|5,WL_A|3,NULL|5,
			MIRR,NULL,PRSM,NULL,CELL,MINE,CELL,MINE,NULL,PRSM,NULL|4,MIRR,
			NULL|2,WL_A|12,WL_A|10,MINE,CELL,MINE,CELL,WL_A|5,WL_A|12,NULL|4,MIRR,
			NULL|3,WL_A|11,WL_A|3,WL_A|3,WL_A|3,WL_A|3,WL_A|7,NULL|4,WL_B|1,WL_B|3,
			NULL|11,WL_B|7,NULL,WL_B|12,CELL,
			NULL|2,MIRR,NULL,MINE,MIRR,MINE,NULL,MIRR,NULL|2,WL_B|10,WARP|0,WL_B|1,WL_B|15,
		},
		//09
		{
			1024, //energySteps
			MINE,CELL,CELL,PRSM,CELL,CELL,CELL,LASR|2,CELL,CELL,CELL,PRSM,CELL,CELL,MINE,
			CELL,CELL,CELL,CELL,MINE,CELL,CELL,NULL,CELL,CELL,MINE,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,MINE,CELL,CELL,WL_B|3|EXPLODE,CELL,CELL,MINE,CELL,CELL,CELL,CELL,
			MINE,CELL,CELL,PRSM,CELL,CELL,CELL,RCVR|0,CELL,CELL,CELL,PRSM,CELL,CELL,MINE,
		},
		//10
		{
			1024, //energySteps
			MIRR,CELL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,MINE,MIRR,NULL,MIRR,
			NULL,MIRR,NULL,MIRR,CELL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,MINE,
			MIRR,CELL,MIRR,MINE,MIRR,NULL,MIRR,NULL,MIRR,MINE,MIRR,CELL,MIRR,CELL,MIRR,
			NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,CELL,
			MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,
			NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,
			MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,
			NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,CELL,MIRR,NULL,MIRR,MINE|EXPLODE,
			LASR|0,NULL,MIRR,NULL,MIRR,CELL,MIRR,MINE,MIRR,NULL,MIRR,CELL,MIRR,NULL,RCVR|0,
		},
		//11
		{
			1024, //energySteps
			RCVR|2,NULL|3,MIRR,NULL,WARP|0,NULL,CELL,NULL|2,MINE,NULL,MINE,NULL,
			MINE|EXPLODE,NULL,CELL,WL_A|11,WL_A|3,NULL|3,WL_A|14,MINE,NULL|2,CELL,NULL|2,
			MIRR,NULL,SL_A,NULL,WL_A|10,NULL|3,WL_A|10,NULL,CELL,MINE,NULL|2,CELL,
			NULL,MIRR,NULL,CELL,WL_A|8,NULL|4,CELL,NULL|3,MINE,WL_A|5,
			WL_A|3,WL_A|3,WL_A|3,WL_A|10,SL_A,NULL,MIRR,NULL,WL_A|10,NULL,WL_A|10,NULL|3,WL_A|5,
			CELL,CELL,CELL,WL_A|10,NULL|4,WL_A|12,NULL,WL_A|13,NULL,MIRR,NULL,WL_A|5,
			CELL,PRSM,CELL,WL_A|15,NULL|2,WL_A|11,NULL,SL_A,NULL|5,WL_A|15,
			NULL|3,WL_A|10,NULL,WL_A|10,WL_A|5,NULL|2,WL_A|5,WL_A|3,NULL|2,WL_A|12,WL_A|15,
			NULL,WARP|0,NULL,WL_A|10,MIRR,CELL,WL_A|5,NULL,MIRR,NULL|3,MIRR,NULL,LASR|3,
		},		
		//12
		{
			1024, //energySteps
			MIRR,NULL,MINE,NULL,CELL,NULL|2,MINE,NULL|2,MIRR,NULL|2,CELL,NULL,
			NULL,WL_A|15,NULL,CELL,WL_A|12,WL_A|12,WL_A|12,NULL,MIRR,CELL,NULL|3,MINE,CELL,
			LASR|1,NULL|3,MIRR,NULL,MIRR,NULL|4,MIRR,NULL|3,
			NULL,MINE,NULL|5,WL_A|2,NULL|2,WARP|0,NULL|3,MIRR,
			WL_B|15,WL_A|10,MIRR,NULL|2,CELL,NULL,WL_A|10,SL_B,SL_B,NULL|2,MINE,NULL|2,
			CELL,NULL|6,WL_A|10,NULL|5,WL_B|14,WL_B|12,
			CELL,NULL,MIRR,NULL,SL_B,NULL|2,WL_A|15,NULL|2,MIRR,NULL|2,WL_A|10|EXPLODE,RCVR|3,
			MIRR,NULL|5,WARP|0,NULL,MIRR,NULL,CELL,NULL|2,WL_B|11,WL_B|3,
			MINE,NULL,MIRR,NULL|2,MINE,NULL|2,CELL,WL_B|3,WL_B|3,MIRR,NULL|2,CELL,
		},		
		//13
		{
			1024, //energySteps
			NULL,CELL,NULL,WL_A|3,WL_A|15,NULL,WL_A|10,NULL,CELL,WL_A|3,WL_A|14,NULL,CELL,WL_A|15,LASR|2,
			MIRR,NULL,CELL,WL_A|10,NULL|2,WL_A|10,MIRR,NULL,WL_A|10,NULL,WL_A|7,WL_A|3,WL_A|15,NULL,
			NULL|3,WL_A|10,MIRR,NULL,WL_A|10,NULL|2,WL_A|10,MIRR,WL_A|1,NULL|2,MIRR,
			NULL|3,WL_A|10,NULL|2,WL_A|8,WL_A|5,NULL,WL_A|3,NULL,WL_A|5,NULL,WL_A|15,WL_A|3,
			NULL,WL_A|10,NULL|2,WL_A|3,NULL|2,WL_A|11,WL_A|2,WL_A|5,NULL|3,WL_A|5,WL_A|12,
			NULL,WL_A|10,NULL|2,WL_A|10,SL_A,NULL|2,WL_A|10,WL_A|5,NULL,WL_A|4,NULL|2,MIRR,
			NULL,WL_A|15,WL_A|5,WL_A|12,WL_A|8,NULL|3,WL_A|10,WL_A|5,NULL,PRSM,WL_A|5,WL_A|13,NULL,
			SL_B|1|EXPLODE,WL_A|15,WL_A|12,NULL,MIRR,NULL,CELL,WL_A|15,WL_A|10,WL_A|5,NULL,CELL,CELL,WL_A|5,NULL,
			RCVR,WL_A|15,NULL,CELL,NULL|2,WL_A|5,CELL,MIRR,WL_A|5,NULL,CELL,CELL,WL_A|5,CELL,
		},		
		//14
		{
			1024, //energySteps
			LASR|2,RCVR|1,WL_B|5|EXPLODE,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,MINE,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,
			MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,
			CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,PRSM,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			MINE,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MINE,
		},		
		//15
		{
			1024, //energySteps
			MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,
			NULL,MIRR,NULL,MIRR,CELL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,
			MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,MINE,MIRR,CELL,MIRR,CELL,MIRR,
			CELL,MIRR,NULL,MIRR,NULL,MIRR,MINE|EXPLODE,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,CELL,
			MIRR,NULL,MIRR,CELL,MIRR,NULL,RCVR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,
			NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,
			MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,
			NULL,MIRR,CELL,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,CELL,
			LASR,NULL,MIRR,NULL,MIRR,CELL,MIRR,MINE,MIRR,NULL,MIRR,CELL,MIRR,MINE,MIRR,
		},		
		//16
		{
			1024, //energySteps
			WL_A|10,NULL|2,WL_A|5,CELL,NULL,CELL,WL_A|10,WARP|3,WL_B|15,WARP|2,NULL,WL_B|7,MINE,CELL,
			WL_A|10,NULL,CELL,WL_A|4,SL_B,NULL|2,WL_A|10,NULL,WARP|0,NULL|2,PRSM,CELL,MINE,
			WL_A|10,WL_A|5,NULL,WL_A|1,WL_A|5,NULL,WL_A|5,WL_A|8,WL_A|15,WL_A|3,NULL,CELL,WL_B|13,MINE,CELL,
			WL_A|10,WL_A|5,NULL,WL_A|5,NULL|2,WL_A|5,NULL|2,WL_A|10,NULL,MINE,WL_B|4,WL_B|15,WL_B|15,
			WL_A|10,WL_A|5,NULL,WL_A|7,NULL|2,WL_A|5,WL_A|3,NULL,WL_A|10,MIRR,NULL|2,SL_B|EXPLODE,RCVR|3,
			WL_A|10,CELL,NULL,WL_A|10,NULL|3,WL_A|5,NULL,WL_A|13,WL_A|3,WL_A|14,WL_A|12,WL_A|12,WL_A|13,
			WL_A|10,NULL,CELL,WL_A|10,NULL|5,WL_A|5,LASR|1,NULL,MIRR,NULL,WARP|0,
			WARP|1,NULL|2,WL_A|10,PRSM,NULL|2,MIRR,NULL,WL_A|5,NULL|4,WL_A|5,
			NULL,MIRR,NULL,WL_A|11,WL_A|3,WL_A|3,NULL|2,CELL,WL_A|5,WARP|3,WL_A|3,WARP|2,WL_A|3,WARP|1,
			
		},		
		//17
		{
			1024, //energySteps
			RCVR|1,SL_A|EXPLODE,NULL,SL_A,SL_A,NULL|3,CELL,NULL|4,MIRR,NULL,
			NULL|2,CELL,NULL|3,MINE,SL_A,SL_A,SL_A,CELL,NULL,SL_A,NULL,SL_A,
			MINE,SL_A,SL_A,SL_A,NULL,MIRR,NULL|5,MIRR,NULL|2,SL_A,
			SL_A,NULL|6,SL_A,NULL,MIRR,NULL|2,CELL,NULL|2,
			NULL|3,MIRR,NULL,SL_A,NULL|2,CELL,NULL|3,SL_A,SL_A,MINE,
			NULL,SL_A,NULL|3,SL_A,NULL,MIRR,NULL,SL_A,SL_A,CELL,NULL|3,
			CELL,NULL|4,SL_A,NULL|4,SL_A,NULL|2,MIRR,CELL,
			SL_A,CELL,NULL|2,MINE,NULL|3,SL_A,NULL|6,
			SL_A,NULL,SL_A,LASR,NULL|2,CELL,NULL|2,MINE,SL_A,CELL,MIRR,NULL,SL_A,
			
		},	
		//18
		{
			1024, //energySteps
			LASR|1,NULL|2,MIRR,WL_A|5,WL_A|8,CELL,WL_A|4,WL_A|15,WL_A|10,RCVR|1,WL_A|5|EXPLODE,CELL,NULL,MIRR,
			WL_A|12,WL_A|13,WL_A|11,NULL,WL_A|7,NULL,MIRR,NULL,WL_A|15,CELL,NULL,WL_A|4,WL_A|15,WL_A|11,NULL,
			MIRR,NULL|3,WL_A|13,NULL|3,WL_A|12,NULL|2,CELL,NULL,WL_A|15,NULL,
			NULL|3,MIRR,NULL,CELL,NULL|2,MIRR,NULL,WL_A|15,WL_A|15,NULL,WL_A|13,NULL,
			NULL,WL_A|14,NULL|2,WL_A|1,WL_A|15,NULL,WL_A|2,NULL|3,WL_A|13,WL_A|11,NULL,CELL,
			NULL,WL_A|10,MIRR,NULL,WL_A|7,WL_A|14,WL_A|12,WL_A|8,CELL,NULL,MIRR,NULL,WL_A|15,NULL|2,
			CELL,WL_A|11,WL_A|3,NULL|3,MIRR,NULL|2,WL_A|11,NULL|2,WL_A|15,NULL,MIRR,
			WL_A|7,WL_A|14,CELL,NULL,WL_A|15,WL_A|11,WL_A|3,WL_A|2,NULL,WL_A|13,NULL|2,CELL,NULL|2,
			WL_A|15,WL_A|10,MIRR,NULL,CELL,NULL,CELL,NULL,MIRR,WL_A|5,WL_A|3,WL_A|3,WL_A|3,WL_A|3,WL_A|7,
		},	
		//19
		{
			1024, //energySteps
			CELL,CELL,CELL,CELL,MINE,CELL,WL_A|5,WL_A|15,LASR|1,NULL,NULL,MIRR,CELL,CELL,MINE,
			MIRR,CELL,CELL,CELL,CELL,MINE,WL_A|5,CELL,MINE,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,MIRR,CELL,CELL,WL_A|10|EXPLODE,RCVR|3,WL_A|15,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MINE,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,MINE,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,MINE,CELL,CELL,CELL,
			MINE,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,
		},
		//20
		{
			1024, //energySteps
			MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,MINE,MIRR,
			NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,
			LASR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,MINE,MIRR,CELL,MIRR,CELL,MIRR,
			NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,
			RCVR|2,NULL,MIRR,CELL,MIRR,NULL,MIRR,MINE,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,
			MINE|EXPLODE,MIRR,NULL,MIRR,MINE,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,CELL,
			MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,
			NULL,MIRR,CELL,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,CELL,
			MIRR,MINE,MIRR,MINE,MIRR,CELL,MIRR,MINE,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,
		},
		//21
		{
			1024, //energySteps
			PRSM,NULL|11,CELL,WL_B|5|EXPLODE,RCVR|3,
			NULL|8,PRSM,NULL|2,CELL,NULL|3,
			MIRR,NULL|2,PRSM,NULL,CELL,NULL,CELL,NULL,CELL,NULL|3,MIRR,NULL,
			NULL|6,WL_B|15,WL_B|15,WL_B|15,NULL|5,CELL,
			NULL,CELL,NULL|13,
			NULL|2,PRSM,NULL|7,PRSM,NULL|2,CELL,NULL,
			NULL|7,PRSM,NULL|3,MIRR,NULL|3,
			NULL|4,MIRR,NULL|8,PRSM,NULL,
			MIRR,NULL|2,CELL,NULL|3,LASR,NULL|2,CELL,NULL|3,CELL,
		},
		//22
		{
			1024, //energySteps
			WL_A|15,WL_A|12,WL_A|12,WL_A|12,WL_A|12,WL_A|12,WL_A|10,WL_B|15,WL_A|12,WL_A|12,WL_A|15,NULL,SL_A,CELL,SL_A,
			NULL|4,WARP|0,NULL,WL_A|10,NULL,WL_A|5,NULL|3,CELL,CELL,CELL,
			PRSM,NULL,PRSM,NULL|2,MIRR,WL_A|10,MIRR,WL_A|5,NULL|3,SL_A,CELL,SL_A,
			CELL,NULL|3,WL_A|5,WL_A|12|EXPLODE,WL_A|10,NULL,WL_A|5,NULL,MIRR,NULL|3,CELL,
			PRSM,CELL,PRSM,NULL,WL_A|5,RCVR|0,WL_A|15,LASR|0,WL_A|15,NULL|3,SL_A,NULL,SL_A,
			CELL,NULL|3,WL_A|4,WL_A|12,WL_A|12,WL_A|12,WL_A|8,NULL|5,CELL,
			PRSM,CELL,PRSM,NULL,MIRR,WL_A|5,WL_A|3,WL_A|14,NULL|2,WL_A|15,NULL,SL_A,NULL,SL_A,
			CELL,NULL,CELL,NULL|4,WL_A|10,MIRR,NULL,WL_A|15,NULL|4,
			PRSM,CELL,PRSM,CELL,NULL|2,WARP|0,NULL|3,WL_A|15,WL_A|3,WL_A|3,WL_A|3,WL_A|7,
		},
		//23
		{
			1024, //energySteps
			WL_B|15,CELL,NULL,MIRR,NULL,CELL,NULL|3,CELL,NULL,MIRR,NULL,CELL,WL_B|15,
			WL_B|15,NULL,CELL,NULL,SL_B,NULL,WL_B|15,MINE,WL_B|15,NULL,SL_B,NULL,CELL,NULL,WL_B|15,
			WL_B|15,WL_B|13,NULL|5,SL_B,NULL|5,WL_B|14,WL_B|15,
			WL_B|4,WL_B|13,WL_B|2,MINE,NULL|2,SL_B,NULL,SL_B,NULL|2,MINE,WL_B|1,WL_B|14,WL_B|8,
			SL_A,WL_B|5,LASR|1,NULL,PRSM,NULL|2,MIRR,NULL|2,PRSM,NULL,MINE,WL_B|10,SL_A,
			WL_B|1,WL_B|7,WL_B|8,MINE,NULL|2,SL_B,NULL,SL_B,NULL|2,MINE,WL_B|4,WL_B|11,WL_B|2,
			WL_B|15,WL_B|7,NULL|5,SL_B,NULL|5,WL_B|11,WL_B|15,
			WL_B|15,NULL,CELL,NULL,SL_B,NULL,WL_B|15,MINE|EXPLODE,WL_B|15,NULL,SL_B,NULL,CELL,NULL,WL_B|15,
			WL_B|15,CELL,NULL,MIRR,NULL,CELL,NULL,RCVR|0,NULL,CELL,NULL,MIRR,NULL,CELL,WL_B|15,
		},
		//24
		{
			1024, //energySteps
			MINE,CELL,CELL,CELL,CELL,CELL,CELL,LASR|2,CELL,CELL,CELL,MIRR,CELL,CELL,MINE,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,NULL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			MIRR,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,MIRR,CELL,CELL,WL_B|14,WL_B|12|EXPLODE,WL_B|13,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,WL_B|10,RCVR|0,WL_B|5,CELL,MIRR,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,WL_B|11,WL_B|3,WL_B|7,CELL,CELL,CELL,CELL,CELL,CELL,
			MIRR,CELL,CELL,CELL,CELL,CELL,CELL,WL_B|15,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,
			MINE,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MINE,
		},
		//25
		{
			1024, //energySteps
			MIRR,CELL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,MINE,MIRR,NULL,MIRR,CELL,MIRR,
			NULL,MIRR,MINE,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,MINE,
			MIRR,MINE,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,MINE,MIRR,CELL,MIRR,CELL,MIRR,
			NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,CELL,
			LASR|1,NULL,MIRR,CELL,MIRR,NULL,MIRR,MINE,MIRR,NULL,MIRR,NULL,MIRR,MINE|EXPLODE,RCVR|3,
			NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,
			MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,
			NULL,MIRR,MINE,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,MINE,MIRR,NULL,
			MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,MINE,MIRR,NULL,MIRR,CELL,MIRR,CELL,MIRR,
		},
		//26
		{
			1024, //energySteps
			RCVR|1,WL_B|5|EXPLODE,NULL,WARP|0,NULL|2,CELL,NULL|2,MIRR,WL_A|12,WL_A|12,WL_A|12,WL_A|12,CELL,
			WL_B|15,WL_B|12,WL_B|8,NULL,WL_B|4,WL_B|12,WL_B|15,WL_B|15,WL_B|10,NULL,SL_B,CELL,SL_B,NULL,WL_A|5,
			MINE,MIRR,CELL,PRSM,CELL,MIRR,MINE,WL_B|5,WL_B|10,SL_B,NULL|3,MIRR,WL_A|4,
			MIRR,CELL,MIRR,CELL,MIRR,CELL,MIRR,WL_B|5,NULL|2,WL_A|1,WL_A|3,WL_A|3,WL_A|3,WL_A|2,
			CELL,MIRR,CELL,MIRR,CELL,MIRR,CELL,WL_B|5,NULL|2,WL_A|7,NULL|2,CELL,WL_A|11,
			MIRR,CELL,MIRR,MINE,MIRR,CELL,MIRR,WL_B|5,MIRR,NULL|5,WL_A|5,
			MINE,MIRR,MINE,MIRR,CELL,MIRR,CELL,WL_B|5,CELL,WL_A|3,WL_A|3,WL_A|3,WL_A|2,NULL,WL_A|5,
			MIRR,CELL,MIRR,CELL,MIRR,MINE,MIRR,WL_B|5,NULL|6,WL_A|7,
			CELL,MIRR,MINE,MIRR,CELL,MIRR,MINE,WL_B|15,WARP|0,NULL|3,MIRR,NULL,LASR|3,
		},
		//27
		{
			1024, //energySteps
			WL_B|14,WL_B|12,WL_B|8,MIRR,NULL,MIRR,WL_B|4,WL_B|12,WL_B|8,MIRR,NULL,MIRR,WL_B|4,WL_B|12,WL_B|13,
			WL_B|10,SL_B,NULL|5,RCVR|2,NULL|5,SL_B,WL_B|5,
			WL_B|10,NULL|6,WL_B|3|EXPLODE,NULL|6,WL_B|5,
			NULL|15,
			MIRR,MIRR,MIRR,MIRR,MIRR,MIRR,MIRR,MIRR,MIRR,MIRR,MIRR,MIRR,MIRR,MIRR,MIRR,
			NULL|6,MINE,NULL,MINE,NULL|6,
			WL_B|10,NULL|13,WL_B|5,
			WL_B|10,NULL|13,WL_B|5,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,LASR|0,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
		},
		//28
		{
			1024, //energySteps
			MIRR,MIRR,NULL|2,WARP|3,WL_A|5,NULL,MIRR,MIRR,NULL|3,MIRR,WL_A|5,CELL,
			MIRR,MIRR,NULL|3,WL_A|5,NULL,MIRR,MIRR,NULL|2,WARP|1,WL_A|1,WL_A|6,CELL,
			WL_A|3,WL_A|3,NULL|2,CELL,WL_A|5,NULL|3,WARP|0,NULL|3,CELL,CELL,
			CELL,WL_A|4,NULL|3,WL_A|14,WL_A|12,WL_A|13,WL_A|3,WL_A|7,WL_A|12,WL_A|12,WL_A|12,WL_A|12,WL_A|12,
			RCVR|1,MINE|EXPLODE,NULL|2,MIRR,WL_A|10,WARP|1,NULL|2,WL_A|10,WARP|0,CELL,CELL,CELL,CELL,
			WL_A|12,WL_A|12,WL_A|12,WL_A|12,WL_A|12,MIRR,MIRR,NULL|2,WL_A|10,CELL,NULL|4,
			MINE,CELL,NULL,WL_A|5,NULL,MIRR,MIRR,NULL|2,WL_A|10,MINE,NULL|4,
			WL_A|5,WL_A|12,WL_A|12,WL_A|12,NULL|5,WL_A|10,CELL,NULL|2,MIRR,MIRR,
			LASR|1,NULL,MIRR,NULL|6,WL_A|10,WARP|3,NULL|2,MIRR,MIRR,
		},
		//29
		{
			1024, //energySteps
			MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MINE,CELL,CELL,CELL,CELL,MIRR,
			CELL,MINE,CELL,CELL,MINE,CELL,CELL,CELL,MINE,MIRR,MINE,CELL,CELL,CELL,CELL,
			RCVR|2,CELL,CELL,MINE,CELL,CELL,CELL,CELL,CELL,MINE,CELL,CELL,CELL,CELL,CELL,
			WL_A|3|EXPLODE,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			MIRR,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,
			NULL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,
			LASR|0,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,

		},
		//30
		{
			1024, //energySteps
			MIRR,CELL,MIRR,MINE,MIRR,MINE,MIRR,MINE,MIRR,CELL,MIRR,MINE,MIRR,MINE,MIRR,
			MINE,MIRR,NULL,MIRR,CELL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,MINE,
			MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,CELL,MIRR,CELL,MIRR,
			CELL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,NULL,MIRR,MINE,
			MIRR,NULL,MIRR,NULL,MIRR,NULL,LASR,NULL,RCVR|1,MINE|EXPLODE,MIRR,NULL,MIRR,NULL,MIRR,
			MINE,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,MINE,
			MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,NULL,MIRR,CELL,MIRR,
			MINE,MIRR,CELL,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,NULL,MIRR,CELL,MIRR,CELL,
			MIRR,CELL,MIRR,MINE,MIRR,MINE,MIRR,MINE,MIRR,MINE,MIRR,CELL,MIRR,MINE,MIRR,
		},
		//31
		{
			1024, //energySteps
			NULL,WL_A|12,WL_A|12,WL_A|13,LASR|2,WL_B|15,MIRR,NULL|5,MIRR,WL_A|5,MINE,
			CELL,WL_A|15,WL_A|15,WL_A|3,NULL,WL_B|15,NULL,WL_B|15,NULL|5,WL_A|5,CELL,
			NULL|4,MIRR,WL_B|15,WARP|0|EXPLODE,WL_B|15,WARP|1,NULL|3,MIRR,WL_A|5,CELL,
			WL_A|12,WL_A|12,WL_A|12,WL_A|10,NULL,WL_B|5,RCVR,WL_B|10,NULL|5,WL_A|5,CELL,
			MINE,CELL,CELL,WARP|0,MINE,WL_B|4,WL_B|12,WL_B|8,NULL,CELL,NULL|4,SL_A,
			CELL,CELL,CELL,PRSM,WARP|1,WL_A|3,NULL|3,SL_B,NULL,SL_B,NULL|3,
			MINE,CELL,CELL,CELL,CELL,WL_A|5,NULL|5,CELL,NULL,CELL,NULL,
			CELL,MINE,CELL,CELL,CELL,WL_A|5,NULL|2,MIRR,NULL,SL_B,NULL,SL_B,NULL|2,
			MINE,CELL,MINE,CELL,MINE,WL_A|5,NULL,MIRR,NULL|6,MIRR,
		},
		//32
		{
			1024, //energySteps
			CELL,NULL|2,WL_A|9,NULL,WL_A|8,MIRR,NULL,WL_A|8,NULL,WL_A|2,NULL,LASR|1,NULL,MIRR,
			NULL,WL_A|4,MINE,WL_A|2,WL_A|4,NULL|2,WL_A|2,CELL,WL_A|8,NULL,MINE,WL_A|6,WL_A|1,NULL,
			NULL,MIRR,NULL|2,WL_A|4,NULL,WL_A|2,NULL,WL_A|4,NULL,WL_A|9,NULL|2,CELL,NULL,
			WL_A|10,WL_A|2,WL_A|2,WL_A|8,CELL,NULL|2,WL_A|6,NULL,WL_A|8,NULL|2,WL_A|4,NULL|2,
			NULL,MINE,NULL|2,WL_A|9,NULL,WL_A|1,NULL|3,MIRR,NULL,CELL,WL_A|4,MINE,
			WL_A|2,WL_A|6,CELL,WL_A|1,CELL,NULL|2,MINE,NULL,WL_A|8,WL_A|1,NULL,WL_A|8,MIRR,NULL,
			CELL,NULL,WL_A|1,MINE,WL_A|1,WL_A|4,NULL,WL_A|4,NULL|3,WL_A|1,WL_A|2,NULL,WL_A|4,
			MINE,MIRR,WL_A|4,NULL|3,MIRR,NULL,WL_A|9,WL_A|4,WL_A|1,CELL,WL_A|2,WL_A|8,MIRR,
			MIRR,NULL,WL_A|4,CELL,WL_A|8,CELL,NULL,MINE|EXPLODE,RCVR|3,NULL,CELL,NULL,MINE,CELL,NULL,
		},
		//33
		{
			1024, //energySteps
			RCVR|2,WL_A|12,WL_A|12,WL_A|12,WL_A|12,WL_A|13,MINE,NULL|5,MIRR,NULL,WL_A|15,
			WL_B|12|EXPLODE,CELL,SL_B,CELL,MINE,WL_A|5,NULL,MIRR,NULL|6,WL_A|5,
			PRSM,NULL|2,MINE,MINE,WL_A|5,NULL|2,WL_A|3,WL_A|3,WL_A|10,NULL|3,WL_A|5,
			NULL,CELL,SL_B,MINE,SL_B,WL_A|5,CELL,NULL|2,CELL,NULL|2,WL_A|14,PRSM,WL_A|12,
			PRSM,NULL|4,WL_A|4,WL_A|12,WL_A|8,NULL|2,CELL,NULL,WL_A|10,CELL,CELL,
			NULL,CELL,SL_B,CELL,SL_B,NULL,CELL,WL_A|5,NULL,WL_A|15,NULL|2,WL_A|8,CELL,CELL,
			PRSM,NULL|2,MINE,NULL|2,SL_B,WL_A|5,NULL,WL_A|15,NULL|3,MINE,MINE,
			CELL,CELL,PRSM,NULL,SL_B,NULL|2,WL_A|5,NULL|4,WL_A|12,WL_A|12,WL_A|15,
			CELL,CELL,NULL|4,MIRR,WL_A|4,NULL,MIRR,WL_A|5,NULL,MIRR,NULL,LASR|3,
		},
		//34
		{
			1024, //energySteps
			MINE,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MINE|EXPLODE,RCVR|3,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MINE,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,MINE,
			CELL,MIRR,CELL,CELL,CELL,CELL,LASR|2,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			CELL,CELL,CELL,CELL,CELL,CELL,NULL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,CELL,
			MINE,CELL,CELL,CELL,CELL,CELL,MIRR,CELL,CELL,CELL,CELL,CELL,CELL,CELL,MINE,
			
		},
		//35
		{
			1024, //energySteps
		},
		//36
		{
			1024, //energySteps
		},
		//37
		{
			1024, //energySteps
		},
		//38
		{
			1024, //energySteps
		},
		//39
		{
			1024, //energySteps
		},
		//40
		{
			1024, //energySteps
		},
		//41
		{
			1024, //energySteps
		},
		//42
		{
			1024, //energySteps
		},
		//43
		{
			1024, //energySteps
		},
		//44
		{
			1024, //energySteps
		},
		//45
		{
			1024, //energySteps
		},
		//46
		{
			1024, //energySteps
		},
		//47
		{
			1024, //energySteps
		},
		//48
		{
			1024, //energySteps
		},
		//49
		{
			1024, //energySteps
		},
		//50
		{
			1024, //energySteps
		},
		//51
		{
			1024, //energySteps
		},
		//52
		{
			1024, //energySteps
		},
		//53
		{
			1024, //energySteps
		},
		//54
		{
			1024, //energySteps
		},
		//55
		{
			1024, //energySteps
		},
		//56
		{
			1024, //energySteps
		},
		//57
		{
			1024, //energySteps
		},
		//58
		{
			1024, //energySteps
		},
		//59
		{
			1024, //energySteps
		},
		//60
		{
			1024, //energySteps
		},
		
		
	};

	

	
	
	//������������ ���� ����
	int angleNodeSteps[][]={
		{0,-2}, //0
		{1,-2}, //1
		{2,-2}, //2
		{2,-1}, //3
		{2,0},  //4
		{2,1}, //5
		{2,2}, //6
		{1,2}, //7
		{0,2}, //8
		{-1,2}, //9
		{-2,2}, //10
		{-2,1}, //11
		{-2,0}, //12
		{-2,-1},//13
		{-2,-2},//14
		{-1,-2},//15
	};
	
	public void initGame() {
		
		beamState = BEAMSTATE_NORMAL;
		gameStateId = GAMESTATE_ACCUMULATING_ENERGY;
		energy=0;
		overheat=0;
		cursorEnabled = false;		
		
		field=new int[field_width*field_height];
		
		unpackLevel(app.playingLevel);
	}
	
	void unpackLevel(int levelNumber) {
		int piece;
		int fieldIndex=0;
		levelNumber--;
		if (levelNumber>=packedLevels.length) return;
		
		energySteps = packedLevels[levelNumber][0];
		
		for (int i=1;i<packedLevels[levelNumber].length;i++) {
			piece = packedLevels[levelNumber][i];
			if ((piece&0xFF00)==NULL) {
				piece&=0xFF;
				if (piece<1) piece=1;
				for (int n=0;n<piece;n++) {
					field[fieldIndex++]=NULL;
					if (fieldIndex>=field_width*field_height) return;
				}
			} else {
				field[fieldIndex++] = piece;
				if (fieldIndex>=field_width*field_height) return;
			}
		}
		while (fieldIndex<field_width*field_height)
			field[fieldIndex++]=NULL;
		
	}
	
	void animateField() {
		int f=0;
		boolean needToExplodeBarrier=true;
		boolean barrierFound = false;
    
		if (++cursorPhase>=cursorPhases) cursorPhase=0;		
		
		switch (gameStateId) {
		case GAMESTATE_ACCUMULATING_ENERGY:
			if (energy==0) app.laserFillInSound.loop();
			energy+=energySteps/40;
			if (energy>=energySteps-1) {
				energy=energySteps-1;
				app.laserFillInSound.stop();
				gameStateId=GAMESTATE_GAMING;
			}

			break;
		case GAMESTATE_GAMING:
			energy++;	//TODO: ������ - ��� :)
			energy--;
			if (energy<=0) app.gotoAppState(Deflektor.APPSTATE_MENU);
			
			if (beamState==BEAMSTATE_OVERHEAT) overheat+=overheatSteps/128;
			else if (beamState==BEAMSTATE_BOMB) overheat+=overheatSteps/20;
			else overheat-=overheatSteps/128;
			
			if (beamState==BEAMSTATE_CONNECTED) {
				gameStateId = GAMESTATE_LEVELCOMPLETED;
				app.levelCompletedSound.play();
			}
			
			if (overheat <=0) overheat =0;
			if (overheat>=overheatSteps) {
				overheat=0; //TODO: ������ - ��� :)
				//TODO: ����������������� ��� ��������� ���� �� ��������� :)
				//app.laserOverheatSound.stop();
				//app.gotoAppState(Deflektor.APPSTATE_MENU);
			}
			
			
			break;
		case GAMESTATE_CALCULATING_ENERGY:
			break;
		case GAMESTATE_OVERHEAT:
			break;
		case GAMESTATE_LEVELCOMPLETED:
			app.playingLevel++;
			if (app.playingLevel<=app.countOfLevels) {
				app.unlockLevel(app.playingLevel);
				initGame();
			} else app.gotoAppState(Deflektor.APPSTATE_MENU);
			break;
		};

		for (int i=0;i<field_width;i++) 
			for (int j=0;j<field_height;j++) {
				f=field[j*field_width+i];
				if ((f&FLD_AUTOROTATING)!=0) rotateThing(i,j);
				if ((f&EXPLODE)!=0) barrierFound = true;
				if ((f&0xf00)==_EXPL) {
					f++;
					if ((f&0xf)>4) f=NULL;
					else needToExplodeBarrier = false;
					field[j*field_width+i]=f;
				};
				if ((f&0xF00)==CELL) needToExplodeBarrier = false;
				if ((f&0xF00)==PRSM) field[j*field_width+i]=(f&0xF00)|((int)((8*Math.random())+0.5));
			};
		if (needToExplodeBarrier && barrierFound) {
			app.exitOpenSound.play();
			for (int i=0;i<field_width*field_height;i++)
				if ((field[i]&EXPLODE)!=0) field[i]=_EXPL;
		};
		
	}
	
	void rotateThing(int x, int y) {
		rotateThing(x,y,1);
		//int f=field[y*field_width+x];
		//field[y*field_width+x]=(++f)&0xFFFFFF1F;
	}
	
	void rotateThing(int x, int y, int angle) {
		int f=field[y*field_width+x];
		field[y*field_width+x]=(f+angle)&0xFFFFFF1F;
	}
	
	void rotateMirror(int x, int y, int angle) {
		if (x>=field_width || y>=field_height) return;
		if ((field[y*field_width+x]&0xFF00)==MIRR)
			rotateThing(x,y,angle);
	}

	void drawGameInfo () {
		int nrg = (energy *64) /energySteps;
		int ovh = (overheat * 64) / overheatSteps;
		if (nrg>63) nrg=63;
		if (nrg<0) nrg=0;
		if (ovh>63) ovh=63;
		if (ovh<0) ovh=0;
		app.menu_putRegion( 28, field_height*16, 64, 8, 0, 8);
		if (nrg>0) app.menu_putRegion( 28, field_height*16+8, nrg, 8, 0, 0);
		if (nrg<63) app.menu_putRegion( 28+nrg, field_height*16+8, 64-nrg, 8, 64+nrg, 0);
		app.menu_putRegion( 100, field_height*16, 64, 8, 64, 8);
		if (ovh>0) app.menu_putRegion( 100, field_height*16+8, ovh, 8, 0, 0);
		if (ovh<63) app.menu_putRegion( 100+ovh, field_height*16+8, 64-ovh, 8, 64+ovh, 0);
		app.menu_putRegion( 172, field_height*16, 48, 16, 0, 16);
		
		
		//level
		app.showBigNumber(6, field_height*16+4, app.playingLevel);
		//pause button
		app.menu_putRegion( (field_width-1)*16, field_height*16, 16, 16, 48, 16);
	}
	
	void drawField() {
		int f_angle;
		int beam_x=0;
		int beam_y=0;
		int beam_angle=0;

		//clear field by null-sprite;
		for (int i=0;i<field_width;i++) 
			for (int j=0;j<field_height;j++) {
				int f=field[j*field_width+i];
				f_angle = f&0x1f;
				if ((f&0xf00)==LASR) {
					beam_x=i*4+2+angleNodeSteps[f_angle*4][0];
					beam_y=j*4+2+angleNodeSteps[f_angle*4][1];
					beam_angle=(f_angle&3)*4;					
				} else if ((f&0xf00)==RCVR) {
					
				};
				//TODO: �������� �� 1 ������
				app.spr_putRegion( i*16, j*16, 8, 8, 7*16, 5*16+8);
				app.spr_putRegion( i*16+8, j*16, 8, 8, 7*16, 5*16+8);
				app.spr_putRegion( i*16, j*16+8, 8, 8, 7*16, 5*16+8);
				app.spr_putRegion( i*16+8, j*16+8, 8, 8, 7*16, 5*16+8);
			};
		
		drawBeam(beam_x,beam_y,beam_angle);
			
		for (int i=0;i<field_width;i++) 
			for (int j=0;j<field_height;j++) {
				f_angle=field[j*field_width+i]&0x1f;
				switch (field[j*field_width+i]&0xf00) {
				case NULL:
					break;
				case LASR:
					app.spr_putRegion( i*16, j*16, 16, 16, ((f_angle&3)*16), 4*16);
					break;
				case RCVR:
					putReceiver(i,j,f_angle);
					break;
				case MIRR:
					putMirror(i,j,f_angle);
					break;
				case WARP:
					putWarpbox(i,j,f_angle);
					break;
				case CELL:
					putCell(i,j);
					break;
				case MINE:
					putMine(i,j);
					break;
				case WL_A:
					putWallA(i,j,f_angle);
					break;
				case WL_B:
					putWallB(i,j,f_angle);
					break;
				case PRSM:
					putPrism(i,j);
					break;
				case SL_A:
					putSlitA(i,j,f_angle);
					break;
				case SL_B:
					putSlitB(i,j,f_angle);
					break;
				case _EXPL:
					app.spr_putRegion( i*16, j*16, 16, 16, ((f_angle&7)*16), 6*16);
					break;
				}
			};
			
			
			
		if (cursorEnabled && cursorPhase<cursorDisplayPhases) {
			app.spr_putRegion( cursorX*16, cursorY*16, 16, 16, 6*16, 6*16);
		};

	};

	//angle=0..31
	void putMirror(int x, int y, int angle) {
		app.spr_putRegion( x*16, y*16, 16, 16, (angle&7)*16, ((angle>>3)&1)*16);
	}
	
	//angle=0..3
	void putLaser(int x,int y, int angle) {

	}
	
	void putReceiver(int x,int y, int angle) {
		app.spr_putRegion( x*16, y*16, 16, 16, (((angle&3)+4)*16), 4*16);
	}
	
	void putCell(int x, int y) {
		app.spr_putRegion( x*16, y*16, 16, 16, 0, 5*16);
	}
	
	void putMine(int x, int y) {
		app.spr_putRegion( x*16, y*16, 16, 16, 16, 5*16);
	}
	
	void putPrism(int x, int y) {
		app.spr_putRegion( x*16, y*16, 16, 16, 6*16, 5*16);
	}
	
	void putWarpbox(int x, int y, int type) {
		app.spr_putRegion( x*16, y*16, 16, 16, (((type&3)+2)*16), 5*16);
	}
	
	void putSlitA(int x, int y, int angle) {
		app.spr_putRegion( x*16, y*16, 16, 16, ((angle&7)*16), 3*16);
	}

	void putSlitB(int x, int y, int angle) {
		app.spr_putRegion( x*16, y*16, 16, 16, ((angle&7)*16), 2*16);
	}
	
	void putWallA(int x, int y, int type) {
		if ((type&8)!=0)	app.spr_putRegion( x*16, y*16, 8, 8, 7*16+8, 5*16);
		if ((type&4)!=0)	app.spr_putRegion( x*16+8, y*16, 8, 8, 7*16+8, 5*16);
		if ((type&2)!=0)	app.spr_putRegion( x*16, y*16+8, 8, 8, 7*16+8, 5*16);
		if ((type&1)!=0)	app.spr_putRegion( x*16+8, y*16+8, 8, 8, 7*16+8, 5*16);
	}
	
	void putWallB(int x, int y, int type) {
		if ((type&8)!=0)	app.spr_putRegion( x*16, y*16, 8, 8, 7*16, 5*16);
		if ((type&4)!=0)	app.spr_putRegion( x*16+8, y*16, 8, 8, 7*16, 5*16);
		if ((type&2)!=0)	app.spr_putRegion( x*16, y*16+8, 8, 8, 7*16, 5*16);
		if ((type&1)!=0)	app.spr_putRegion( x*16+8, y*16+8, 8, 8, 7*16, 5*16);
	}
	
	void drawBeam(int beamX, int beamY, int beamAngle) {
		
		int newBeamX;
		int newBeamY;
		int oldBeamAngle;
		boolean endBeam=false;
		
		beamState = BEAMSTATE_NORMAL;
		
		while (!endBeam) {

			newBeamX = beamX+angleNodeSteps[beamAngle][0];
			newBeamY = beamY+angleNodeSteps[beamAngle][1];
			oldBeamAngle = beamAngle;
			if (newBeamX>field_width*4 || newBeamX<0 || newBeamY>field_height*4 || newBeamY<0) {
				endBeam=true;
				continue;
			};
			
			drawSpriteLine(beamX, beamY, newBeamX, newBeamY);

			if (newBeamX>=field_width*4 || newBeamX<0 || newBeamY>=field_height*4 || newBeamY<0) {
				endBeam=true;
				continue;
			};
			
			beamX = newBeamX;
			beamY = newBeamY;
			
			int sx=beamX&3;
			int sy=beamY&3;
			int fx=beamX/4;
			int fy=beamY/4;
			int f=field[fx+fy*field_width];
			int f_angle=f&0x1f;
			
			
			//�������� �� ����������� ���� � ������� �����������
			if ((sx==2) && (sy==2)) {
				switch (f&0x0f00) {
				case LASR:
					endBeam=true;
					continue;
				case RCVR:
					//if right angle
					if ((f_angle*4)==((beamAngle+8)&15)) beamState = BEAMSTATE_CONNECTED;
					endBeam=true;
					break;
				case MIRR:
					beamAngle =(((f_angle<<1)-beamAngle-beamAngle)>>1)&0xf;
					break;
				case WARP:
					for (int i=0;i<field.length;i++) {
						if ( (field[i]==f) && (i!=(fx+fy*field_width))) {
							beamY=(i/field_width)*4+2;
							beamX=(i-(((int)(beamY/4))*field_width))*4+2;
							break;
						};
					};
					break;
				case CELL:
					field[fx+fy*field_width]=_EXPL;
					app.burnCellSound.play();
					endBeam=true;
					continue;
				case MINE:
					beamState = BEAMSTATE_BOMB;
					endBeam=true;
					continue;
				case PRSM:
					beamAngle= (beamAngle-4+(f&0xFF))&0xf;
					break;
				case _EXPL:
					if (f_angle>2) break;
					endBeam=true;
					continue;
				}
			}

			//�������� �� ����������� � �������� ����������� (�����, �������).
			int mp_beam_x = (beamX+beamX+angleNodeSteps[beamAngle][0])/2;
			int mp_beam_y = (beamY+beamY+angleNodeSteps[beamAngle][1])/2; 	
			
			
			int f1=field[(mp_beam_x/4)+(mp_beam_y/4)*field_width];
			switch (f1&0x0F00) {
			case WL_A:
				int wall_angle=-1;
				final int wall_a_angle_matrix[]={
						//x,y;u&3==0 true=1, false=0;walls	u
						//����� ������� ��������� ������� ���� �� ��� ������ �����
					-1,	//0,0,0,0000	�� �������� ����������
					2,	//0,0,0,0001	2
					6,	//0,0,0,0010	6
					4,	//0,0,0,0011	4
					6,	//0,0,0,0100	6
					0,	//0,0,0,0101	0
					2,	//0,0,0,0110	2
					2,	//0,0,0,0111	2
					2,	//0,0,0,1000	2
					6,	//0,0,0,1001	6
					0,	//0,0,0,1010	0
					6,	//0,0,0,1011	6
					4,	//0,0,0,1100	4
					6,	//0,0,0,1101	6
					2,	//0,0,0,1110	2
					-1,	//0,0,0,1111	��������� ���������� (���� �� ������ ����)
					
						//����� ������� ��������� ������� ���� ��� ������ �����
					-1,	//0,0,1,0000	�� �������� ����������
					-1,	//0,0,1,0001	�� �������� ����������
					-1,	//0,0,1,0010	�������� ����������
					4,	//0,0,1,0011	4
					-1,	//0,0,1,0100	�� �������� ����������
					0,	//0,0,1,0101	0
					-2,	//0,0,1,0110	��������� ����������
					-2,	//0,0,1,0111	��������� ����������
					-1,	//0,0,1,1000	�� �������� ����������
					-2,	//0,0,1,1001	��������� ����������
					0,	//0,0,1,1010	0
					-2,	//0,0,1,1011	6
					4,	//0,0,1,1100	4
					-2,	//0,0,1,1101	6
					-2,	//0,0,1,1110	2
					-1	//0,0,1,1111	��������� ���������� (���� �� ������ ����)					
				};
				int wallsAround=f;
				int wallUP=0;
				int wallUPLEFT=0;
				int wallLEFT=0;
				
				//��������� ���������� � ��������� ������ ������, �����-������, �����.
				if (fx>0) wallLEFT=field[fx-1+fy*field_width];
				if ((fx>0) && (fy>0)) wallUPLEFT=field[fx-1+(fy-1)*field_width];
				if (fy>0) wallUP=field[fx+(fy-1)*field_width];
				
				if ((wallsAround&0xF00)!=	WL_A) wallsAround=0;	else wallsAround&=0xf;
				if ((wallLEFT&0xF00)!=		WL_A) wallLEFT=0;		else wallLEFT&=0xf;
				if ((wallUPLEFT&0xF00)!=	WL_A) wallUPLEFT=0;	else wallUPLEFT&=0xf;
				if ((wallUP&0xF00)!=		WL_A)	wallUP=0;		else wallUP&=0xf;
				
				
				if ((beamY&2)==0) {
					//�������� ��������:
					wallsAround = (wallsAround>>2) | ((wallUP<<2)&0xC);
					wallUP = wallUP>>2;
					wallLEFT = (wallLEFT>>2) | ((wallUPLEFT<<2)&0xC);
					wallUPLEFT = wallUPLEFT>>2;
				};
				
				if ((beamX&2)==0) {
					//�������� ������:
					wallsAround = ((wallsAround >>1) &5 ) | ((wallLEFT &5) <<1);
				};
				
				if (((beamX&1)+(beamY&1))==0) {
					wall_angle=wall_a_angle_matrix[( ((beamAngle&3)==0)?16:0 )|(wallsAround&0xF)];
					//���� ���� ���� �� ��������� � �����
					if ((beamAngle&3)!=0 && wallsAround!=6 && wallsAround!=9) {
						//�� ���������, ������ �� ��� � ����������� ��� �� ������ �������� ����� ����.
						if ((beamAngle<4) && ((wallsAround&4)==0)) wall_angle=-1;
						else if ((beamAngle>4) && (beamAngle<8) && ((wallsAround&1)==0)) wall_angle=-1;
						else if ((beamAngle>8) && (beamAngle<12) && ((wallsAround&2)==0)) wall_angle=-1;
						else if ((beamAngle>12) && (wallsAround&8)==0) wall_angle=-1;
					}
				} else {

				//	1,0,x,x0x0	�� �������� ����������
				//	1,0,x,x0x1	4
				//	1,0,x,x1x0	4
				//	1,0,x,x1x1	���������� ����������	
				if ((beamX&1)==1) switch (wallsAround&0x5) {
					case 1: case 4: wall_angle=4; break;
					case 5: endBeam=true; 
					};
					
				//0,1,x,xx00	�� �������� ����������
				//0,1,x,xx01	0
				//0,1,x,xx10	0
				//0,1,x,xx11	���������� ����������
				if ((beamY&1)==1) switch (wallsAround&0x3) {
					case 1: case 2: wall_angle=0; break;
					case 3: endBeam=true; 
					};
					
				};
				
				if (wall_angle>=0) beamAngle=(wall_angle*2-beamAngle)&0xf;
				else if (wall_angle==-1) break;
				else if (wall_angle==-2) { beamAngle=(beamAngle+8)&0xf; break; };
				
				//continue;
				break;
			case WL_B:
				int crd=((mp_beam_x>>1)&1)+((mp_beam_y)&2);
				if ((crd==0) && ((f1&8)!=0)) {	endBeam=true;	continue; };
				if ((crd==1) && ((f1&4)!=0)) {	endBeam=true;	continue; };
				if ((crd==2) && ((f1&2)!=0)) {	endBeam=true;	continue; };
				if ((crd==3) && ((f1&1)!=0)) {	endBeam=true;	continue; };
				//(mp_beam_x>1)&1 - ���������� ������ �������� 0..1
				//(mp_beam_y)&2 - ���������� ������ �������� 0..1
				//0,0 && 8
				//0,1 && 4
				//1,0 && 2
				//1,1 && 1
				break;
			case SL_A:
				if ((f1&7)!=(beamAngle&7)) {
					if ((beamX&3)==0)
						beamAngle=(0-beamAngle)&0xf;
					if ((beamY&3)==0)
						beamAngle=(4*2-beamAngle)&0xf;
					//continue;
					break;
				};
				break;
			case SL_B:
				if ((f1&7)!=(beamAngle&7)) {
					endBeam=true;
					//continue;
					break;
				};
				break;
			}
			//���� ��������� ����� � �������� �������
			if (Math.abs(beamAngle-oldBeamAngle)==8) {
				endBeam=true;
				beamState = BEAMSTATE_OVERHEAT;
			}
			
		}
		
		
		
		
	};
	
	void drawSpriteLine(int x0, int y0, int x1, int y1) {
		int lx0, lx1, ly0, ly1;
		if (x0<x1) { lx0=x0; lx1=x1; ly0=y0; ly1=y1;  }
		else { lx0=x1; lx1=x0; ly0=y1; ly1=y0;  };
		int sx=lx1-lx0;
		int sy=ly1-ly0;
		if (sx==0 && sy==-2)					app.spr_putRegionSafe( lx0*4-4, ly0*4-8, 8, 8, 0, 112);
		if (sx==1 && sy==-2 && ((lx0&1)==0))	app.spr_putRegionSafe( lx0*4-4, ly0*4-8, 16, 8, 8, 120);
		if (sx==1 && sy==-2 && ((lx0&1)==1))	app.spr_putRegionSafe( lx0*4-4, ly0*4-8, 16, 8, 12, 112);
		if (sx==2 && sy==-2)					app.spr_putRegionSafe( lx0*4-4, ly0*4-12, 16, 16, 24, 112);
		if (sx==2 && sy==-1 && ((ly0&1)==0))	app.spr_putRegionSafe( lx0*4,   ly0*4-4, 16, 8, 40, 120);
		if (sx==2 && sy==-1 && ((ly0&1)==1))	app.spr_putRegionSafe( lx0*4-8, ly0*4-8, 16, 8, 40, 112);
		if (sx==2 && sy==0)						app.spr_putRegionSafe( lx0*4,   ly0*4-4, 8, 8, 56, 112);
		if (sx==2 && sy==1 && ((ly0&1)==0))		app.spr_putRegionSafe( lx0*4,   ly0*4-4, 16, 8, 64, 112);
		if (sx==2 && sy==1 && ((ly0&1)==1)) 	app.spr_putRegionSafe( lx0*4-8, ly0*4, 16, 8, 64, 120);
		if (sx==2 && sy==2)						app.spr_putRegionSafe( lx0*4-4, ly0*4-4, 16, 16, 80, 112);
		if (sx==1 && sy==2 && ((lx0&1)==0))		app.spr_putRegionSafe( lx0*4-4, ly0*4, 16, 8, 96, 112);
		if (sx==1 && sy==2 && ((lx0&1)==1))		app.spr_putRegionSafe( lx0*4-4, ly0*4, 16, 8, 100, 120);
		if (sx==0 && sy==2)						app.spr_putRegionSafe( lx0*4-4, ly0*4, 8, 8, 0, 112);
		
		
	}
	
	void touch(int x, int y) {
		if (x<0 || x>= field_width || y< 0 || y>=field_height) return;
		int f=field[y*field_width+x];
		if ((f&0xFF00)==MIRR) {
			rotateThing(x,y);
		};
	};
	
	/*
	int getMirrorAngle (int x, int y) {
		if (x<0 || x>= field_width || y< 0 || y>=field_height) return -1;
		int f=field[y*field_width+x];
		if ((f&0xFF00)==MIRR) {
			return f&0x1f;
		};
		return -1;
	};
	
	void setMirrorAngle (int x, int y, int angle) {
		if (x<0 || x>= field_width || y< 0 || y>=field_height || angle<0) return;
		int f=field[y*field_width+x];
		if ((f&0xFF00)==MIRR) {
			field[y*field_width+x]=(f&0xFFFFFF00)|(angle&0x1f);
		};
	};
	*/
	
	
	
}