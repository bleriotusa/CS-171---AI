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
	private HashMap<BoardModel, Integer> hMap;
	
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

		
		// initialize xHeight

		// initialize length, height, klength
		width = state.getWidth();
		height = state.getHeight();
		kLength = state.getkLength();
		
		// *** UPDATE AFTER EACH PLACEMENT, use lastmove to update other player
		xHeight = new ArrayList<Integer>();
		for(int i = 0; i < width; i++)
			xHeight.add(0);
		
		System.out.println("width is: "+ width);
		System.out.println("xHeight is: " + xHeight.size());

		// set player values
		this.player = player;
		if(player == 1)
			opponent = 2;
		else
			opponent = 1;

		// initialize HashSet for keeping track of each game node (each state)'s value
		hMap = new HashMap<BoardModel, Integer>();
				
		// get rid of this when timer is implemented
		this.depthLimit = 3;

	}
	
	@Override
	public Point getMove(BoardModel state)
	{
		// update xHeight
		updateXHeight(state.getLastMove().x);
		// generate list of moves in random order for first time
		listOfMoves = new PriorityQueue<PointWithScore>(11, new ReversePriority());

		int depth = depthLimit;
		ArrayList<Point> moves = generateMoves();
		PriorityQueue<PointWithScore> movesWithScores = new PriorityQueue<PointWithScore>(11, new ReversePriority());
		
		// if no tree has been made yet (if hashMap doesn't have a record of child at all)... 
		// do a blind search
		if(hMap.get(moves.get(0))== null)	
		// for each move, place the piece on a copy of the board, then send that new board to Min
		// this will cause a DFS to happen and return the lowest value given by making this move
		// then we add all the moves with their corresponding scores to a priority queue that is max first
			for(Point move: moves)
			{
				currentDepth = 0;
				movesWithScores.add(new PointWithScore(move, MinMove(state.clone().placePiece(move, player), depthLimit)));
			}
		
		// if tree HAS been made, then just check the tree to see which one to start with,
		// then do the same process as the blind search
		else
		{
			System.out.println("TREE HAS BEEN MADE");
			for(Point move: moves)
				listOfMoves.add(new PointWithScore(move, hMap.get(state.clone().placePiece(move,player))));
			while(!listOfMoves.isEmpty())
			{
				System.out.println("POPPING OFF QUEUE");
				Point move = listOfMoves.remove();
				currentDepth = 0;
				movesWithScores.add(new PointWithScore(move, MinMove(state.clone().placePiece(move, player), depthLimit)));
			}
		}
		Point bestMove = movesWithScores.remove();
		// update xHeight
		updateXHeight(bestMove.x);
		// to get the move that results in the state with the highest score, just pop off the first item.
		return bestMove;
		
//		for(int currentDepth = 0; currentDepth <= depthLimit; currentDepth++)
//		{
//			;
//		}
//		

	}
	
//	private Point MinMax(BoardModel state)
//	{
//		
//		return MaxMove(state);
//	}
	
	private int MaxMove(BoardModel state, int depthLimit)
	{
		currentDepth += 1;

		System.out.println("MAXMOVE RAN, CURRENT DEPTH = :" + currentDepth);
		// check if depthLimit reached. If so, record to hash table and send back eval of state
		if (currentDepth >= depthLimit)
		{
			int eval = EvalState(state);
			hMap.put(state, eval);
			return eval;
		}
		// else, try every possible move, until depth limit is up
		// and save the score for every move
		// then return largest score
		ArrayList<Point> moves = generateMoves();
		int bestMoveVal = Integer.MIN_VALUE;
		BoardModel bestState = null;

		// initialize priority queue. this queue will keep track of the
		// moves, and have them sorted with best move first;
		
		// passes in ReversePriority Comparator to tell PQ how to operate:
		// by reversing the order of natural ordering
		PriorityQueue<PointWithScore> movesWithScores = new PriorityQueue<PointWithScore>(11, new ReversePriority());
		boolean childrenExplored = false;
		BoardModel stateCopy = null;

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

		// check if children are explored
		if(hMap.containsKey(state.clone().placePiece(moves.get(0), player)))
			childrenExplored = true;
		
		// if state is in the hashMap (i.e. this part of game tree is already explored)
		// make sorted list of moves that would get to each state, according to the states'
		// values. These values would go into the "move" that gets to the state
		if(childrenExplored)
		{
			for(Point move: moves)
			{
				stateCopy = state.clone();
				stateCopy.placePiece(move, player);
			
				movesWithScores.add(new PointWithScore(move, hMap.get(stateCopy)));
			}
			// go through the priority list and DFS each node
			while(!movesWithScores.isEmpty())
			{
				stateCopy = state.clone();
				stateCopy.placePiece(movesWithScores.remove(), player);
				int minMove = MinMove(stateCopy, depthLimit);
				if(minMove > bestMoveVal)
				{
					bestMoveVal = minMove;
					bestState = stateCopy;
				}
			}
		}
		// if not in hashMap then just do DFS with no ordering
		else
		{
			for(Point move: moves)
			{
				stateCopy = state.clone();
				stateCopy.placePiece(move, player);
				int minMove = MinMove(stateCopy, depthLimit);
				if(minMove > bestMoveVal)
				{
					bestMoveVal = minMove;
					bestState = stateCopy;
				}
			}
		}
		hMap.put(bestState, bestMoveVal);
		return bestMoveVal;
	}
	
	private int MinMove(BoardModel state, int depthLimit)
	{
		currentDepth += 1;

		System.out.println("MINMOVE RAN, CURRENT DEPTH = :" + currentDepth);
		if (currentDepth >= depthLimit)
		{
			int eval = EvalState(state);
			hMap.put(state, eval);
			return eval;
		}
		
		ArrayList<Point> moves = generateMoves();
		PriorityQueue<Integer> scores = new PriorityQueue<Integer>(11, new ReverseIntPriority());
		PriorityQueue<PointWithScore> movesWithScores = new PriorityQueue<PointWithScore>(11, new ReversePriority());
		int bestMoveVal = Integer.MAX_VALUE;
		BoardModel bestState = null;
		boolean childrenExplored = false;
		BoardModel stateCopy = null;

		if(hMap.containsKey(state.clone().placePiece(moves.get(0), opponent)))
			childrenExplored = true;
		
		if(childrenExplored)
		{
			for(Point move: moves)
			{
				stateCopy = state.clone();
				stateCopy.placePiece(move, opponent);

				movesWithScores.add(new PointWithScore(move, hMap.get(stateCopy)));
			}
			// go through the priority list and DFS each node
			while(!movesWithScores.isEmpty())
			{
				stateCopy = state.clone();
				stateCopy.placePiece(movesWithScores.remove(), opponent);
				int maxMove = MaxMove(stateCopy, depthLimit);
				if(maxMove < bestMoveVal)
				{
					bestMoveVal = maxMove;
					bestState = stateCopy;
				}
			}
		}
		// if not in hashMap then just do DFS with no ordering
		else
		{
			for(Point move: moves)
			{
				stateCopy = state.clone();
				stateCopy.placePiece(move, opponent);
				int maxMove = MaxMove(stateCopy, depthLimit);
				if(maxMove < bestMoveVal)
				{
					bestMoveVal = maxMove;
					bestState = stateCopy;
				}
			}
		}
		hMap.put(bestState, bestMoveVal);
		return bestMoveVal;
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
		System.out.println(depthLimit);

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
	private void updateXHeight(int x)
	{
		xHeight.set(x, xHeight.get(x)+1);
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
