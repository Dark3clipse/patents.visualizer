package org.tue.students.patents.assignment2;

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
	
	public static final class Settings{
		public static final boolean generateQueries = false;
		public static final boolean outputQueriesOneByOne = false;
	}
	
	public DerwentPatentParser(){

		//read the file
		f = new DIIFile();
		f.read("data.txt");
		
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
		
		//get patent inventors
		HashMap<String, Integer> inventorCode = new HashMap<String, Integer>();
		HashMap<String, Integer> inventorCited = new HashMap<String, Integer>();
		HashMap<String, Integer> inventorFamSize = new HashMap<String, Integer>();
		for(int i=0;i<f.getLineNumbers();i++){
			String s = f.getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i);
			ArrayList<String> p = f.getDataArray(DIIFile.PATENT_NUMBER, i);
			String[] parts = s.split(";");
			for(int j=0;j<parts.length;j++){
				String[] sub = parts[j].split("\\(");
				String code = sub[1].replaceAll("\\)", "");
				Integer result = inventorCode.put(code, p.size());//1
				Integer result2 = inventorCited.put(code, 0);
				Integer result3 = inventorFamSize.put(code, p.size());
				if (result != null){
					inventorCode.put(code, (int)result+p.size());//1
					inventorCited.put(code, (int)result2-1);
					inventorFamSize.put(code, (int)result3 + p.size());
				}
			}
		}
		
		//get all cited patents
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
		
		//System.out.println("CITED PATENTS: "+citedPatents.toString());
		
		//generate search queries
		if (Settings.generateQueries){
			Scanner s = new Scanner(System.in);
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
				System.out.println(js);
				//break;
				if (Settings.outputQueriesOneByOne){
					while(s.hasNext()){
						s.next();
						break;
					}
				}
			}
		}
		
		//load ALL CITATION DATA FILES!! :O
		int results = 26;
		DIIFile[] citationFiles = new DIIFile[results];
		for(int q=0;q<results;q++){
			citationFiles[q] = new DIIFile();
			citationFiles[q].read("c"+(q+1)+".txt");
			
			//get all cited patents
			ArrayList<String> citedPatentsLocal = new ArrayList<String>();
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
							citedPatentsLocal.add(temp2[h]);
						}
					}
				}
			}
			
			//loop through all cited patents
			for(int d = 0;d<citedPatentsLocal.size();d++){
				if (patents.containsKey(citedPatentsLocal.get(d))){
					Integer oldValue = patents.put(citedPatentsLocal.get(d), 1);
					patents.put(citedPatentsLocal.get(d), (int)oldValue+1);
				}
			}
			
			
			
			//get all inventors
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
		
		//sort citation value based
		ValueComparator bvc =  new ValueComparator(patents);
        TreeMap<String,Integer> sortedPatents = new TreeMap<String,Integer>(bvc);
        sortedPatents.putAll(patents);
		
		Iterator it = sortedPatents.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        //System.out.println(pairs.getKey() + " = " + pairs.getValue());
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    //sort family size based
	    ValueComparator bvc2 =  new ValueComparator(patentsfam);
        TreeMap<String,Integer> sortedPatentsFam = new TreeMap<String,Integer>(bvc2);
        sortedPatentsFam.putAll(patentsfam);
		
		Iterator it2 = sortedPatentsFam.entrySet().iterator();
	    while (it2.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it2.next();
	        //System.out.println(pairs.getKey() + " = " + pairs.getValue());
	        it2.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    //translate inventor codes to inventor names
	    HashMap<String, Integer> inventorName = new HashMap<String, Integer>();
	    HashMap<String, Integer> inventorCitedName = new HashMap<String, Integer>();
	    HashMap<String, Integer> inventorFamSizeName = new HashMap<String, Integer>();
	    HashMap<String, Integer> checkDoubles = new HashMap<String, Integer>();
	    for(int i=0;i<f.getLineNumbers();i++){
			String s = f.getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i);
			String[] parts = s.split(";");
			for(int j=0;j<parts.length;j++){
				String[] sub = parts[j].split("\\(");
				String code = sub[1].replaceAll("\\)", "");
				int result = inventorCode.get(code);
				if (checkDoubles.put(code, result) == null){
					inventorName.put(sub[0].trim(), result);
					inventorCitedName.put(sub[0].trim(), inventorCited.get(code));
					inventorFamSizeName.put(sub[0].trim(), inventorFamSize.get(code));
				}
			}
		}
	    
	    //sort inventors patent number
	    ValueComparator bvc3 =  new ValueComparator(inventorName);
        TreeMap<String,Integer> sortedInventors = new TreeMap<String,Integer>(bvc3);
        sortedInventors.putAll(inventorName);
		
		Iterator it3 = sortedInventors.entrySet().iterator();
	    while (it3.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it3.next();
	        System.out.println(pairs.getKey() + " = " + pairs.getValue());
	        it3.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    //sort inventors cited patents
	    ValueComparator bvc4 =  new ValueComparator(inventorCitedName);
        TreeMap<String,Integer> sortedInventorsCited = new TreeMap<String,Integer>(bvc4);
        sortedInventorsCited.putAll(inventorCitedName);
		
		Iterator it4 = sortedInventorsCited.entrySet().iterator();
	    while (it4.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it4.next();
	        //System.out.println(pairs.getKey() + " = " + pairs.getValue());
	        it4.remove(); // avoids a ConcurrentModificationException
	    }
	    
	    //sort inventors fam size
	    ValueComparator bvc5 =  new ValueComparator(inventorFamSizeName);
        TreeMap<String,Integer> sortedInventorsFamSize = new TreeMap<String,Integer>(bvc5);
        sortedInventorsFamSize.putAll(inventorFamSizeName);
		
		Iterator it5 = sortedInventorsFamSize.entrySet().iterator();
	    while (it5.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it5.next();
	        //System.out.println(pairs.getKey() + " = " + pairs.getValue());
	        it5.remove(); // avoids a ConcurrentModificationException
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