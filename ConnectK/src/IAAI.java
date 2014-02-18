import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.HashMap;

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
	HashMap<BoardModel, Integer> hMap;
	
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
	private class ReverseIntPriority implements Comparator<Integer>
	{
		public int compare(Integer o1, Integer o2){
			return o2 - o1;
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

		// initialize HashSet for keeping track of each game node (each state)'s value
		hMap = new HashMap<BoardModel, Integer>();
				
		// get rid of this when timer is implemented
		this.depthLimit = 10;

	}
	
	@Override
	public Point getMove(BoardModel state)
	{
		// generate list of moves in random order for first time
		int depth = depthLimit;
		
		for(int currentDepth = 0; currentDepth <= depthLimit; currentDepth++)
		{
			;
		}
		

		listOfMoves = new PriorityQueue<PointWithScore>(11, new ReversePriority());
		return null;
	}
	
//	private Point MinMax(BoardModel state)
//	{
//		
//		return MaxMove(state);
//	}
	
	private int MaxMove(BoardModel state, int depthLimit)
	{
		// check if depthLimit reached. If so, record to hash table and send back eval of state
		if (currentDepth == depthLimit)
		{
			int eval = EvalState(state);
			hMap.put(state, eval);
			return eval;
		}
		// else, try every possible move, until depth limit is up
		// and save the score for every move
		// then return largest score
		ArrayList<Point> moves = generateMoves();
		
		// initialize priority queue. this queue will keep track of the
		// moves, and have them sorted with best move first;
		
		// passes in ReversePriority Comparator to tell PQ how to operate:
		// by reversing the order of natural ordering
		PriorityQueue<PointWithScore> movesWithScores = new PriorityQueue<PointWithScore>();
		boolean childrenExplored = false;

		// IDEA:
		// get a sorted list of children nodes if possible, then expanding them
		//
		// METHOD:
		// just apply each move to the passed-in state, then get the values for 
		// each state. then start with the move that leads to the state with the largest value
		
		// Getting a sorted list of children nodes:
		//	- check if children have been explored
		//	- if the resulting state is in the hashMap, stick the value onto the move that
 		//		  would get to the resulting state, and put into PQ 
 		//		- then do DFS using PQ as the source of moves to apply
		//	- if NOT in hashMap, then just continue with the depth first search by using
		//	- the array of moves that has no particular order

		// use this to keep a sorted list of scores
		PriorityQueue<Integer> scores = new PriorityQueue<Integer>(11, new ReverseIntPriority());

		// check if children are explored
		if(hMap.containsKey(state.clone().placePiece(moves.get(0), player)))
			childrenExplored = true;
		
		// if state is in the hashMap (i.e. this part of game tree is already explored)
		// make sorted list of moves that would get to each state, according to the states'
		// values. These values would go into the "move" that gets to the state
		if(childrenExplored)
		{
			BoardModel stateCopy = null;
			for(Point move: moves)
			{
				stateCopy = state.clone();
				stateCopy.placePiece(move, player);
			
				if(hMap.containsKey(stateCopy))
				{
					movesWithScores.add(new PointWithScore(move, hMap.get(stateCopy)));
				}
			}
			// go through the priority list and DFS each node
			while(!listOfMoves.isEmpty())
			{
				stateCopy = state.clone();
				stateCopy.placePiece(movesWithScores.remove(), player);
				scores.add(MinMove(stateCopy, depthLimit));
			}
		}
		// if not in hashMap then just do DFS with no ordering
		else
		{
			for(Point move: moves)
			{
				BoardModel stateCopy = state.clone();
				stateCopy.placePiece(move, player);
				scores.add(MinMove(stateCopy, depthLimit));
			}
		}
		currentDepth += 1;
		return scores.peek();
	}
	
	private int MinMove(BoardModel state, int depthLimit)
	{
		if (currentDepth == depthLimit)
		{
			int eval = EvalState(state);
			hMap.put(state, eval);
			return eval;
		}
		
		ArrayList<Point> moves = generateMoves();
		PriorityQueue<Integer> scores = new PriorityQueue<Integer>(11);
		PriorityQueue<PointWithScore> movesWithScores = new PriorityQueue<PointWithScore>();
		boolean childrenExplored = false;

		if(hMap.containsKey(state.clone().placePiece(moves.get(0), opponent)))
			childrenExplored = true;
		
		if(childrenExplored)
		{
			BoardModel stateCopy = null;
			for(Point move: moves)
			{
				stateCopy = state.clone();
				stateCopy.placePiece(move, opponent);
			
				if(hMap.containsKey(stateCopy))
				{
					movesWithScores.add(new PointWithScore(move, hMap.get(stateCopy)));
				}
			}
			// go through the priority list and DFS each node
			while(!listOfMoves.isEmpty())
			{
				stateCopy = state.clone();
				stateCopy.placePiece(movesWithScores.remove(), opponent);
				scores.add(MaxMove(stateCopy, depthLimit));
			}
		}
		// if not in hashMap then just do DFS with no ordering
		else
		{
			for(Point move: moves)
			{
				BoardModel stateCopy = state.clone();
				stateCopy.placePiece(move, opponent);
				scores.add(MaxMove(stateCopy, depthLimit));
			}
		}
		currentDepth += 1;
		return scores.peek();
	}
	
	private int EvalState(BoardModel state)
	{
		return 0;
	}
 
//	@Override
//	public Point getMove(BoardModel state) 
//	{
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
//		
//	
//		return null;
//	}
//	
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
