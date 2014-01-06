package org.tue.students.patents.landscape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;

public class LandscapeDrawer implements ApplicationListener {
	private String TAG = "LandscapeDrawer";
	
	private OrthographicCamera camera;
	private SpriteBatch batch;
	private ShapeRenderer shapes;
	private BitmapFont fnt;
	private Matrix4 projMatrix;
	private int w;
	private int h;
	private Mesh mesh;
	
	private float[][] map;
	private ArrayList<PatentKeyword> keywords = new ArrayList<PatentKeyword>();
	private HashMap<String, Vector2> words = new HashMap<String, Vector2>();
	private HashMap<Vector2, Color> dots = new HashMap<Vector2, Color>();
	private HashMap<String, String> classNames;
	private HashMap<String, Color> companyColors = new HashMap<String, Color>();
	
	//drawing options
	private int quality = 256;//Minimum: 2, Maximum: 256
	private float decayConstant = 5;//20
	private float median = .7f;
	private int minValueForWord = 300;
	private int minValueForDot = 1;
	private int minPatentsForInventorLegend = 15;
	
	
	/** Get the height of a part of the map.
	 * @param x the x-position between -1 and 1.
	 * @param y the y-position between -1 and 1.
	 * @return the height between 0 and 1. */
	private float getMapHeight(int i, int j){
		if (map == null){
			computeMap();
		}
		
		return map[i][j];
	}
	
	private void computeMap(){
		LOG.pb(TAG, "generating map");
		
		map = new float[quality][quality];
		float max = 0f;
		for(int i=0;i<quality;i++){
			for(int j=0;j<quality;j++){
				float x = ((float)i / (float)(quality-1))*2 - 1;
				float y = ((float)j / (float)(quality-1))*2 - 1;
				Vector2 pos = new Vector2(x, y);
				float rt = 0;
				for(int h=0;h<keywords.size();h++){
					float length = pos.cpy().sub(keywords.get(h).position).len();
					float height = keywords.get(h).value;
					rt += height * Math.exp(-length * decayConstant);
				}
				map[i][j] = rt;
				if (rt > max){
					max = rt;
				}
			}
		}
		for(int i=0;i<quality;i++){
			for(int j=0;j<quality;j++){
				map[i][j] /= max*median;
			}
		}
		
		LOG.pe();
	}
	
	private java.awt.Color getHeightColor(float height){
		java.awt.Color c;
		float[] cRange = new float[]{.15f, .3f, .4f, .55f, .7f, .86f, 1f};
		if (height < cRange[0]){
			height = cRange[0];
			c = java.awt.Color.getHSBColor((195f-(73f*(height/cRange[0])))/360f, .17f-(.04f*(height/cRange[0])), .83f+(.06f*(height/cRange[0])));
		}else if (height < cRange[1]){
			height = cRange[1];
			c = java.awt.Color.getHSBColor((122f-(2f*((height-cRange[0])/(cRange[1]-cRange[0]))))/360f, .13f, .89f-(.14f*((height-cRange[0])/(cRange[1]-cRange[0]))));
		}else if (height < cRange[2]){
			height = cRange[2];
			c = java.awt.Color.getHSBColor((120f+(12f*((height-cRange[1])/(cRange[2]-cRange[1]))))/360f, .13f+(.08f*((height-cRange[1])/(cRange[2]-cRange[1]))), .75f-(.03f*((height-cRange[1])/(cRange[2]-cRange[1]))));
		}else if (height < cRange[3]){
			height = cRange[3];
			c = java.awt.Color.getHSBColor((132f-(10f*((height-cRange[2])/(cRange[3]-cRange[2]))))/360f, .21f+(.01f*((height-cRange[2])/(cRange[3]-cRange[2]))), .72f-(.09f*((height-cRange[2])/(cRange[3]-cRange[2]))));
		}else if (height < cRange[4]){
			height = cRange[4];
			c = java.awt.Color.getHSBColor((122f-(94f*((height-cRange[3])/(cRange[4]-cRange[3]))))/360f, .22f-(.14f*((height-cRange[3])/(cRange[4]-cRange[3]))), .63f-(.01f*((height-cRange[3])/(cRange[4]-cRange[3]))));
		}else if (height < cRange[5]){
			height = cRange[5];
			c = java.awt.Color.getHSBColor((28f-(2f*((height-cRange[4])/(cRange[5]-cRange[4]))))/360f, .08f-(.03f*((height-cRange[4])/(cRange[5]-cRange[4]))), .62f-(.07f*((height-cRange[4])/(cRange[5]-cRange[4]))));
		}else if (height < cRange[6]){
			height = cRange[6];
			c = java.awt.Color.getHSBColor((26f+(254f*((height-cRange[5])/(cRange[6]-cRange[5]))))/360f, .05f-(.04f*((height-cRange[5])/(cRange[6]-cRange[5]))), .55f+(.41f*((height-cRange[5])/(cRange[6]-cRange[5]))));
		}else{
			c = java.awt.Color.getHSBColor((26f+254f)/360f, .01f, .55f+.41f);
		}
		return c;
	}
	
