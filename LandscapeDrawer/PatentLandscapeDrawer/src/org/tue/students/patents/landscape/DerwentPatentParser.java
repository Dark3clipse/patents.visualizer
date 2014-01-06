package org.tue.students.patents.landscape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class DerwentPatentParser {
	
	private DIIFile f;
	private boolean generatedPatentValueList = false;
	HashMap<String, Integer> patents;
	
	/** Creates a HashMap contaning all patents. The key is the patent number and the value is equal to: -(occurances-1) 
	 * @param families if true, the value is the family size. */
	public HashMap<String, Integer> createListOfPatents(boolean families){
		//put all patent numbers in a HashMap. the integer represents the number of occurances in negative
		HashMap<String, Integer> patents = new HashMap<String, Integer>();
		HashMap<String, Integer> patentsfam = new HashMap<String, Integer>();
		for(int i=0;i<f.getLineNumbers();i++){
			
			//each line contains patent families. Therefore each row returns patent numbers as an array
			ArrayList<String> p = f.getDataArray(DIIFile.PATENT_NUMBER, i);

			//put all patents in an HashMap
			for(int j=0;j<p.size();j++){
				String str = p.get(j).trim();
				Integer result = patents.put(str, 0 );
				Integer result2 = patentsfam.put(str, p.size() );
				if (result != null){
					patents.put(str, ((int)result)-1);
					patentsfam.put(str, ((int)result2 + p.size()));
				}
			}
		}
		
		return families?patentsfam:patents;
	}
	
	/** Creates a list containing all inventor codes as keys. 
	 * @param value determines what the value should represent. 
	 * If 0, represents the number of patents owned by the company. 
	 * If 1, value is equal to -(occurances-1). */
	public HashMap<String, Integer> createListOfInventors(int value){
		HashMap<String, Integer> inventorCode = new HashMap<String, Integer>();
		HashMap<String, Integer> inventorCited = new HashMap<String, Integer>();
		for(int i=0;i<f.getLineNumbers();i++){
			String s = f.getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i);
			ArrayList<String> p = f.getDataArray(DIIFile.PATENT_NUMBER, i);
			String[] parts = s.split(";");
			for(int j=0;j<parts.length;j++){
				String[] sub = parts[j].split("\\(");
				String code = sub[1].replaceAll("\\)", "");
				Integer result = inventorCode.put(code, p.size());//1
				Integer result2 = inventorCited.put(code, 0);
				if (result != null){
					inventorCode.put(code, (int)result+p.size());//1
					inventorCited.put(code, (int)result2-1);
				}
			}
		}
		
		return value==0?inventorCode:inventorCited;
	}
	
	/** Creates a list of all patents cited. */
	public ArrayList<String> createListOfCitedPatents(){
		ArrayList<String> citedPatents = new ArrayList<String>();
		for(int i=0;i<f.getLineNumbers();i++){
			ArrayList<String> citationsRaw = new ArrayList<String>(Arrays.asList(f.getData(DIIFile.CITED_PATENT_NUMBER, i).split(";")));
			
			for(int j=0;j<citationsRaw.size();j++){
				String[] temp = citationsRaw.get(j).split("--");
				for(int k=0;k<temp.length;k++){
					String[] temp2 = temp[k].split(" ");
					for(int h=0;h<temp2.length;h++){
						if (temp2[h].length() < 4){
							continue;
						}
						int count = 0;
						for (int p = 0, len = temp2[h].length(); p < len; p++) {
						    if (Character.isDigit(temp2[h].charAt(p))) {
						        count++;
						    }
						}
						if (count < 4){
							continue;
						}
						
						citedPatents.add(temp2[h]);
					}
				}
			}
		}
		
		return citedPatents;
	}
	
	/** Generates javascript to be invoked in Derwent. The javascript is able to search all cited patents.
	 * The results should be downloaded and provided to this program. 
	 * @param citedPatents list of all patents to be searched for. These are the patents cited by your primary patent set. 
	 * @param outputOneByOne if true, returns each single search query and waits for input to output the next one (to console). */
	public ArrayList<String> generateCitedPatentQuery(ArrayList<String> citedPatents, boolean outputOneByOne){
		Scanner s = new Scanner(System.in);
		ArrayList<String> queries = new ArrayList<String>();
		int i=0;
		while(i < citedPatents.size()){
			String query = "CP=(";
			for(int j=0;j<50 && i < citedPatents.size();i++,j++){
				if (j != 0){
					query += " OR ";
				}
				query+=citedPatents.get(i);
			}
			query+=")";
			
			
			//generate javascript
			String js = "document.getElementById('value(input1)').value='"+query+"';";
			js += "document.getElementById('DIIDW_AdvancedSearch_input_form').submit();";
			queries.add(js);
			
			if (outputOneByOne){
				while(s.hasNext()){
					s.next();
					break;
				}
			}
		}
		s.close();
		
		return queries;
	}
	
	/** Creates a list of forward citations by reading the secondary set of patents.
	 * @param files the number of files available 
	 * @returns all patent numbers sited by the patents that are cited by your primary set of patents. */
	public ArrayList<String> createListOfForwardCitations(int files){
		DIIFile[] citationFiles = new DIIFile[files];
		ArrayList<String> citedPatents = new ArrayList<String>();
		for(int q=0;q<files;q++){
			citationFiles[q] = new DIIFile();
			citationFiles[q].read("c"+(q+1)+".txt");
			
			//get all cited patents in this file
			for(int i=0;i<citationFiles[q].getLineNumbers();i++){
				ArrayList<String> citationsRaw = new ArrayList<String>(Arrays.asList(citationFiles[q].getData(DIIFile.CITED_PATENT_NUMBER, i).split(";")));
				for(int j=0;j<citationsRaw.size();j++){
					String[] temp = citationsRaw.get(j).split("--");
					for(int k=0;k<temp.length;k++){
						String[] temp2 = temp[k].split(" ");
						for(int h=0;h<temp2.length;h++){
							if (temp2[h].length() < 4){
								continue;
							}
							int count = 0;
							for (int p = 0, len = temp2[h].length(); p < len; p++) {
							    if (Character.isDigit(temp2[h].charAt(p))) {
							        count++;
							    }
							}
							if (count < 4){
								continue;
							}
							citedPatents.add(temp2[h]);
						}
					}
				}
			}
			
			citationFiles[q].clear();
		}
		
		return citedPatents;
	}
	
	/** Counts the times a patent has been cited. Places the result in the values of the HashMap that you provide.
	 * @param patents HashMap containing patent numbers as keys and -(occurances-1) as value. 
	 * @param citedPatents list containing all patent numbers cited by your second generation of patents. (forward cited patents) */
	public void countForwardCitations(HashMap<String, Integer> patents, ArrayList<String> citedPatents){
		for(int d = 0;d<citedPatents.size();d++){
			if (patents.containsKey(citedPatents.get(d))){
				Integer oldValue = patents.put(citedPatents.get(d), 1);
				patents.put(citedPatents.get(d), (int)oldValue+1);
			}
		}
	}
	
	/** Creates a list of forward citations per company by reading the secondary set of patents.
	 * Then it counts how many times each company cited one of your primary patents. It places the results in your HashMap.
	 * @param files the number of files available 
	 * @param inventorCited HashMap with inventor codes as keys and -(occurances-1) as values. */
	public void countForwardCompanyCitations(int files, HashMap<String, Integer> inventorCited){
		DIIFile[] citationFiles = new DIIFile[files];
		for(int q=0;q<files;q++){
			citationFiles[q] = new DIIFile();
			citationFiles[q].read("c"+(q+1)+".txt");
		
			for(int i=0;i<citationFiles[q].getLineNumbers();i++){
				String s = citationFiles[q].getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i);
				String[] parts = s.split(";");
				for(int j=0;j<parts.length;j++){
					String[] sub = parts[j].split("\\(");
					if (sub.length < 2){
						continue;
					}
					String code = sub[1].replaceAll("\\)", "");
					Integer result = inventorCited.put(code, 1);
					if (result != null){
						inventorCited.put(code, (int)result+1);
					}
				}
			}
			
			citationFiles[q].clear();
		}
	}
	
	/** Sorts a HashMap based on the values.
	 * @param map the map to be sorted. */
	public TreeMap<String, Integer> sortHashMap(HashMap<String, Integer> map){
		ValueComparator bvc =  new ValueComparator(map);
        TreeMap<String,Integer> sortedMap = new TreeMap<String,Integer>(bvc);
        sortedMap.putAll(map);
        return sortedMap;
	}
	
	/** Creates a new Map containing inventor names instead of inventor codes as keys. 
	 * Does NOT replace the inserted HashMap.*/
	public HashMap<String, Integer> createMapWithInventorNamesFromCodes(HashMap<String, Integer> inventorCodeMap){
		HashMap<String, Integer> inventorName = new HashMap<String, Integer>();
	    HashMap<String, Integer> checkDoubles = new HashMap<String, Integer>();
	    for(int i=0;i<f.getLineNumbers();i++){
			String s = f.getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i);
			String[] parts = s.split(";");
			for(int j=0;j<parts.length;j++){
				String[] sub = parts[j].split("\\(");
				String code = sub[1].replaceAll("\\)", "");
				int result = inventorCodeMap.get(code);
				if (checkDoubles.put(code, result) == null){
					inventorName.put(sub[0].trim(), result);
				}
			}
		}
	    return inventorName;
	}
	
	/** Creates a new Map containing inventor names instead of inventor codes as keys. 
	 * Does NOT replace the inserted HashMap.*/
	public HashMap<String, HashMap<String, Integer>> createMapWithInventorNamesFromCodes3D(HashMap<String, HashMap<String, Integer>> inventorCodeMap){
		HashMap<String, HashMap<String, Integer>> inventorName = new HashMap<String, HashMap<String, Integer>>();
	    HashMap<String, HashMap<String, Integer>> checkDoubles = new HashMap<String, HashMap<String, Integer>>();
	    for(int i=0;i<f.getLineNumbers();i++){
			String s = f.getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i);
			String[] parts = s.split(";");
			for(int j=0;j<parts.length;j++){
				String[] sub = parts[j].split("\\(");
				String code = sub[1].replaceAll("\\)", "");
				HashMap<String, Integer> result = inventorCodeMap.get(code);
				if (checkDoubles.put(code, result) == null){
					inventorName.put(sub[0].trim(), result);
				}
			}
		}
	    return inventorName;
	}
	
	public void printMap(TreeMap<String, Integer> map){
		Iterator it = map.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        System.out.println(pairs.getKey()+" = "+pairs.getValue());
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	}
	
	public DerwentPatentParser(){

		//read the file
		f = new DIIFile();
		f.read("data.txt");
		
		//run the example
		example();
	}
	
	public DIIFile getPrimaryFile(){
		return f;
	}
	
	public void example(){
		//get a nice list of all patents
		HashMap<String, Integer>patents = createListOfPatents(false);
		HashMap<String, Integer>patentsFam = createListOfPatents(true);
		
		//get patent inventors
		HashMap<String, Integer> inventorCode = createListOfInventors(0);
		HashMap<String, Integer> inventorCited = createListOfInventors(1);
		
		//Generate search queries to obtain generation 2.
		/*ArrayList<String> citedPatents = createListOfCitedPatents();
		generateCitedPatentQuery(citedPatents, true);*/
		
		//analyze generation 2
		int files = 26;
		ArrayList<String> citedPatentsForward = createListOfForwardCitations(files);
		countForwardCitations(patents, citedPatentsForward);
		countForwardCompanyCitations(files, inventorCited);
	    
	    //translate inventor codes to inventor names
		HashMap<String, Integer> inventorName = createMapWithInventorNamesFromCodes(inventorCode);
		HashMap<String, Integer> inventorCitedName = createMapWithInventorNamesFromCodes(inventorCited);
	    
		//sort the results
		TreeMap<String, Integer> sortedPatents = sortHashMap(patents);
		TreeMap<String, Integer> sortedPatentsFam = sortHashMap(patentsFam);
		TreeMap<String, Integer> sortedInventorName = sortHashMap(inventorName);
		TreeMap<String, Integer> sortedInventorCitedName = sortHashMap(inventorCitedName);
		
		//print the results
		printMap(sortedPatents);
	}
	
	/** @return the value of a particular patent based on forward citations. 0 if not found. */
	public int getPatentValue(String patent){
		if (!generatedPatentValueList){
			patents = createListOfPatents(false);
			
			int files = 26;
			ArrayList<String> citedPatentsForward = createListOfForwardCitations(files);
			countForwardCitations(patents, citedPatentsForward);
			
			generatedPatentValueList = true;
		}
		
		Integer value = patents.get(patent);
		if (value != null){
			return value;
		}else{
			System.out.println("patent not found! "+patent);
			return 0;
		}
	}
	
	class ValueComparator implements Comparator<String> {

	    Map<String, Integer> base;
	    public ValueComparator(Map<String, Integer> base) {
	        this.base = base;
	    }

	    // Note: this comparator imposes orderings that are inconsistent with equals.    
	    public int compare(String a, String b) {
	        if (base.get(a) >= base.get(b)) {
	            return 1;
	        } else {
	            return -1;
	        } // returning 0 would merge keys
	    }
	}
	
	public static void main (String[] args){
		new DerwentPatentParser();
	}
}