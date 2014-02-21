import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;

import connectK.BoardModel;
import connectK.CKPlayer;

public class IAAI extends CKPlayer 
{
	// shows how many spaces of each column are used
	private ArrayList<Integer> xHeight;
	private int width;
	private int height;
	private int kLength;
	private byte player;
	private byte opponent;
	private int currentDepth;

	private static final long TIMELIMIT = 4990000000L;

	private PriorityQueue<PointWithScore> listOfMoves;
	private HashMap<BoardModel, Double> hMap;

	// point class that also keeps an integer score
	private class PointWithScore extends Point implements Comparable<PointWithScore>
	{
		private static final long serialVersionUID = 1L;
		private double score;
		public PointWithScore(Point point, double score)
		{
			super(point);
			this.score = score;
		}
		public double getScore()
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

		// initialize length, height, klength
		width = state.getWidth();
		height = state.getHeight();
		kLength = state.getkLength();

		// initialize xHeight
		// *** UPDATE AFTER EACH PLACEMENT, use lastmove to update other player
		xHeight = new ArrayList<Integer>();
		
		for(int i = 0; i < width; i++)
			xHeight.add(0);


		// set player values
		this.player = player;
		this.opponent = (byte)((player == 1) ? 2 : 1);

		// initialize HashSet for keeping track of each game node (each state)'s value
		hMap = new HashMap<BoardModel, Double>();

		// get rid of this when timer is implemented
//		this.depthLimit = 0;

	}

	@Override
	public Point getMove(BoardModel state)
	{
		long startTime = System.nanoTime();

		//.out.println("GETTING MOVE...");
		// update xHeight
		if(state.getLastMove() != null)
			updateXHeight(state.getLastMove().x);
		// generate list of moves in random order for first time
		ArrayList<Point> moves = generateMoves();

		// keep track of which moves to investigate first
		listOfMoves = null;

		Point bestMove = null;
		// keep track of the moves returned so far
		PriorityQueue<PointWithScore> movesWithScores = null;

		// start IDS search with a series of Depth Limited Searches
		//for(int currDepth = 0; currDepth <= DEPTHLIMIT; currDepth++)
		int currDepth = 0;

		while(System.nanoTime() - startTime < TIMELIMIT)
		{
			// re-initialize move storages
			movesWithScores = new PriorityQueue<PointWithScore>(11, new ReversePriority());
			listOfMoves = new PriorityQueue<PointWithScore>(11, new ReversePriority());
			// reset depth counter

			// if no tree has been made yet (if hashMap doesn't have a record of child at all)... 
			// do a blind search
			if(hMap.get(state.clone().placePiece(moves.get(0), player))== null)	
			// for each move, place the piece on a copy of the board, then send that new board to Min
			// this will cause a DFS to happen and return the lowest value given by making this move
			// then we add all the moves with their corresponding scores to a priority queue that is max first
				for(Point move: moves)
				{
					currentDepth = 0;
					movesWithScores.add(new PointWithScore(move, MinMove(state.clone().placePiece(move, player), currDepth, startTime)));
				}

			// if tree HAS been made, then just check the tree to see which one to start with,
			// then do the same process as the blind search
			else
			{
				//System.out.println("TREE HAS BEEN MADE");
				for(Point move: moves)
					listOfMoves.add(new PointWithScore(move, hMap.get(state.clone().placePiece(move,player))));
				while(!listOfMoves.isEmpty())
				{
					//System.out.println("POPPING OFF QUEUE - ROOT");
					PointWithScore move = listOfMoves.remove();
					//System.out.println("ROOT QUEUE VALUE: " + move.getScore());
					currentDepth = 0;
					movesWithScores.add(new PointWithScore(move, MinMove(state.clone().placePiece(move, player), currDepth, startTime)));
				}
			}

			currDepth++;
		}		
		// to get the move that results in the state with the highest score, just pop off the first item.
		bestMove = movesWithScores.remove();
		// update xHeight
		updateXHeight(bestMove.x);

		// clean up and return
		listOfMoves = null;
		moves = null;
		movesWithScores = null;

		System.out.println("Depth reached: " + currDepth);
		System.out.println("Time: " + (System.nanoTime() - startTime));
		
		return bestMove;
	}

