import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.util.Comparator;
import java.util.PriorityQueue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

class PointWithScore extends Point implements  Comparator<PointWithScore>
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
	public int compareTo(PointWithScore o) {
		if(this.score < o.score)
			return -1;
		else if(this.score > o.score)
			return 1;
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int compare(PointWithScore o1, PointWithScore o2) {
		return 0 - o1.compareTo(o2);
	}
}

public class IAAITest {
	PriorityQueue<PointWithScore> pq;
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		Comparator<PointWithScore> comp = new PointWithScore(new Point(1,2),3);
		pq = new PriorityQueue<PointWithScore>(11,comp);
		
	}

	@Test
	public void test() {
		PointWithScore x = new PointWithScore(new Point(1,2), 2);
		PointWithScore y = new PointWithScore(new Point(1,6), 10);
		PointWithScore z = new PointWithScore(new Point(4,3), 5);
		
		System.out.println(pq.comparator());
		pq.add(x);
		pq.add(y);
		pq.add(z);
		
		System.out.println(pq.peek().getScore());
		assertTrue(pq.poll().getScore() == 10);
		assertTrue(pq.poll().getScore() == 5);
		assertTrue(pq.poll().getScore() == 2);

	}

}
