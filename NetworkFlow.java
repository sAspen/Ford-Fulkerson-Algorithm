import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Scanner;
import java.util.TreeSet;


public class NetworkFlow {
	
	private int M, //Number of vertices
		maxFlow; //Optimal flow
	private int capacity[][], //Capacities of edges
		flow[][]; //Flows of edges
	
	private File inputFile, outputFile;
	private PrintWriter out;
	
	/**
	 * Constructor for an instance of NetworkFlow. 
	 * Makes sure that the input file can be read, and that the output file can be created or deleted.
	 * @param infname	filename of the input file
	 * @param outfname	filename of the output file
	 */
	public NetworkFlow(String infname, String outfname) throws FileNotFoundException {
		inputFile = new File(infname);
		if (!inputFile.canRead()) {
			System.out.println("File " + infname + " could be not found, or cannot be read!");
			System.exit(0);
		}
		outputFile = new File(outfname);
		if (outputFile.exists() && !outputFile.delete()) {
			System.out.println("File " + outfname + " could not be deleted!");
			System.exit(0);
		}
		out = new PrintWriter(outputFile);
	}

	/**
	 * Breadth-first search for an augmenting path in the graph from the source to the sink.
	 * 
	 * Also calculates residuals.
	 * 
	 * @param source source/start vertex
	 * @param sink sink/target vertex
	 * @return an int array, containing backtracable path from source to sink, 
	 * and the residual of the final edge in the path
	 */
	private int[] breadthFirstFindPath(int source, int sink) {
		int path[] = new int[M + 1], //Augmenting path from source to sink. path[i] == -1 means that vertex i isn't in the path.
		res[] = new int[M], //Array of residuals to a vertex
		c, //Current vertex
		n; //Next vertex
		Arrays.fill(path, -1); //Initial values in the path array. 
		
		//Make sure the residual to the source is never favored.
		res[0] = Integer.MAX_VALUE;
		
		//Queue
		ArrayDeque<Integer> q = new ArrayDeque<Integer>();
		//First element is the source
		q.add(source); 
		
		while (!q.isEmpty()) {
			c = q.remove();
			
			for (n = 0; n < M; n++) 
				//If there is an edge between c and n
				if (capacity[c][n] > 0) {
					//Possible residual from c to n
					int r = capacity[c][n] - flow[c][n];
					//If the residual is positive and this vertex isn't already in the path
					if (r > 0 && path[n] == -1) {
						//c is now n's parent
						path[n] = c;
						//Residual to n is the smallest of the residual to c and the residual from c to n.
						res[n] = Math.min(res[c], r);
						
						//If n isn't the sink
						if (n != sink)
							//Add n to the queue
							q.add(n);
						else {
							//Record the final residual in the array and return it
							path[M] = res[sink]; 
							return path;
						}
					}
				}
		}
		//No path available from the source to the sink
		return null;
	}
	
	/**
	 * Breadth-first search for the source side of the current cut.
	 * 
	 * Actually just tries to find an augmenting path in the graph from the sink to the source, and returns the unfinished path on failure to find such a path.
	 * 
	 * @return an int array, with values greater than -1 denoting that the vertex is in the source side of the cut
	 */
	private int[] breadthFirstFindCutSourceSide() {
		int path[] = new int[M + 1], //Augmenting path from source to sink. path[i] == -1 means that vertex i isn't in the path.
		c, //Current vertex
		n; //Next vertex
		Arrays.fill(path, -1); //Initial values in the path array. 
		
		//Queue
		ArrayDeque<Integer> q = new ArrayDeque<Integer>();
		//First element is the source
		q.add(0); 
		
		while (!q.isEmpty()) {
			c = q.remove();
			
			for (n = 0; n < M; n++) 
				//If there is an edge between c and n
				if (capacity[c][n] > 0) {
					//Possible residual from c to n
					int r = capacity[c][n] - flow[c][n];
					//If the residual is postive and this vertex isn't already in the path
					if (r > 0 && path[n] == -1) {
						//c is now n's parent
						path[n] = c;
						
						//If n isn't the sink
						if (n != M -1)
							//Add n to the queue
							q.add(n);
						else {
							//If there's an optimal flow in the graph, then we should never come here...
							return path;
						}
					}
				}
		}
		
		//Return the unfinished augmenting path from the source to the sink.
		return path;
	}
	
	/**
	 * Creates an optimal flow in the graph from the source (vertex 1) to the sink (vertex M), which is stored in the flow[][] array.
	 */
	private void fordFulkerson() {
		for (int path[] = breadthFirstFindPath(0, M - 1); 
		path != null && path[M] != 0; //While there's still an augmenting path from the sink to the source
		path = breadthFirstFindPath(0, M - 1)) { //Try to find an augmenting path from the sink to the source
			maxFlow += path[M]; //Path[M] has the value of the residual to the sink in the augmenting path, so we add it to the current flow.
			
			//Now go through the path and modify the flows over the edges with the residual to the sink.
			for (int n = M - 1, c = path[n]; n != 0; n = c, c = path[c]) {
				flow[c][n] += path[M];
				flow[n][c] -= path[M];
			}
		}
	}
	
	/**
	 * Creates an optimal flow in the graph using Ford-Fulkerson.
	 * 
	 * Does some initializing before letting the algorithm get to work.
	 */
	public void maxFlow() {
		maxFlow = 0;
		
		flow = new int[M][M];
		for (int i = 0; i < M; i++)
			for (int j = 0; j < M; j++)
				flow[i][j] = flow[i][j];
		
		fordFulkerson();
	}
	
	
	/**
	 * Outputs to the output file the optimal flow, the flow over each edge and the source side of a cut.
	 */
	public void outputToFile() {
		//Write the optimal flow
		out.println(maxFlow);
		
		//Write the flow over each edge
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < M; j++)
				out.print((flow[i][j] >= 0 ? flow[i][j] : 0) + " "); //Write a zero if the flow is "negative"
			out.println();
		}
		out.flush();
		
		//Get the source side of a cut
		int path[] = breadthFirstFindCutSourceSide();
		
		//Sort the vertices by adding them to a TreeSet.
		TreeSet<Integer> minCut = new TreeSet<Integer>();
		minCut.add(0);
		for (int i = 0; i < M; i++)
			if (path[i] != -1) minCut.add(i);
		
		//Write the source side of the cut
		for (int i : minCut) 
			out.print(i + 1 + " ");
		
		out.close();
	}
	
	/**
	 * Processes the input file.
	 */
	public void readInputFile() throws FileNotFoundException {
		Scanner s = null;
		s = new Scanner(inputFile);
		
		M = s.nextInt();
		
		capacity = new int[M][M];
		for (int i = 0; i < M; i++) 
			for (int j = 0; j < M; j++) 
				capacity[i][j] = s.nextInt();
		
		s.close();
	}
	
}
