package binpack;

public class NF {
	// Returns number of bins required
    // using next fit online algorithm
    static int nextFit(int weight[], int n, int c){
 
        // Initialize result (Count of bins) and remaining
        // capacity in current bin.
        int res = 0, bin_rem = c;
 
        // Place items one by one
        for (int i = 0; i < n; i++) {
            // If this item can't fit in current bin
            if (weight[i] > bin_rem) {
                res++; // Use a new bin
                bin_rem = c - weight[i];
            }
            else
                bin_rem -= weight[i];
        }
        return res;
    }
 
    // Driver program
    public static void main(String[] args){
        int weight[] = { 2, 5, 4, 7, 1, 3, 8 };
        int c = 10;
        int n = weight.length;
        System.out.println("Number of bins required in Next Fit : " + nextFit(weight, n, c));
    }
}
 
// This code has been contributed by 29AjayKumar
