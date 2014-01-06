package org.tue.students.patents.landscape;

import com.badlogic.gdx.math.Vector2;

public class PatentKeyword {
	
	public PatentKeyword(String name, Vector2 position, float value){
		this.position = position;
		this.name = name;
		this.value = value;
	}
	
	public Vector2 position;
	public float value;
	public String name;
}
