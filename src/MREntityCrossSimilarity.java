import java.lang.Comparable;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.WritableComparable;
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

@SuppressWarnings("deprecation")
public class MREntityCrossSimilarity {

	private static class DotProductData {
		public String firstValue;
		public String secondValue;
		public Double dotProduct;
		public Set<CategoryProduct> orderedCategories;
	}

	private static class CategoryProduct implements Comparable<CategoryProduct> {
		public String category;
		public Double weight;

		CategoryProduct(String category, Double weight) {
			this.category = category;
			this.weight = weight;
		}
		
		@Override
		public String toString() {
			return category + ":" + weight;
		}
		
		// CompareTo
		@Override
		public int compareTo(CategoryProduct other) {
			if (weight > other.weight) {
				return 1;
			} else if (weight < other.weight) {
				return -1;
			} else {
				return category.compareTo(other.category);
			}
		}
	}

	public static DotProductData ComputeDotProduct(String first, String second) {
		Set<CategoryProduct> categorySet = new TreeSet<CategoryProduct>();
		System.out.println(first + " : " + second);
		DotProductData dotProduct = new DotProductData();
		String[] parts = first.split("\t");
		dotProduct.firstValue = parts[0];
		String[] firstParts = parts[1].split(",");

		parts = second.split("\t");
		dotProduct.secondValue = parts[0];
		String[] secondParts = parts[1].split(",");
		dotProduct.dotProduct = 0.0;

		HashMap<String, Double> categoryScores = new HashMap<String, Double>();
		for (int i = 0; i < firstParts.length; ++i) {
			parts = firstParts[i].split(":");
			categoryScores.put(parts[0], Double.parseDouble(parts[1]));
		}
		for (int i = 0; i < secondParts.length; ++i) {
			parts = secondParts[i].split(":");
			Double prevValue = categoryScores.get(parts[0]);
			if (prevValue != null) {
				Double product = prevValue * Double.parseDouble(parts[1]);
				categorySet.add(new CategoryProduct(parts[0], product));
				dotProduct.dotProduct += product;
			}
		}
		dotProduct.orderedCategories = categorySet;
		return dotProduct;
	}

	private static class MyText implements WritableComparable<MyText> {
		private String key1;
		private String key2;

		MyText() {
			this.key1 = null;
			this.key2 = null;
		}

		MyText(String key1, String key2) {
			this.key1 = key1;
			this.key2 = key2;
		}

		@Override
		public void readFields(DataInput in) throws IOException {
			int strLen = in.readInt();
			String inStr = "";
			for (int i = 0; i < strLen; ++i) {
				inStr += in.readChar();
			}
			String[] parts = inStr.split("\t");
			key1 = parts[0];
			key2 = parts[1];
		}

		@Override
		public void write(DataOutput out) throws IOException {
			String outStr = key1 + "\t" + key2;
			out.writeInt(outStr.length());
			out.writeChars(key1 + "\t" + key2);
		}

		@Override
		public int compareTo(MyText rhs) {
			int compareRes = this.key1.compareTo(rhs.key1);
			if (compareRes == 0) {
				return this.key2.compareTo(rhs.key2);
			}
			return compareRes;
		}

		public String getKey1() {
			return key1;
		}
	}

	private static class MyMapper extends MapReduceBase implements
			Mapper<LongWritable, Text, MyText, Text> {
		// We should know total records to do the cross similarity.
		int totalRecords = 0;

		@Override
		public void configure(JobConf job) {
			totalRecords = job.getInt("totalRecords", 0);
		}

		@Override
		public void map(LongWritable key, Text value,
				OutputCollector<MyText, Text> output, Reporter reporter)
				throws IOException {
			String[] parts = value.toString().split("");
			Integer recordNumber = Integer.parseInt(parts[0]);
			String newKey = String.format("%010d", recordNumber);
			output.collect(new MyText(newKey, "0"), new Text(parts[1]));
			for (int i = 1; i <= totalRecords; ++i) {
				newKey = String.format("%010d", i);
				output.collect(new MyText(newKey, "1"), new Text(parts[1]));
			}
		}
	}

	private static class MyPartitioner implements Partitioner<MyText, Text>,
			JobConfigurable {

		@Override
		public int getPartition(MyText key, Text value, int numReduceTasks) {
			Integer partition = Integer.parseInt(key.getKey1().toString())
					% numReduceTasks;
			return partition;
		}

		@Override
		public void configure(JobConf conf) {
		}
	}

	private static class MyReducer extends MapReduceBase implements
			Reducer<MyText, Text, Text, Text> {

		@Override
		public void reduce(MyText key, Iterator<Text> values,
				OutputCollector<Text, Text> output, Reporter reporter)
				throws IOException {
			String firstValue = "";
			while (values.hasNext()) {
				String currentValue = values.next().toString();
				if (firstValue == "") {
					firstValue = currentValue;
				}
				if (currentValue.compareTo(firstValue) != 0) {
					DotProductData data = ComputeDotProduct(firstValue,
							currentValue);
					String categoryString = "";
					boolean first = true;
					for (CategoryProduct cat : data.orderedCategories) {
						if (!first) {
							categoryString += "," + cat;
							first = false;
						} else {
							categoryString += cat;
						}
					}
					output.collect(new Text(data.firstValue + ", "
							+ data.secondValue), new Text(data.dotProduct
							.toString() + "\t" + categoryString));
				}
			}
		}
	}

	private static final class SortReducerByValuesValueGroupingComparator
			implements RawComparator<MyText> {
		public int compare(byte[] text1, int start1, int length1, byte[] text2,
				int start2, int length2) {
			// look at first character of each text byte array
			// Deserialize the binary stream and get the MyText object from
			// them.
			// Then add the comparison to group by the primary key (it is
			// already sorted
			// on primary and secondary).
			MyText myText1 = null, myText2 = null;
			try {
				ObjectInputStream in = new ObjectInputStream(
						new ByteArrayInputStream(text1, start1, length1));
				myText1 = (MyText) in.readObject();
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				ObjectInputStream in = new ObjectInputStream(
						new ByteArrayInputStream(text2, start2, length2));
				myText2 = (MyText) in.readObject();
			} catch (Exception e) {
				e.printStackTrace();
			}

			return myText1.key1.compareTo(myText2.key1);
		}

		@Override
		public int compare(MyText o1, MyText o2) {
			int compareRes = o1.key1.compareTo(o2.key1);
			/*
			 * if (compareRes == 0) { return o1.key2.compareTo(o2.key2); }
			 */
			return compareRes;
		}
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException {

		String inputPath = args[0];
		String outputPath = args[1];
		Integer totalRecords = Integer.parseInt(args[2]);
		Integer numReducers = Integer.parseInt(args[3]);

		// int reduceTasks = Integer.parseInt(args[2]);

		JobConf conf = new JobConf(MRWordTopicConverter.class);

		conf.setJobName("MREntityCrossSimilarity");
		conf.setNumReduceTasks(numReducers);
		conf.setInt("totalRecords", totalRecords);

		conf.set("mapred.task.timeout", "12000000");
		// conf.set("mapred.child.java.opts", "-Xmx4000M -Xms2000M");

		// conf.setInputFormat(TextInputFormat.class);
		// conf.setOutputFormat(TextOutputFormat.class);

		conf.setMapperClass(MyMapper.class);
		conf.setPartitionerClass(MyPartitioner.class);
		conf.setReducerClass(MyReducer.class);
		conf
				.setOutputValueGroupingComparator(SortReducerByValuesValueGroupingComparator.class);

		conf.setMapOutputKeyClass(MyText.class);
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
