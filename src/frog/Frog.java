package frog;

import gameCommons.Game;
import gameCommons.IFrog;
import util.Case;
import util.Direction;

public class Frog implements IFrog {
	
	private Game game;
	private Case position;
	private Direction Direction;

	public Frog (Game g){
		this.game=g;
		this.position = new Case(g.width/2,0);
		this.Direction = util.Direction.up;
	}

	@Override
	public Case getPosition() {
		return  this.position;

	}

	@Override
	public Direction getDirection() {
		return this.Direction ;
	}

	@Override
	public void move(Direction key) {

		switch (key){

			case up:
				if ( this.position.ord < game.height - 1 )
				this.position = new Case(position.absc, position.ord+1 );
				break;
			case down:
				if ( this.position.ord > 0 )
				this.position = new Case(position.absc, position.ord-1 );
				break;
			case left:
				if ( this.position.absc > 0 )
				this.position = new Case(position.absc-1, position.ord );
				break;
			case right :
				if ( this.position.absc < game.width - 1 )
				this.position = new Case(position.absc+1, position.ord );
				break;

		}

		this.game.update();

		}



}
