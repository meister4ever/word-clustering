import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.apache.hadoop.conf.Configuration;

public class EntityTopicMap {
	private final Double THRESHOLD = 0.25;
	private HashMap<String, HashMap<String, Double>> entityTopics = new HashMap<String, HashMap<String, Double>>();
	
	public EntityTopicMap(String filename, Configuration conf) {
		String fileContent = HdfsFileUtil.ReadFileContent(filename, conf);
		String[] lines = fileContent.split("\n");
		for (String line : lines) {
			String[] parts = line.split("\t");
			String entityName = StringUtil.clean(parts[0]);
			String topicStr = StringUtil.clean(parts[1]);
			String[] topicScores = topicStr.split(",");
			HashMap<String, Double> topicScoreMap = entityTopics.get(entityName);
			if (topicScoreMap == null) {
				topicScoreMap = new HashMap<String, Double>();
				entityTopics.put(entityName, topicScoreMap);
			}
			for (String topicScore : topicScores) {
				String[] topicScoreParts = topicScore.split(":");
				String topic = StringUtil.clean(topicScoreParts[0]);
				Double score = Double.parseDouble(StringUtil.clean(topicScoreParts[1]));
				topicScoreMap.put(topic, score);
			}
		}
	}
	
	public HashMap<String, Double> getEntityTopicMap(String entityName) {
		return entityTopics.get(entityName);
	}
	
	private double getDistance(String entity1, String entity2) {
		HashMap<String, Double> entityTopics1 = entityTopics.get(entity1);
		HashMap<String, Double> entityTopics2 = entityTopics.get(entity2);
		
		if (entityTopics1 == null || entityTopics2 == null) {
			return 0.0;
		}
		
		double ret = 0.0;
		double modulusA = 0.0, modulusB = 0.0;
		for (String topic : entityTopics1.keySet()) {
			double entity1topicScore = entityTopics1.get(topic);
			double entity2topicScore = entityTopics2.get(topic);
			ret += (entity1topicScore * entity2topicScore);
			modulusA += (entity1topicScore*entity1topicScore);
			modulusB += (entity2topicScore*entity2topicScore);
		}
		modulusA = Math.sqrt(modulusA);
		modulusB = Math.sqrt(modulusB);
		
		ret = ret / (modulusA*modulusB);
		return ret;
	}
	
	public TreeSet<EntityScore> getKSimEntities(String entityName) {
		TreeSet<EntityScore> ret = new TreeSet<EntityScore>();
		for (String entity : entityTopics.keySet()) {
			if (entity.compareTo(entityName) == 0) {
				continue;
			}
			Double distance = getDistance(entityName, entity);
			ret.add(new EntityScore(entity, distance));
			if (ret.size() > 10) {
				ret.remove(ret.first());
			}
		}
		return ret;
	}
}

class EntityScore implements Comparable<EntityScore> {
	
	public String entity;
	public Double score;
	
	public EntityScore(String entity, Double score) {
		this.entity = entity;
		this.score = score;
	}
	
	@Override
	public int compareTo(EntityScore other) {
		if (this.score < other.score) {
			return -1;
		} else if (this.score > other.score) {
			return 1;
		} else {
			this.entity.compareTo(other.entity);
		}
		return 0;
	}
	
	public String toString() {
		return this.entity + ":" + this.score;
	}
}