	private double MaxMove(BoardModel state, int depthLimit, long startTime)
	{
		currentDepth += 1;

		//System.out.println("MAXMOVE RAN, CURRENT DEPTH = :" + currentDepth);
		// check if depthLimit reached. If so, record to hash table and send back eval of state
		//if (currentDepth >= depthLimit)
		if(System.nanoTime() - startTime > TIMELIMIT || currentDepth >= depthLimit)
		{
			double eval = EvalState(state, player);
			hMap.put(state, eval);
			return eval;
		}
		// else, try every possible move, until depth limit is up
		// and save the score for every move
		// then return largest score
		ArrayList<Point> moves = generateMoves();
		double bestMoveVal = Double.MIN_VALUE;

		// initialize priority queue. this queue will keep track of the
		// moves, and have them sorted with best move first;

		// passes in ReversePriority Comparator to tell PQ how to operate:
		// by reversing the order of natural ordering
		PriorityQueue<PointWithScore> movesWithScores = new PriorityQueue<PointWithScore>(11, new ReversePriority());
		boolean childrenExplored = false;
		BoardModel stateCopy = null;
		int localDepth = currentDepth;

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
			// DFS:
			// 1. make sure global depth field is reset to current depth
			// 2. make a move on a copy of the state
			// 3. send state through MinMove function
			// 4. get returned value and associate it with the move
			// 5. put PointWithScore into PQ so that highest value comes out first
//			//System.out.println("POPPING OFF QUEUE: MAX");
			while(!movesWithScores.isEmpty())
			{
				currentDepth = localDepth;
				stateCopy = state.clone();
				stateCopy.placePiece(movesWithScores.remove(), player);
				double minMove = MinMove(stateCopy, depthLimit, startTime);
				if(minMove > bestMoveVal)
				{
					bestMoveVal = minMove;
				}
			}
		}
		// if not in hashMap then just do DFS with no ordering
		else
		{
			for(Point move: moves)
			{
				currentDepth = localDepth;
				stateCopy = state.clone();
				stateCopy.placePiece(move, player);
				double minMove = MinMove(stateCopy, depthLimit, startTime);
				if(minMove > bestMoveVal)
					bestMoveVal = minMove;
			}
		}

		// record this state with the best value found
		hMap.put(state, bestMoveVal);

