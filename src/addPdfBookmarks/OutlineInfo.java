package addPdfBookmarks;

import java.util.HashMap;
import java.util.List;

public class OutlineInfo {
	HashMap<String, Object> preOutline=null;
	List<HashMap<String,Object>> outlines=null;
	int preLevel=0;

	public OutlineInfo(HashMap<String, Object> preOutline,
			List<HashMap<String, Object>> outlines, int preLevel) {
		this.outlines=outlines;
		this.preLevel=preLevel;
		this.preOutline=preOutline;
	}

	public List<HashMap<String, Object>> getOutlines() {
		return outlines;
	}

	public HashMap<String, Object> getPreOutline() {
		return preOutline;
	}

	public int getPreLevel() {
		return preLevel;
	}

}
