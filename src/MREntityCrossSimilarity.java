import java.io.IOException;
import java.util.Iterator;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Partitioner;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
//import org.apache.hadoop.mapred.TextInputFormat;
//import org.apache.hadoop.mapred.TextOutputFormat;

@SuppressWarnings("deprecation")
public class MREntityCrossSimilarity {
	private static class MyMapper extends MapReduceBase implements
			Mapper<Text, Text, Text, Text> {
		// We should know total records to do the cross similarity.
		int totalRecords = 0;

		@Override
		public void configure(JobConf job) {
			totalRecords = job.getInt("totalRecords", 0);
		}

		@Override
		public void map(Text key, Text value,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			String[] parts = value.toString().split("\t");
			Integer recordNumber = Integer.parseInt(parts[0]);
			String newKey = String.format("%010d", recordNumber);
			output.collect(new Text(newKey), new Text("0\t" + value.toString()));
			for (int i = 0; i < totalRecords; ++i) {
				newKey = String.format("%010d", i);
				output.collect(new Text(newKey), new Text("1\t" + value.toString()));
			}
		}
	}
	
	private static class MyPartitioner implements Partitioner<Text, Text>, JobConfigurable {

		@Override
		public int getPartition(Text key, Text value, int numReduceTasks) {
			Integer partition = Integer.parseInt(key.toString()) % numReduceTasks;
			return partition;
		}

		@Override
		public void configure(JobConf conf) {		
		}
	}
	
	private static class MyReducer extends MapReduceBase implements
		Reducer<Text, Text, Text, Text> {

		@Override
		public void reduce(Text key, Iterator<Text> values,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			// TODO Auto-generated method stub
			String firstValue = "";
		    while (values.hasNext()) {
		    	String currentValue = values.next().toString().split("\t")[1];
		    	if (firstValue == "") {
		    		firstValue = currentValue;
		    	}
		    	if (currentValue != firstValue) {
		    		output.collect(new Text(firstValue + " X " + currentValue), new Text(""));
		    	}
		    }
		}
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException {

		String inputPath = args[0];
		String outputPath = args[1];
		Integer totalRecords = Integer.parseInt(args[2]);

		//int reduceTasks = Integer.parseInt(args[2]);

		JobConf conf = new JobConf(MRWordTopicConverter.class);

		conf.setJobName("MRWordTopicConverter");
		conf.setNumReduceTasks(2);
		conf.setInt("totalRecords", totalRecords);

		conf.set("mapred.task.timeout", "12000000");
		//conf.set("mapred.child.java.opts", "-Xmx4000M -Xms2000M");

		//conf.setInputFormat(TextInputFormat.class);
		//conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapperClass(MyMapper.class);
		conf.setPartitionerClass(MyPartitioner.class);
		conf.setReducerClass(MyReducer.class);
		
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);

		FileInputFormat.setInputPaths(conf, new Path(inputPath));
		FileOutputFormat.setOutputPath(conf, new Path(outputPath));

		long startTime = System.currentTimeMillis();
		JobClient.runJob(conf);
		System.out.println("Job Finished in "
				+ (System.currentTimeMillis() - startTime) / 1000.0
				+ " seconds");
	}
}