		return bestMoveVal;
	}

	private double MinMove(BoardModel state, int depthLimit, long startTime)
	{
		currentDepth += 1;

		//System.out.println("MINMOVE RAN, CURRENT DEPTH = :" + currentDepth);
		//if (currentDepth >= depthLimit)
		if(System.nanoTime() - startTime > TIMELIMIT || currentDepth >= depthLimit)
		{
			double eval = EvalState(state, opponent);
			hMap.put(state, eval);
			return eval;
		}

		ArrayList<Point> moves = generateMoves();
		PriorityQueue<PointWithScore> movesWithScores = new PriorityQueue<PointWithScore>(11);
		double bestMoveVal = Double.MAX_VALUE;
		boolean childrenExplored = false;
		BoardModel stateCopy = null;
		int localDepth = currentDepth;

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
			//System.out.println("POPPING OFF QUEUE: MIN-");
			while(!movesWithScores.isEmpty())
			{
				currentDepth = localDepth;
				stateCopy = state.clone();
				stateCopy.placePiece(movesWithScores.remove(), opponent);
				double maxMove = MaxMove(stateCopy, depthLimit, startTime);
				if(maxMove < bestMoveVal)
				{
					bestMoveVal = maxMove;
				}
			}
		}
		// if not in hashMap then just do DFS with no ordering
		else
		{
			for(Point move: moves)
			{
				currentDepth = localDepth;
				stateCopy = state.clone();
				stateCopy.placePiece(move, opponent);
				double maxMove = MaxMove(stateCopy, depthLimit, startTime);
				if(maxMove < bestMoveVal)
					bestMoveVal = maxMove;
			}
		}
		hMap.put(state, bestMoveVal);
		return bestMoveVal;
	}

	private double EvalState(BoardModel state, byte player1)
	{
		double scorePlayer1 = 0.0;
		double scorePlayer2 = 0.0;

		int countP1 = 0;
		int countP2 = 0;

		byte player2 = (byte)((player1 == 1) ? 2 : 1);

		int l = kLength - 1;

		// search for possible lines
		// horizontal
		for(int i = 0; i < width - l; i++)
		{	
			// traverse up
			for(int j = 0; j < height; j++)
			{
				// check kLength spaces to the right
				for(int k = 0; k < kLength; k++)
				{
					if(state.pieces[j][i + k] == player1)
						countP1++;

					else if(state.pieces[j][i + k] == player2)
						countP2++;
				}

				// player1 has checker(s), player2 doesn't -> player1 advantage
				if((countP1 != 0) && (countP2 == 0))
					scorePlayer1 += applyWeight(countP1);

				// player2 has checker(s), player1 doesn't -> player2 advantage 
				else if((countP2 != 0) && (countP1 == 0))
					scorePlayer2 += applyWeight(countP2);

				// otherwise, both player can play the line or
				// the line has mixture of player1's and player2's checkers
				// thus, it does not help any player
				// reset counter
				countP1 = 0;
				countP2 = 0;
			}
		}

		// vertical
		for(int i = 0; i < width; i++)
		{	
			for(int j = 0; j < height - l; j++)
			{
				for(int k = 0; k < kLength; k++)
				{
					if(state.pieces[j + k][i] == player1)
						countP1++;

					else if(state.pieces[j + k][i] == player2)
						countP2++;
				}

				if((countP1 != 0) && (countP2 == 0))
					scorePlayer1 += applyWeight(countP1);

				else if((countP2 != 0) && (countP1 == 0))
					scorePlayer2 += applyWeight(countP2);

				countP1 = 0;
				countP2 = 0;
			}
		}

		// diagonal /
		for(int i = 0; i < width - l; i++)
		{	
			for(int j = 0; j < height - l; j++)
			{
				for(int k = 0; k < kLength; k++)
				{
					if(state.pieces[j + k][i + k] == player1)
						countP1++;

					else if(state.pieces[j + k][i + k] == player2)
						countP2++;
				}

				if((countP1 != 0) && (countP2 == 0))
					scorePlayer1 += applyWeight(countP1);

				else if((countP2 != 0) && (countP1 == 0))
					scorePlayer2 += applyWeight(countP2);

				countP1 = 0;
				countP2 = 0;
			}
		}

		// diagonal \
		for(int i = l; i < width; i++)
		{	
			for(int j = 0; j < height - l; j++)
			{
				for(int k = 0; k < kLength; k++)
				{
					if(state.pieces[j + k][i - k] == player1)
						countP1++;

					else if(state.pieces[j + k][i - k] == player2)
						countP2++;
				}

				if((countP1 != 0) && (countP2 == 0))
					scorePlayer1 += applyWeight(countP1);

				else if((countP2 != 0) && (countP1 == 0))
					scorePlayer2 += applyWeight(countP2);

				countP1 = 0;
				countP2 = 0;
			}
		}

		return scorePlayer1 - scorePlayer2;
	}

	private double applyWeight(int n)
	{
		return n * n * n * n;
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
	private void updateXHeight(int x)
	{
		xHeight.set(x, xHeight.get(x)+1);
	}

	@SuppressWarnings("unused")
	private boolean getMoveGravityOn(BoardModel state)
	{
		return state.gravityEnabled();
	}

	@SuppressWarnings("unused")
	private Point getMoveGravityOff(BoardModel state)
	{
		return null;
	}
}
