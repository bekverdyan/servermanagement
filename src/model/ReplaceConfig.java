package model;

import java.util.List;

public class ReplaceConfig {
	public String file;

	public List<KeyValuePair> keyWords;

	@Override
	public String toString() {
		return "ReplaceConfig [file=" + file + ", keyWords=" + keyWords + "]";
	}
	
	
}
