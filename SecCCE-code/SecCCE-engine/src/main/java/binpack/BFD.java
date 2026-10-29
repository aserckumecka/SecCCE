package binpack;

import java.util.Arrays;
import java.util.Collections;

public class BFD {
	 /* Copy firstFit() from above */
	 
    // Returns number of bins required using first fit
    // decreasing offline algorithm
    static int bestFitDec(Integer weight[], int n, int c){
        // First sort all weights in decreasing order
        Arrays.sort(weight, Collections.reverseOrder());
         
     // Initialize result (Count of bins)
	    int res = 0;
	 
	    // Create an array to store
	    // remaining space in bins
	    // there can be at most n bins
	    int []bin_rem = new int[n];
	 
	    // Place items one by one
	    for (int i = 0; i < n; i++){
	         
	        // Find the best bin that
	        // can accomodate
	        // weight[i]
	        int j;
	 
	        // Initialize minimum space
	        // left and index
	        // of best bin
	        int min = c + 1, bi = 0;
	 
	        for (j = 0; j < res; j++){
	            if (bin_rem[j] >= weight[i] &&
	                bin_rem[j] - weight[i] < min)
	            {
	                bi = j;
	                min = bin_rem[j] - weight[i];
	            }
	        }
	 
	        // If no bin could accommodate weight[i],
	        // create a new bin
	        if (min == c + 1)
	        {
	            bin_rem[res] = c - weight[i];
	            res++;
	        }
	        else // Assign the item to best bin
	            bin_rem[bi] -= weight[i];
	    }
	    return res;
    }
 
    // Driver code
    public static void main(String[] args){
    	long startTime = System.currentTimeMillis();
        long estimatedTime;
        
        Integer weight[] = { 2, 5, 4, 7, 1, 3, 8 };
        int c = 10;
        int n = weight.length;
        estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("in " + estimatedTime + " ms");
        System.out.print("Number of bins required in First Fit " + "Decreasing : " + bestFitDec(weight, n, c));
    }
}
