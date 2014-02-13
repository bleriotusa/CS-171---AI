import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import connectK.BoardModel;
import connectK.CKPlayer;

public class IAAI extends CKPlayer 
{
	// shows how many spaces of each column are used
	private ArrayList<Integer> xHeight;
	private ArrayList<Point> moves;
	private int width;
	private int height;
	private int kLength;
	private byte player;
	private byte opponent;
	private int currentDepth;
	private int depthLimit;
	
	private PriorityQueue<PointWithScore> listOfMoves;
	
	// point class that also keeps an integer score
	private class PointWithScore extends Point implements Comparable<PointWithScore>
	{
		private static final long serialVersionUID = 1L;
		private int score;
		public PointWithScore(Point point, int score)
		{
			super(point);
			this.score = score;
		}
		public int getScore()
		{
			return score;
		}
		@Override
		public int compareTo(PointWithScore o) {
			if(this.score < o.score)
				return -1;
			else if(this.score > o.score)
				return 1;
			// TODO Auto-generated method stub
			return 0;
		}
	}
	
	private class ReversePriority implements Comparator<PointWithScore>
	{
		public int compare(PointWithScore o1, PointWithScore o2){ 
			return 0 - o1.compareTo(o2);
		}
	}
	
	public IAAI(byte player, BoardModel state) 
	{
		super(player, state);
		teamName = "IAAI";
		// *** UPDATE AFTER EACH PLACEMENT, use lastmove to update other player
		xHeight = new ArrayList<Integer>(state.width);
		
		// initialize xHeight
		for(Integer num: xHeight)
			num = 0;
		// initialize length, height, klength
		width = state.getWidth();
		height = state.getHeight();
		kLength = state.getkLength();
		
		// set player values
		this.player = player;
		if(player == 1)
			opponent = 2;
		else
			opponent = 1;
		
		// initialize priority queue. this queue will keep track of the
		// moves, and have them sorted with best move first
		// passes in ReversePriority Comparator to tell PQ how to operate:
		// 		by reversing the order of natural ordering
		
		listOfMoves = new PriorityQueue<PointWithScore>(11, new ReversePriority());
		
		
		// get rid of this when timer is implemented
		this.depthLimit = 10;

	}
	
	private Point IDS(BoardModel state)
	{
		moves = generateMoves();
		int depth = depthLimit;

		for(Point move: moves)
		{
			BoardModel copy = state.clone();
			copy.placePiece(move, player);
			listOfMoves.add(new PointWithScore(move, MinMove(copy, depth)));
		}
		return null;
	}
	
//	private Point MinMax(BoardModel state)
//	{
//		
//		return MaxMove(state);
//	}
	
	private int MaxMove(BoardModel state, int depthLimit)
	{
		// check if depthLimit reached. If so, send back eval of state
		if (currentDepth == depthLimit)
			return EvalState(state);
		
		// else, try every possible move, until depth limit is up
		// and save the score for every move
		// then return largest score
		ArrayList<Point> moves;
		moves = generateMoves();
		ArrayList<Integer> scores = new ArrayList<Integer>();

		for(Point move: moves)
		{
			BoardModel copy = state.clone();
			copy.placePiece(move, player);
			scores.add(MinMove(copy, depthLimit));
		}
		int bestScore = Integer.MIN_VALUE;
		for(Integer score: scores)
		{
			if(score > bestScore)
				bestScore = score;
		}
		currentDepth += 1;
		return bestScore;
	}
	
	private int MinMove(BoardModel state, int depthLimit)
	{
		if (currentDepth == depthLimit)
			return EvalState(state);
		
		ArrayList<Point> moves;
		moves = generateMoves();
		ArrayList<Integer> scores = new ArrayList<Integer>();

		for(Point move: moves)
		{
			BoardModel copy = state.clone();
			copy.placePiece(move, opponent);
			scores.add(MaxMove(copy, depthLimit));
		}
		int bestScore = Integer.MAX_VALUE;
		for(Integer score: scores)
		{
			if(score < bestScore)
				bestScore = score;
		}
		currentDepth += 1;
		return bestScore;
	}
	
	private int EvalState(BoardModel state)
	{
		return 0;
	}
 
	@Override
	public Point getMove(BoardModel state) 
	{
//		if(state.gravityEnabled())
//		{
//			
//			
//			for(int i=0; i<state.getWidth(); ++i)
//				for(int j=0; j<state.getHeight(); ++j)
//					if(state.getSpace(i, j) == 0)
//						return new Point(i,j);
//			
//		}
//		
//		else
//		{
//			
//		}
		
	
		return null;
	}
	
	@Override
	public Point getMove(BoardModel state, int deadline)
	{
		return getMove(state);
	}
	
	private ArrayList<Point> generateMoves()
	{
		ArrayList<Point> moves = new ArrayList<Point>(width);
		
		for(int i = 0; i < width; i++)
		{
			if(xHeight.get(i) < height)
				moves.add(new Point(i, xHeight.get(i)));
		}
		
		return moves;
	}
	
	private boolean getMoveGravityOn(BoardModel state)
	{
		return state.gravityEnabled();
	}
	
	private Point getMoveGravityOff(BoardModel state)
	{
		return null;
	}
}
