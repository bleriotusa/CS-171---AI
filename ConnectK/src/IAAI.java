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
	private boolean qt;
	private static final boolean PRUNE = true;
	private static final long TIMELIMIT = 4800000000L;

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
		// *** UPDATE AFTER EACH PLACEMENT, use last move to update other player
		xHeight = new ArrayList<Integer>();

		for(int i = 0; i < width; i++)
			xHeight.add(0);

		// set player values
		this.player = player;
		this.opponent = (byte)((player == 1) ? 2 : 1);		
	}

	@Override
	public Point getMove(BoardModel state)
	{
		long startTime = System.nanoTime();
		// initialize HashSet for keeping track of each explored game node (each state)'s value
		// a state's value is defined as: 
		// 1. if leaf node, eval(node)
		// 2. if internal , max or min of children nodes
		hMap = new HashMap<BoardModel, Double>();
		System.out.println("GETTING MOVE...");

		// update xHeight
		if(state.getLastMove() != null && state.gravity)
			updateXHeight(state.getLastMove().x);

		// generate list of moves in random order for first time
		ArrayList<Point> moves = generateMoves(state);

		// keep track of the best move so far
		PointWithScore bestMove = new PointWithScore(new Point(), -Double.MAX_VALUE);

		// scan the board to check
		// 1. if we can win with the first move, play the piece if found
		// 2. if not, check if the opponent can win with the first move, block the piece if found

		// check for player's winning and play it
		for(Point move: moves)
		{
			// place a player's piece on the check board
			if(state.clone().placePiece(move, player).winner() == player)
			{
				// update xHeight
				if(state.gravity)
					updateXHeight(move.x);

				return new PointWithScore(move, Double.POSITIVE_INFINITY);
			}
		}

		// check for opponent's winning and block it
		for(Point move: moves)
		{
			// place a player's piece on the check board
			if(state.clone().placePiece(move, opponent).winner() == opponent)
			{
				// update xHeight
				if(state.gravity)
					updateXHeight(move.x);

				return new PointWithScore(move, Double.NEGATIVE_INFINITY);
			}
		}
		
		// start IDS search with a series of Depth Limited Searches
		for(int depthLimit = 0; System.nanoTime() - startTime < TIMELIMIT; depthLimit++)
		{
			// initialize move storage
			listOfMoves = new PriorityQueue<PointWithScore>(11, new ReversePriority());
			// reset depth counter

			// update alpha beta
			double alpha = -Double.MAX_VALUE;
			double beta = Double.MAX_VALUE;
			// if no tree has been made yet (if hashMap doesn't have a record of child at all)... 
			// do a blind search
			if(hMap.get(state.clone().placePiece(moves.get(0), player))== null)	
			// for each move, place the piece on a copy of the board, then send that new board to Min
			// this will cause a DFS to happen and return the lowest value given by making this move
			// then we save the move that results in the best score
				for(Point move: moves)
				{
					currentDepth = 0;
					double min = MinMove(state.clone().placePiece(move, player), depthLimit, startTime, alpha, beta);
					if(min >= bestMove.getScore())
						bestMove = new PointWithScore(move, min);
				}

			// if tree HAS been made, then just check the tree to see which one to start with,
			// then do the same process as the blind search
			else
			{
				for(Point move: moves)
					listOfMoves.add(new PointWithScore(move, hMap.get(state.clone().placePiece(move,player))));
				
				PointWithScore move = listOfMoves.remove();
				bestMove = new PointWithScore(move, MinMove(state.clone().placePiece(move, player), depthLimit, startTime, alpha, beta));
				
				while(!listOfMoves.isEmpty())
				{
					move = listOfMoves.remove();
					currentDepth = 0;
					double min = MinMove(state.clone().placePiece(move, player), depthLimit, startTime, alpha, beta);

					if(min >= bestMove.getScore())
						bestMove = new PointWithScore(move, min);
				}
			}
		}		
		System.out.println("BestMove: " + bestMove.getScore());

		// update xHeight
		if(state.gravity)
			updateXHeight(bestMove.x);

		// clean up and return
		listOfMoves = null;
		moves = null;		

		return bestMove;
	}

	private double MaxMove(BoardModel state, int depthLimit, long startTime, double alpha, double beta)
	{
		currentDepth += 1;

		// check if depthLimit reached. If so, record to hash table and send back eval of state
		if(System.nanoTime() - startTime > TIMELIMIT || currentDepth >= depthLimit || state.winner() != -1)
		{
			double eval = EvalState(state, player);
			hMap.put(state, eval);
			return eval;
		}
		// else, try every possible move, until depth limit is up
		// and save the score for every move
		// then return largest score
		ArrayList<Point> moves = generateMoves(state);
		double bestMoveVal = Double.NEGATIVE_INFINITY;

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
			while(!movesWithScores.isEmpty())
			{
				currentDepth = localDepth;
				stateCopy = state.clone();
				PointWithScore move = movesWithScores.remove();
				stateCopy.placePiece(move, player);
				double minMove = MinMove(stateCopy, depthLimit, startTime, alpha, beta);

				if(minMove > bestMoveVal)
				{
					bestMoveVal = minMove;
					alpha = bestMoveVal;
					if(PRUNE && (alpha >= beta))
						break;
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
				double minMove = MinMove(stateCopy, depthLimit, startTime, alpha, beta);

				if(minMove > bestMoveVal)
				{
					bestMoveVal = minMove;
					alpha = bestMoveVal;
					if(PRUNE && (alpha >= beta))
						break;
				}
			}
		}

		// record this state with the best value found
		hMap.put(state, bestMoveVal);

		return bestMoveVal;
	}

	private double MinMove(BoardModel state, int depthLimit, long startTime, double alpha, double beta)
	{
		currentDepth += 1;

		if(System.nanoTime() - startTime > TIMELIMIT || currentDepth >= depthLimit || state.winner() != -1) 
		{
			double eval = EvalState(state, player);
			
			if(!qt || System.nanoTime() - startTime > TIMELIMIT - 100000000L)
			hMap.put(state, eval);
			return eval;
		}

		ArrayList<Point> moves = generateMoves(state);
		double bestMoveVal = Double.POSITIVE_INFINITY;
		PriorityQueue<PointWithScore> movesWithScores = new PriorityQueue<PointWithScore>(11, new ReversePriority());

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
			while(!movesWithScores.isEmpty())
			{
				currentDepth = localDepth;
				stateCopy = state.clone();
				stateCopy.placePiece(movesWithScores.remove(), opponent);
				double maxMove = MaxMove(stateCopy, depthLimit, startTime, alpha, beta);

				if(maxMove < bestMoveVal)
				{
					bestMoveVal = maxMove;
					beta = bestMoveVal;
					if(PRUNE && (alpha >= beta))
						break;
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
				double maxMove = MaxMove(stateCopy, depthLimit, startTime, alpha, beta);

				if(maxMove < bestMoveVal)
				{
					bestMoveVal = maxMove;
					beta = bestMoveVal;
					if(PRUNE && (alpha >= beta))
						break;
				}
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

		qt = false;
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
					if(state.pieces[i + k][j] == player1)
						countP1++;

					else if(state.pieces[i + k][j] == player2)
						countP2++;
				}

				// player1 has checker(s), player2 doesn't -> player1 advantage
				if((countP1 > 0) && (countP2 == 0))
				{
					scorePlayer1 += applyWeight(countP1);
					qt = quiescenceTest(player1, countP1);
				}

				// player2 has checker(s), player1 doesn't -> player2 advantage 
				else if((countP2 > 0) && (countP1 == 0))
				{
					scorePlayer2 += applyWeight(countP2);
					qt = quiescenceTest(player2, countP2);
				}

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
					if(state.pieces[i][j + k] == player1)
						countP1++;

					else if(state.pieces[i][j + k] == player2)
						countP2++;
				}

				if((countP1 > 0) && (countP2 == 0))
				{
					scorePlayer1 += applyWeight(countP1);
					qt = quiescenceTest(player1, countP1);
				}

				else if((countP2 > 0) && (countP1 == 0))
				{
					scorePlayer2 += applyWeight(countP2);
					qt = quiescenceTest(player2, countP2);
				}

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
					if(state.pieces[i + k][j + k] == player1)
						countP1++;

					else if(state.pieces[i + k][j + k] == player2)
						countP2++;
				}

				if((countP1 > 0) && (countP2 == 0))
				{
					scorePlayer1 += applyWeight(countP1);
					qt = quiescenceTest(player1, countP1);
				}

				else if((countP2 > 0) && (countP1 == 0))
				{
					scorePlayer2 += applyWeight(countP2);
					qt = quiescenceTest(player2, countP2);
				}

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
					if(state.pieces[i - k][j + k] == player1)
						countP1++;

					else if(state.pieces[i - k][j + k] == player2)
						countP2++;
				}

				if((countP1 > 0) && (countP2 == 0))
				{
					scorePlayer1 += applyWeight(countP1);
					qt = quiescenceTest(player1, countP1);
				}

				else if((countP2 > 0) && (countP1 == 0))
				{
					scorePlayer2 += applyWeight(countP2);
					qt = quiescenceTest(player2, countP2);
				}

				countP1 = 0;
				countP2 = 0;
			}
		}

		double score = scorePlayer1 - scorePlayer2;

		return score;
	}

	private double applyWeight(int n)
	{
		if(n == kLength)
			return Double.MAX_VALUE*3;
		
		return n * n * n;
	}

	private boolean quiescenceTest(byte p, int n)
	{
		if(qt)
			return true;
		
		if(p == opponent && n == kLength - 1)
			return true;

		return false;
	}

	@Override
	public Point getMove(BoardModel state, int deadline)
	{
		return getMove(state);
	}

	private ArrayList<Point> generateMoves(BoardModel state)
	{
		ArrayList<Point> moves = new ArrayList<Point>(width);

		if(state.gravity)
		{
			for(int i = 0; i < width; i++)
			{
				if(xHeight.get(i) < height)
					moves.add(new Point(i, xHeight.get(i)));
			}
		}

		else
		{
			for(int i = 0; i < width; i++)
			{
				for(int j = 0; j < height; j++)
				{
					if(state.getSpace(i, j) == 0)
						moves.add(new Point(i, j));
				}
			}
		}
		return moves;
	}
	
	private void updateXHeight(int x)
	{
		xHeight.set(x, xHeight.get(x)+1);
	}
}
