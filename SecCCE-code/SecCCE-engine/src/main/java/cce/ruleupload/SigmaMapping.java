package cce.ruleupload;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import org.json.simple.JSONObject;


public class SigmaMapping {
	private String title;
	private String id;
	private LinkedHashMap<String, String> related;
	private String license;
	private String status;
	private String description;
	private String author;
	private ArrayList<String> references;
	private LinkedHashMap<String, String> logsource;
	private LinkedHashMap<String, LinkedHashMap<String, Object>> detection;
	private String condition;
	private ArrayList<String> fields;
	private ArrayList<String> falsepositives;
	private String level;
	private ArrayList<String> tags;
	private String match; 
	private String timeframe;
	private String event;
	
	public SigmaMapping() {}
	
	public SigmaMapping(LinkedHashMap<String, Object> yamlRuleObj) {
		setTitle(String.valueOf(yamlRuleObj.get("title")));
		setAuthor(String.valueOf(yamlRuleObj.get("author")));
		setDescription(String.valueOf(yamlRuleObj.get("description")));
		setId(String.valueOf(yamlRuleObj.get("id")));
		setLevel(String.valueOf(yamlRuleObj.get("level")));
		setStatus(String.valueOf(yamlRuleObj.get("status")));
		setLicense(String.valueOf(yamlRuleObj.get("license")));
		setMatch(String.valueOf(yamlRuleObj.get("match")));
		setTimeframe(String.valueOf(yamlRuleObj.get("timeframe")));
		
		setTags((ArrayList<String>) yamlRuleObj.get("tags"));
		setReferences((ArrayList<String>) yamlRuleObj.get("references"));
		setFalsepositives((ArrayList<String>) yamlRuleObj.get("falsepositives"));
		setFields((ArrayList<String>) yamlRuleObj.get("fields"));
		
		setRelated((LinkedHashMap<String, String>) yamlRuleObj.get("related"));
		setLogsource((LinkedHashMap<String, String>) yamlRuleObj.get("logsource"));
		setDetection((LinkedHashMap<String, LinkedHashMap<String, Object>>) yamlRuleObj.get("detection"));
		setCondition(String.valueOf(getDetection().get("condition")));
		getDetection().remove("condition");
	}
	
	public String toCSVFull() {
		String obj = new String();
		obj.concat("title: " + getTitle());
		obj.concat(", id: " + getId());
		obj.concat(", related: " + getRelated());
		obj.concat(", license: " + getLicense());
		obj.concat(", status: " + getStatus());
		obj.concat(", description: " + getDescription());
		obj.concat(", author: " + getAuthor());
		obj.concat(", references: " + getReferences());
		obj.concat(", logsource: " + getLogsource());
		obj.concat(", fields: " + getFields());
		obj.concat(", falsepositives: " + getFalsepositives());
		obj.concat(", level: " + getLevel());
		obj.concat(", tags: " + getTags());
		obj.concat(", match: " + getMatch());
		obj.concat(", timeframe: " + getTimeframe());
		System.out.println("toCSVFull: " + obj);
		return obj;
	}
	
	public String toJsonFull() {
		JSONObject obj = new JSONObject();
		obj.put("title", getTitle());
		obj.put("id", getId());
		obj.put("related", getRelated());
		obj.put("license", getLicense());
		obj.put("status", getStatus());
		obj.put("description", getDescription());
		obj.put("author", getAuthor());
		obj.put("references", getReferences());
		obj.put("logsource", getLogsource());
		obj.put("fields", getFields());
		obj.put("falsepositives", getFalsepositives());
		obj.put("level", getLevel());
		obj.put("tags", getTags());
		obj.put("match", getMatch());
		obj.put("timeframe", getTimeframe());
		
		return obj.toString();
	}
	
	public String toJson() {
		JSONObject obj = new JSONObject();
		obj.put("title", getTitle());
		//obj.put("level", getLevel());
		//obj.put("id", getId());
		
		return obj.toString();
	}
	
	public String getMatch() {
		return match;
	}
	public void setMatch(String match) {
		this.match = match;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public LinkedHashMap<String, String> getRelated() {
		return related;
	}
	public void setRelated(LinkedHashMap<String, String> related) {
		this.related = related;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public ArrayList<String> getReferences() {
		return references;
	}
	public void setReferences(ArrayList<String> references) {
		this.references = references;
	}
	public LinkedHashMap<String, String> getLogsource() {
		return logsource;
	}
	public void setLogsource(LinkedHashMap<String, String> logsource) {
		this.logsource = logsource;
	}
	public LinkedHashMap<String, LinkedHashMap<String, Object>> getDetection() {
		return detection;
	}
	public void setDetection(LinkedHashMap<String, LinkedHashMap<String, Object>> detection) {
		this.detection = detection;
	}
	public ArrayList<String> getFields() {
		return fields;
	}
	public void setFields(ArrayList<String> fields) {
		this.fields = fields;
	}
	public ArrayList<String> getFalsepositives() {
		return falsepositives;
	}
	public void setFalsepositives(ArrayList<String> falsepositives) {
		this.falsepositives = falsepositives;
	}
	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}
	public ArrayList<String> getTags() {
		return tags;
	}
	public void setTags(ArrayList<String> tags) {
		this.tags = tags;
	}
	public String getLicense() {
		return license;
	}
	public void setLicense(String license) {
		this.license = license;
	}
	public String getTimeframe() {
		return timeframe;
	}
	public void setTimeframe(String timeframe) {
		this.timeframe = timeframe;
	}
	public String getEvent() {
		return event;
	}
	public void setEvent(String event) {
		this.event = event;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
}