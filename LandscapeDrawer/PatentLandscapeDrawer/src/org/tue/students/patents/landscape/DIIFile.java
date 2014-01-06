package org.tue.students.patents.landscape;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class DIIFile {
	
	private final int arrayListLength = 24;
	
	//default
	public static final int PATENT_FAMILY = 0;
	public static final int TITLE = 1;
	public static final int INVENTOR = 2;
	public static final int ASSIGNEE_NAME_AND_CODE = 3;
	public static final int DERWENT_PRIMARY_ACCESSION_NUMBER = 4;
	public static final int ABSTRACT = 5;
	//TF
	public static final int EQUIVALENT_ABSTRACT = 7;
	public static final int DERWENT_CLASS_CODE = 8;
	public static final int DERWENT_MANUAL_CODE = 9;
	public static final int INTERNATIONAL_PATENT_CLASSIFICATION = 10;
	public static final int PATENT_DETAILS = 11;
	public static final int APPLICATION_DETAILS_AND_DATE = 12;
	public static final int FURTHER_APPLICATION_DETAILS = 13;
	public static final int PRIORITY_APPLICATION_INFORMATION_AND_DATE = 14;
	public static final int DESIGNATED_STATES = 15;
	public static final int FIELD_OF_SEARCH = 16;
	public static final int CITED_PATENT_NUMBER = 17;
	public static final int CITED_ARTICLES = 18;
	public static final int DCR_NUMBER = 19;
	public static final int MARKUSH_NUMBER = 20;
	public static final int RING_INDEX_NUMBER = 21;
	public static final int DERWENT_REGISTRY_NUMBER = 23;
	
	//special
	public static final int PATENT_NUMBER = -1;
	
	private String path;
	private ArrayList<String> categories;
	private HashMap<Integer, ArrayList<String>> map;
	
	/** @returns a single data cell.
	 * @param type the data type. You can choose from DIIFile's static members.
	 * @param lineNumber the line number to get the data from. */
	public String getData(int type, int lineNumber){
		return map.get(lineNumber).get(type);
	}
	
	/** @returns an array of data.
	 * @param type the data type. You can choose from DIIFile's static members.
	 * @param lineNumber the line number to get the data from. */
	public ArrayList<String> getDataArray(int type, int lineNumber){
		switch(type){
			default:
			case PATENT_NUMBER:
				return new ArrayList<String>(Arrays.asList(getData(PATENT_FAMILY, lineNumber).split(";")));
		}
	}
	
	/** @return the number of lines available. */
	public int getLineNumbers(){
		return map.size();
	}
	
	public void clear(){
		map.clear();
		categories.clear();
	}
	
	public void read(String file){
	    try {
	    	//System.out.println("Initializing...");
	    	/*Path path = FileSystems.getDefault().getPath("data", file);
			BufferedReader reader = Files.newBufferedReader(path, Charset.forName("UTF-16"));*/
	    	FileHandle handle = Gdx.files.internal("data/"+file);
	    	BufferedReader reader = handle.reader(8192, "UTF-16");
	    	
			String line = null;
			ArrayList<String> lines = new ArrayList<String>();
			
			//System.out.println("Reading file...");
			while((line = reader.readLine()) != null){
				lines.add(line);
			}
			//System.out.println("File placed into memory.");
			
			//System.out.println("Creating internal database...");
			categories = lineToArray(lines, 0);
			map = new HashMap<Integer, ArrayList<String>>();
			for(int i=1;i<lines.size();i++){
				map.put(i-1, lineToArray(lines, i));
			}
			reader.close();
			//System.out.println("Internal database created.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<String> lineToArray(ArrayList<String> lines, int nr){
		String[] split = lines.get(nr).split("\\t");
		ArrayList<String> arr = new ArrayList<String>(Arrays.asList(split));
		while(arr.size() < arrayListLength){
			arr.add("");
		}
		return arr;
	}
}