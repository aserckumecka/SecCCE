package binpack;

public class FF {
	// Returns number of bins required using first fit
	// online algorithm
	static int firstFit(Integer[] weight, int n, int c){
	    // Initialize result (Count of bins)
	    int res = 0;
	 
	    // Create an array to store remaining space in bins
	    // there can be at most n bins
	    int []bin_rem = new int[n];
	 
	    // Place items one by one
	    for (int i = 0; i < n; i++){
	        // Find the first bin that can accommodate
	        // weight[i]
	        int j;
	        for (j = 0; j < res; j++){
	            if (bin_rem[j] >= weight[i]){
	                bin_rem[j] = bin_rem[j] - weight[i];
	                break;
	            }
	        }
	 
	        // If no bin could accommodate weight[i]
	        if (j == res){
	            bin_rem[res] = c - weight[i];
	            res++;
	        }
	    }
	    return res;
	}
	 
	// Driver program
	public static void main(String[] args){
	    Integer weight[] = { 2, 5, 4, 7, 1, 3, 8 };
	    int c = 10;
	    int n = weight.length;
	    System.out.print("Number of bins required in First Fit : " + firstFit(weight, n, c));
	}
	// This code is contributed by Rajput-Ji
}
