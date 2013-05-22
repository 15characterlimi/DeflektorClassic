package pro.oneredpixel.deflektorclassic;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class LevelsState extends State {

	int page=1;
	
	LevelsState(Deflektor defl) {
		super(defl);
		// TODO Auto-generated constructor stub
	}
	
	public boolean tap(float x, float y, int tapCount, int button) {
		//app.gotoAppState(Deflektor.APPSTATE_GAME);
		int ix=(int)((x-app.winX)/app.sprScale);
		int iy=(int)((y-app.winY)/app.sprScale);
		if ((ix>=0) && (ix<240) && (iy>=0) && (iy<160)) {
			if ((page>1) && checkInBox(ix,iy,0,160/2-8-8,32,32)) page--;
			if ((page<3) && checkInBox(ix,iy,240-16-8-8-8, 160/2-8,32,32)) page++;
			int lx=(ix-44)/8;
			int ly=(iy-20)/8;
			if ( ((lx&3)!=3) && ((ly&3)!=3) ) {
				lx=lx/4; ly=ly/4;
				int lev=ly*5+lx+(page-1)*20+1;
				if ((lx>=0) && (lx<5) && (ly>=0) && (ly<4) && (lev<=app.unlockedLevel)) {
					app.playingLevel = lev;
					app.gotoAppState(Deflektor.APPSTATE_GAME);
				};
			}
		}		
		return false;
	}
	
	boolean checkInBox(int x,int y, int bx, int by, int bwidth, int bheight) {
		return (x>=bx)&&(x<(bx+bwidth))&&(y>=by)&&(y<(by+bheight));
	};

	public void render(SpriteBatch batch) {
		batch.setProjectionMatrix(app.camera.combined);
		batch.begin();
		
		int s=(page-1)*20+1;
		for (int i=0;i<4;i++) {
			for (int j=0;j<5;j++) {
				drawLevelBox(44+j*32,20+i*32,s++);
				if (s>app.countOfLevels) break;
			};
			if (s>app.countOfLevels) break;
		};

		if (page>1) app.menu_putRegion(8, 160/2-8, 16, 16, 32,32);
		if (page<3) app.menu_putRegion(240-8-16, 160/2-8, 16, 16, 48,32);
		
		batch.end();
	};
	
	void drawLevelBox(int x, int y, int levelNumber) {
		app.menu_putRegion(x, y, 24, 24, 0,32);
		app.showBigNumber(x+4,y+8,levelNumber);
		if (app.unlockedLevel<levelNumber) app.menu_putRegion(x+15, y+15, 8, 8, 24,32);
	};
	

	


}