import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;


public class TopicStatUtil {
	public static class Stats {
		public double mean;
		public double sdev;
		
		Stats(double mean, double sdev) {
			this.mean = mean;
			this.sdev = sdev;
		}
	}
	
	private HashMap<String, Stats> topicStatMap = new HashMap<String, TopicStatUtil.Stats>();
	
	public TopicStatUtil(String filename, Configuration conf) {
		String fileContent = HdfsFileUtil.ReadFileContent(filename, conf);
		String[] lines = fileContent.split("\n");
		for (String line : lines) {
			String[] parts  = line.split("\t");
			String topic = StringUtil.clean(parts[0]);
			String[] statStrs = parts[1].split(",");
			Double mean = Double.parseDouble(StringUtil.clean(statStrs[0]));
			Double sdev = Double.parseDouble(StringUtil.clean(statStrs[1]));
			topicStatMap.put(topic, new Stats(mean, sdev));
		}
	}
	
	public Double getMean(String topic) {
		return topicStatMap.get(topic).mean;
	}
	
	public Double getSdev(String topic) {
		return topicStatMap.get(topic).sdev;
	}
}