	private void generateMesh(){
		int verticesMax = quality * quality * 7;
		int indicesMax = (quality-1) * (2 + (quality-1)*2);
		
		//initialize mesh
		mesh = new Mesh(true, verticesMax, indicesMax, 
				new VertexAttribute(Usage.Position, 3, "a_position"),
				new VertexAttribute(Usage.Color, 4, "a_color"));
		
		//generate vertices
		float[] vertices = new float[quality * quality * 7];
		for(int i=0;i<quality;i++){
			for(int j=0;j<quality;j++){
				float x = ((float)i / (float)(quality-1))*2 - 1;
				float y = ((float)j / (float)(quality-1))*2 - 1;
				float height = getMapHeight(i, j);
				java.awt.Color color = getHeightColor(height);
				vertices[(i*quality+j)*7 + 0] = x;
				vertices[(i*quality+j)*7 + 1] = y;
				vertices[(i*quality+j)*7 + 2] = 0;
				vertices[(i*quality+j)*7 + 3] = (float)color.getRed()/255f;
				vertices[(i*quality+j)*7 + 4] = (float)color.getGreen()/255f;
				vertices[(i*quality+j)*7 + 5] = (float)color.getBlue()/255f;
				vertices[(i*quality+j)*7 + 6] = 1;
			}
		}
		mesh.setVertices(vertices);
		
		//generate indices
		short[] indices = new short[indicesMax];
		for(int j=0;j<quality-1;j++){
			indices[quality*j*2 +0] = (short)(j+1);
			indices[quality*j*2 +1] = (short)j;
			for(int i=1;i<quality;i++){
				indices[quality*j*2 + i*2 +0] = (short) (j + i*quality + 1);
				indices[quality*j*2 + i*2 +1] = (short) (j + i*quality);
			}
		}
		mesh.setIndices(indices);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void create() {		
		
		//setup camera
		w = Gdx.graphics.getWidth();
		h = Gdx.graphics.getHeight();
		camera = new OrthographicCamera(w, h);
		projMatrix = camera.combined;
		batch = new SpriteBatch();
		shapes = new ShapeRenderer();
		fnt = new BitmapFont();
		LOG.pb(TAG, "Adding random patents to the map generator");
		
		//add keywords
		//TODO: hier
		//generateDataUsingClassCodes();
		/*HashMap<String, HashMap<Integer, Integer>> classCodeValues = generateClassValues();
		System.out.println(classCodeValues.toString());*/
		
		//key = section code (X, Q, etc), value = HashMap ()
		//key2 = class code (numbers in X01, Q11 etc)
		//value = total value for section
		//int value = getClassCodeValue(classCodeValues, "X23");
		
		DerwentPatentParser parser = new DerwentPatentParser();
		HashMap<String, Integer> map = new  HashMap<String, Integer>();
		for(int i=0;i<parser.getPrimaryFile().getLineNumbers();i++){
			int nr = parser.getPrimaryFile().getDataArray(DIIFile.PATENT_NUMBER, i).size();
			String assignee = parser.getPrimaryFile().getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i);
			String classcode = parser.getPrimaryFile().getData(DIIFile.DERWENT_CLASS_CODE, i);
			String[] cda = assignee.split(";");
			
			for
				for(int j=0;j<cda.length;j++){
					Integer vorig = map.put(cda[j].trim(), nr);
					if (vorig != null){
						map.put(cda[j].trim(), vorig+nr);
					}
			}
			parser.printMap(parser.sortHashMap(map));
			//System.out.println(assignee+"\t"+nr+"\t"+classcode);
		}
		
		//generate the mesh
		LOG.pe();
		generateMesh();
	}
	
