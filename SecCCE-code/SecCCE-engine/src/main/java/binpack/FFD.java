package binpack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import cce.ruleupload.BPdata;

public class FFD {
    // Returns number of bins required using first fit
    // decreasing offline algorithm
    public static ArrayList<ArrayList<String>> firstFitDec(Integer weight[], ArrayList<BPdata> bpData, int bincap) {
        // First sort all weights in decreasing order
    	Arrays.sort(weight, Collections.reverseOrder());
    	
        // Initialize result (Count of bins)
	    int res = 0;
	    
	    // Create an array to store remaining space in bins
	    // there can be at most n bins
	    int n = weight.length;
	    int []bin_rem = new int[n];
	 
	    ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
	    
	    //define safety limit to bincap: 0.95, 0.9, ...
	    bincap = (int) (bincap * 0.9); 
	    System.out.println("Bincap used by FFD: " + bincap);
	    
	    // Place items one by one
	    for (int i = 0; i < n; i++){
	        // Find the first bin that can accommodate
	        // weight[i]
	    	String devName = new String();
	    	for (int k = 0; k < bpData.size(); k++) {
				if (bpData.get(k).cost.equals(weight[i])) {
					devName = bpData.get(k).name;
					bpData.remove(k);
					break;
				}
			}
	        int j;
	        for (j = 0; j < res; j++){
	            if (bin_rem[j] >= weight[i]){
	                bin_rem[j] = bin_rem[j] - weight[i];
	                ret.get(j).add(devName);
	                break;
	            }
	        }
	 
	        // If no bin could accommodate weight[i]
	        if (j == res){
	            bin_rem[res] = bincap - weight[i];
	            ret.add(new ArrayList<String>());
	            ret.get(j).add(devName);
	            res++;
	        }
	    }
	    for (int i = 0; i < ret.size(); i++) {
        	System.out.println("Binpack " + i + " = " + ret.get(i));
	    }
	    return ret;
    }
    
    // Driver code
    public static void main(String[] args){
//    	long startTime = System.currentTimeMillis();
//        long estimatedTime;
//        
//        Integer weight[] = { 2, 5, 4, 7, 1, 3, 8 };
//        String names[] = {"a","b","c","d","e","f","g"};
//        ArrayList<String> namesArray = new ArrayList<String>();
//        for (String string : names) {
//			namesArray.add(string);
//		}
//        
//        int c = 10;
//        estimatedTime = System.currentTimeMillis() - startTime;
//        System.out.println("in " + estimatedTime + " ms");
//        ArrayList<ArrayList<String>> ret =  firstFitDec(weight, namesArray, c);
//        //System.out.println("Number of bins required in First Fit " + "Decreasing : " + firstFitDec(weight, names, c));
//        for (int i = 0; i < ret.size(); i++) {
//        	System.out.println("Bin " + i + " = " + ret.get(i));
//		}
    }
}
 
// This code is contributed by Rajput-Ji
