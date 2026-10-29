package cce.ruleupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.yaml.snakeyaml.Yaml;

public class Load_YML_Rules {
	
	// Load the rule files (YAML) to generate java code
	public static ArrayList<SigmaMapping> loadRules(){
		ArrayList<SigmaMapping> sigmaMappingArray = new ArrayList<SigmaMapping>();
		Yaml yaml = new Yaml();
		String projectPath = System.getProperty("user.dir");
		
		String templateCodeLocation = projectPath + "/rule_template_code/"; //rule code destination
		File templateCodePath = new File(templateCodeLocation);
		if (!templateCodePath.exists()) {
			templateCodePath.mkdir();
		}
		File rulesFolder = new File("rules_yml/"); // YML rule files
		if (!rulesFolder.exists()) {
			rulesFolder.mkdir();
		}
		File[] yml_rules = rulesFolder.listFiles();
		if (yml_rules.length > 0) {
			for (int j = 0; j < yml_rules.length; j++) {
				System.out.println(yml_rules[j].toString());
				try {
					InputStream inputStream = new FileInputStream(yml_rules[j]);
					Reader reader = new InputStreamReader(inputStream);
					LinkedHashMap<String, Object> yamlRuleObj = yaml.load(reader);
					sigmaMappingArray.add(new SigmaMapping(yamlRuleObj));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
		return sigmaMappingArray;
	}
	
	public static ArrayList<String> loadRulesYml(){
		ArrayList<String> rulesArray = new ArrayList<String>();
		String projectPath = System.getProperty("user.dir");
		String templateCodeLocation = projectPath + "/rule_template_code/";
		File templateCodePath = new File(templateCodeLocation);
		
		if (!templateCodePath.exists()) {
			templateCodePath.mkdir();
		}
		File rulesFolder = new File("rules_yml/");
		
		if (!rulesFolder.exists()) {
			rulesFolder.mkdir();
		}
		File[] yml_rules = rulesFolder.listFiles();
		
		if (yml_rules.length > 0) {
			
		}
		for (int j = 0; j < yml_rules.length; j++) {
		    try {
		        String yamlContent = new String(Files.readAllBytes(yml_rules[j].toPath()));
		        rulesArray.add(yamlContent);
		    } catch (Exception e) {
		    	e.printStackTrace();
		    }
		}
		return rulesArray;
	}
}