	/** @return value of a section. Returns 0 if not found. */
	private int getClassCodeValue(HashMap<String, HashMap<Integer, Integer>> division, String section){
		return division.get(section.substring(0, 1)).get(Integer.parseInt(section.substring(1, 3)));
	}
	
	private HashMap<String, HashMap<Integer, Integer>> generateClassValues(){
		DerwentPatentParser parser = new DerwentPatentParser();
		DIIFile f = parser.getPrimaryFile();
		//HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
		
		HashMap<String, HashMap<Integer, Integer>> division = new HashMap<String, HashMap<Integer, Integer>>();
		HashMap<String, HashMap<Integer, String>> divisionAssignee = new HashMap<String, HashMap<Integer, String>>();
		classNames = new HashMap<String, String>();
		for(int i=0;i<f.getLineNumbers();i++){
			
			
			String line = f.getData(DIIFile.DERWENT_CLASS_CODE, i);
			String[] classes = line.split(";");
			for(int j=0;j<classes.length;j++){
				classes[j] = classes[j].trim();
				String section = classes[j].substring(0, 1);
				String code = classes[j].substring(1, 3);
				int codeI = Integer.parseInt(code);
				HashMap<Integer, Integer> mapSection = division.get(section);
				HashMap<Integer, String> mapSectionAss = divisionAssignee.get(section);
				System.out.println(classes[j]);
				int valueTot = 0;
				ArrayList<String> patents = f.getDataArray(DIIFile.PATENT_NUMBER, i);
				for(int k=0;k<patents.size();k++){
					valueTot += parser.getPatentValue(patents.get(k).trim());
				}
				if (mapSection == null){
					HashMap<Integer, Integer> value = new HashMap<Integer, Integer>();
					value.put(codeI, valueTot);
					division.put(section, value);
					
					HashMap<Integer, String> val = new HashMap<Integer, String>();
					val.put(codeI, f.getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i));
					divisionAssignee.put(section, val);
				}else{
					Integer prev = mapSection.put(codeI, valueTot);
					if (prev != null){
						mapSection.put(codeI, prev+valueTot);
					}
					
					String ass = f.getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i);
					String prev2 = mapSectionAss.put(codeI, ass);
					if (prev2 != null){
						mapSectionAss.put(codeI, prev+";"+ass);
					}
				}
				
				classNames.put(classes[j].substring(0, 3), classes[j].substring(5, classes[j].length()-1));
			}
		}
		
		return division;
	}
	
	private void generateDataUsingClassCodes() {
		DerwentPatentParser parser = new DerwentPatentParser();
		DIIFile f = parser.getPrimaryFile();
		//HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
		HashMap<String, HashMap<Integer, Integer>> division = new HashMap<String, HashMap<Integer, Integer>>();
		HashMap<String, HashMap<Integer, String>> divisionAssignee = new HashMap<String, HashMap<Integer, String>>();
		classNames = new HashMap<String, String>();
		for(int i=0;i<f.getLineNumbers();i++){
			
			String line = f.getData(DIIFile.DERWENT_CLASS_CODE, i);
			String[] classes = line.split(";");
			/*String abstract_i =  f.getData(DIIFile.ABSTRACT, i); // abstract_i string of abstract of line i
			ArrayList<String> abstract_array = new ArrayList<String>(); // abstract_array ; array of all abstracts
			abstract_array.add(abstract_i);*/
			// split all the abstract in individual array.
			// compare all the array components to the keywords
			// count all the corrosponding words for each abstract. 
			// give all keyword a value for each patent, or percentage
			// determine the x and y position based on those presentages
			
			
			
			
			for(int j=0;j<classes.length;j++){
				classes[j] = classes[j].trim();
				String section = classes[j].substring(0, 1);
				String code = classes[j].substring(1, 3);
				int codeI = Integer.parseInt(code);
				HashMap<Integer, Integer> mapSection = division.get(section);
				HashMap<Integer, String> mapSectionAss = divisionAssignee.get(section);
				System.out.println(classes[j]);
				int valueTot = 0;
				ArrayList<String> patents = f.getDataArray(DIIFile.PATENT_NUMBER, i);
				for(int k=0;k<patents.size();k++){
					valueTot += parser.getPatentValue(patents.get(k).trim());
				}
				if (mapSection == null){
					HashMap<Integer, Integer> value = new HashMap<Integer, Integer>();
					value.put(codeI, valueTot);
					division.put(section, value);
					
					HashMap<Integer, String> val = new HashMap<Integer, String>();
					val.put(codeI, f.getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i));
					divisionAssignee.put(section, val);
				}else{
					Integer prev = mapSection.put(codeI, valueTot);
					if (prev != null){
						mapSection.put(codeI, prev+valueTot);
					}
					
					String ass = f.getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i);
					String prev2 = mapSectionAss.put(codeI, ass);
					if (prev2 != null){
						mapSectionAss.put(codeI, prev+";"+ass);
					}
				}
				
				classNames.put(classes[j].substring(0, 3), classes[j].substring(5, classes[j].length()-1));
			}
		}

		//get list of inventors
		HashMap<String, Integer> inventorCode = parser.createListOfInventors(0);
		TreeMap inventorCodeSorted = parser.sortHashMap(inventorCode);
		
		TreeMap treeMap = new TreeMap();
		treeMap.putAll(division);
		Iterator it = treeMap.entrySet().iterator();
		TreeMap treeMapAss = new TreeMap();
		treeMapAss.putAll(divisionAssignee);
		Iterator itAss = treeMapAss.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        Map.Entry pairsAss = (Map.Entry)itAss.next();
	        
	        HashMap<Integer, Integer> dat = (HashMap<Integer, Integer>) pairs.getValue();
	        HashMap<Integer, String> datAss = (HashMap<Integer, String>) pairsAss.getValue();
	        
	        Iterator it2 = dat.entrySet().iterator();
	        Iterator it2Ass = datAss.entrySet().iterator();
	        while (it2.hasNext()) {
	            Map.Entry pairs2 = (Map.Entry)it2.next();
	            Map.Entry pairs2Ass = (Map.Entry)it2Ass.next();
	            
	            String classCode;
	            if ((Integer)pairs2.getKey() < 10){
	            	classCode = pairs.getKey()+"0"+pairs2.getKey();
	            }else{
	            	classCode = pairs.getKey()+""+pairs2.getKey();
	            }
	            int value = (Integer) pairs2.getValue();
	            
	            String assignee = (String)pairs2Ass.getValue();
	            String[] assignees = assignee.split(";");
	            String code = "";
	            boolean first = true;
	            for(int p=0;p<assignees.length;p++){
	            	String[] codeF = assignees[p].split("\\(");
	            	if (codeF.length >= 2){
	            		if (!first){
	            			code+=";";
	            		}else{
	            			first = false;
	            		}
	            		code += codeF[1].substring(0, codeF[1].length()-1);
	            	}
	            }
	            
	            System.out.println(classCode + " = " + value + ", codes: "+code);
	            
	            Vector2 loc = getDerwentClassLocation(classCode);
	            keywords.add(new PatentKeyword(getClassName(classCode), loc, value));
	            if (value >= minValueForWord ){
	            	words.put(getClassName(classCode), loc);
	            }
	            
	            if (value >= minValueForDot){
	            	String[] codes = code.split(";");
	            	for(int u=0;u<codes.length;u++){
	            		
	            		int inventorNrPatents = inventorCode.get(codes[u]);
	            		if (inventorNrPatents >= minPatentsForInventorLegend){
	            		
		            		Vector2 newLoc = loc.cpy();
		            		if (u!=0){
		            			float rand = (float) Math.random();
		            			newLoc = newLoc.add(.05f*(float)Math.cos(rand*Math.PI*2), .05f*(float)Math.sin(rand*Math.PI*2));
		            		}
			            	Color col = companyColors.get(codes[u]);
			            	if (col == null){
			            		col = getNewColor();
			            		companyColors.put(codes[u], col);
			            	}
			            	dots.put(newLoc, col);
	            		}else{
	            			Color col = Color.BLACK;
			            	dots.put(loc, col);
	            		}
	            	}
	            }
	            
	            it2.remove(); // avoids a ConcurrentModificationException
	        }
	        it.remove(); // avoids a ConcurrentModificationException
	    }
	}

	static int colorIndex = 0;
	private static Color getNewColor() {
		Color c;
		
		switch(colorIndex){
			case 0:c=Color.BLUE;break;
			case 1:c=Color.RED;break;
			case 2:c=new Color(1f, .5f, 39f/255f, 1f);break;
			case 3:c=Color.MAGENTA;break;
			case 4:c=Color.PINK;break;
			case 5:c=Color.CYAN;break;
			case 6:c=Color.YELLOW;break;
			case 7:c=Color.GREEN;break;
			case 8:c=new Color(0f, .5f, .5f, 1f);break;
			case 9:c=new Color(126f/255f, 187f/255f, 106f/255f, 1f);break;
			case 10:c=new Color(199f/255f, 202f/255f, 91f/255f, 1f);break;
			case 11:c=new Color(128f/255f, 128f/255f, 0f, 1f);break;
			case 12:c=new Color(.5f, 0, 0f, 1f);break;
			case 13:c=new Color(0, .5f, 0f, 1f);break;
			case 14:c=new Color(122f/255f, 122f/255f, 1f, 1f);break;
			case 15:c=new Color(40f/255f, 40f/255f, 40f/255f, 1f);break;
			case 16:c=new Color(216f/255f, 216f/255f, 216f/255f, 1f);break;
			case 17:c=new Color(64f/255f, 0f, 128f/255f, 1f);break;
			case 18:c=new Color(146f/255f, 36f/255f, 1f, 1f);break;
			case 19:c=new Color(254f/255f, 103f/255f, 133f/255f, 1f);break;
			case 20:c=new Color(0f/255f, 128f/255f, 255f/255f, 1f);break;
			case 21:c=new Color(128f/255f, 0f/255f, 64f/255f, 1f);break;
			case 22:c=new Color(138f/255f, 69f/255f, 0f/255f, 1f);break;
			case 23:c=new Color(13f/255f, 0f/255f, 85f/255f, 1f);break;
			case 24:c=new Color(83f/255f, 51f/255f, 2f/255f, 1f);break;
			case 25:c=new Color(240f/255f, 240f/255f, 240f/255f, 1f);break;
			default:
				c=Color.WHITE;
				System.out.println("Color overflow!");
				break;
		}
		
		colorIndex++;
		return c;
	}

	public String getClassName(String classCode){
		return classNames.get(classCode);
	}
	
	public Vector2 getDerwentClassLocation(String classCode){
		/* L, Refractories, Ceramics, Cement and Electro(in)organics
		 * A, Polymers and Plastics
		 * M, Metallurgy
		 * 
		 * Engineering
		 * 
		 * P, General Engineering
		 * Q, Mechanical
		 * 
		 * Electrical
		 * X, Electric Power Engineering 
		 * U, Semiconductors and Electronic Circuitry, 
		 * T, Computing and Control
		 * W, Communications
		 * V, Electronic Components
		 * S, Instrumentation, Measuring and Testing
		 */
		
		Vector2 v = new Vector2();
		
		String section = classCode.substring(0, 1);
		int nr = Integer.parseInt(classCode.substring(1, 3));
		
		int max = 0;
		if (section.equals("X")){
			v.add(0, 0);
			max = 27;
		}else if (section.equals("Q")){
			v.add(.5f, -.5f);
			max = 79;
		}else if (section.equals("W")){
			v.add(-.3f, .3f);
			max = 7;
		}else if (section.equals("V")){
			v.add(-.5f, 0);
			max = 6;
		}else if (section.equals("U")){
			v.add(.3f, .5f);
			max = 25;
		}else if (section.equals("T")){
			v.add(-.5f, -.5f);
			max = 6;
		}else if (section.equals("S")){
			v.add(.5f, 0);
			max = 5;
		}else if (section.equals("P")){
			v.add(0, -.5f);
			max = 62;
		}else if (section.equals("A")){
			v.add(0, .5f);
			max = 95;
		}else if (section.equals("L")){
			v.add(-.2f, -.7f);
			max = 3;
		}else if (section.equals("M")){
			v.add(.2f, -.7f);
			max = 22;
		}else{
			v.add(1, 1);
		}
		float x = (float)((Math.random()*.2+.2)*Math.cos(((float)nr/(float)max) * 2 * Math.PI));
		float y = (float)((Math.random()*.2+.2)*Math.sin(((float)nr/(float)max) * 2 * Math.PI));
		v.add(x, y);
		
		System.out.println(classCode+" = "+getClassName(classCode));
		
		return v;
	}
	
	private void writePerCountryToFile(DIIFile f, DerwentPatentParser parser){
		HashMap<String, HashMap<String, Integer>> inventorAndCountry = new HashMap<String, HashMap<String, Integer>>();
		HashMap<String, String> countriesList = new HashMap<String, String>();
		for(int i=0;i<f.getLineNumbers();i++){
			ArrayList<String> countries = new ArrayList<String>();
			ArrayList<String> inventors = new ArrayList<String>();
			
			ArrayList<String> line = f.getDataArray(DIIFile.PATENT_NUMBER, i);
			
			
			String s = f.getData(DIIFile.ASSIGNEE_NAME_AND_CODE, i);
			String[] parts = s.split(";");
			for(int j=0;j<parts.length;j++){
				String[] sub = parts[j].split("\\(");
				String code = sub[1].replaceAll("\\)", "");
				inventors.add(code);
			}
			
			for(int j=0;j<line.size();j++){
				String patent = line.get(j).trim();
				for(int k=0;k<patent.length();k++){
					if (Character.isDigit(patent.charAt(k))){
						String countryCode = patent.substring(0, k);
						countries.add(countryCode);
						countriesList.put(countryCode, countryCode);
						for(int p=0;p<inventors.size();p++){
							HashMap<String, Integer> temp = new HashMap<String, Integer>();
							temp.put(countryCode, 1);
							HashMap<String, Integer> result = inventorAndCountry.put(inventors.get(p), temp);
							if (result != null){
								Integer result2 = result.put(countryCode, 1);
								if (result2!=null){
									result.put(countryCode, (int)result2 + 1);
								}
								inventorAndCountry.put(inventors.get(p), result);
							}
						}
						break;
					}
				}
			}
		}
		
		inventorAndCountry = parser.createMapWithInventorNamesFromCodes3D(inventorAndCountry);
		
		//write first line to file
		FileHandle output = Gdx.files.local("output/inventorCountries.xls");
		output.writeString("\t", false);
		SortedSet<String> keysCountries = new TreeSet<String>(countriesList.keySet());
		for (String key : keysCountries) { 
			String code = countriesList.get(key);
			output.writeString(code+"\t", true);
		}output.writeString("\n", true);
		
		SortedSet<String> keys = new TreeSet<String>(inventorAndCountry.keySet());
		for (String key : keys) { 
			HashMap<String, Integer> innerMap = inventorAndCountry.get(key);
			output.writeString(key+"\t", true);
			System.out.println(key);
			SortedSet<String> keys2 = new TreeSet<String>(innerMap.keySet());
			
			HashMap<String, Integer> insertion = new HashMap<String, Integer>();
			for (String key2 : keys2) {
				Integer value = innerMap.get(key2);
				System.out.println("\t"+key2+"\t"+value);
				
				int nr=0;
				for (String key3 : keysCountries) { 
					String code = countriesList.get(key3);
					if (code.equals(key2)){
						insertion.put(key2, nr);
						break;
					}
					nr++;
				}
			}
			
			TreeMap<String, Integer> sortplacement = parser.sortHashMap(insertion);
			SortedSet<String> skeys = new TreeSet<String>(sortplacement.keySet());
			int last = 0;
			for (String skey : skeys) { 
				Integer value = insertion.get(skey);
				for(int x=0;x<value-last;x++){
					output.writeString("0\t", true);
				}
				last = value;
				output.writeString(String.valueOf(innerMap.get(skey)), true);
			}
			
			while (last < countriesList.size()){
				output.writeString("0\t", true);
				last++;
			}
			
			output.writeString("\n", true);
		}
	}

	@Override
	public void dispose() {
		
	}

	@Override
	public void render() {		
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		for(int i=0;i<quality-1;i++){
			mesh.scale(.5f*w, .5f*h, 1);
			mesh.render(GL10.GL_TRIANGLE_STRIP, i*quality*2, quality*2);
			mesh.scale(2f/w, 2f/h, 1);
		}
		
		shapes.setProjectionMatrix(projMatrix);
		shapes.begin(ShapeType.Filled);
		//draw dots
		Iterator it = dots.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            Vector2 pos = (Vector2)pairs.getKey();
            Color c = (Color)pairs.getValue();
            shapes.setColor(c);
            shapes.circle(pos.x*w*.5f, pos.y*h*.5f, 2);
        }
        
        //draw legend
        it = companyColors.entrySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            String code = (String)pairs.getKey();
            Color c = (Color)pairs.getValue();
            shapes.setColor(c);
            shapes.rect(10-w*.5f, 10-h*.5f + 15*count, 10, 10);
            count++;
        }
		shapes.end();
		shapes.begin(ShapeType.Line);
		
		//draw legend again but now outlined
		it = companyColors.entrySet().iterator();
        count = 0;
        shapes.setColor(Color.BLACK);
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            String code = (String)pairs.getKey();
            shapes.rect(10-w*.5f, 10-h*.5f + 15*count, 10, 10);
            count++;
        }
		shapes.end();
		
		batch.setProjectionMatrix(projMatrix);
		batch.begin();
		batch.enableBlending();
		fnt.setColor(0f, 0f, 0f, 1f);
		fnt.setScale(.6f);
		
		//draw sections
		it = words.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            Vector2 pos = (Vector2)pairs.getValue();
            String str = (String)pairs.getKey();
            TextBounds b = fnt.getBounds(str);
            fnt.draw(batch, str, pos.x*w*.5f - b.width*.5f, pos.y*h*.5f+b.height*.5f +10);
            //it.remove();
        }
        
        //draw company names
        it = companyColors.entrySet().iterator();
        count = 0;
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            String code = (String)pairs.getKey();
            fnt.draw(batch, code, 25-w*.5f, 19-h*.5f+15*count);
            count++;
        }
		
		batch.disableBlending();
		batch.end();
		
		
	}

	@Override
	public void resize(int width, int height) {
		w=width;
		h=height;
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
