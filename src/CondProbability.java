import java.lang.Math;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Iterator;
import java.io.ObjectInputStream;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.RawComparator;
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
public class CondProbability {
  private static class CondProbabilityMapper extends MapReduceBase implements
    Mapper<LongWritable, Text, Text, Text> {

      @Override
        public void configure(JobConf job) {
        }

      @Override
        public void map(LongWritable key, Text value,
            OutputCollector<Text, Text> output, Reporter reporter)
        throws IOException {
          String[] parts = value.toString().split("\t");
          Integer count = Integer.parseInt(parts[1]);
          String[] keyParts = parts[0].split("");
          if (keyParts.length > 1) {
            output.collect(new Text(keyParts[0] + "b"), new Text(parts[0] + "" + count));
            output.collect(new Text(keyParts[1] + "b"), new Text(parts[0] + "" + count));
          } else {
            output.collect(new Text(parts[0] + "a"), new Text(Integer.toString(count)));
          }
        }
    }

  public static class CondProbabilityReducer extends MapReduceBase implements
    Reducer<Text, Text, Text, Text> {

      @Override
        public void configure(JobConf job) {
        }

      @Override
        public void reduce(Text key, Iterator<Text> values,
            OutputCollector<Text, Text> output, Reporter reporter)
        throws IOException {
          boolean firstKey = true;
          Integer countA = 0;
          System.out.println(key.toString());
          while (values.hasNext()) {
            String valueStr = values.next().toString();
            if (firstKey) {
              firstKey = false;
              countA = Integer.parseInt(valueStr);
              continue;
            }
            String[] keySplits = key.toString().split("");
            String[] valueSplits = valueStr.split(""); 
            Integer cooccurence = Integer.parseInt(valueSplits[1]);
            Double prob = cooccurence.doubleValue() / countA.doubleValue();
            output.collect(new Text(valueSplits[0]), new Text(prob + "" + cooccurence));
          }
        }
    }

  private static class CondProbabilityPartitioner implements Partitioner<Text, Text>,
          JobConfigurable {
    @Override
      public int getPartition(Text key, Text value, int numReduceTasks) {
        String compareString = key.toString();
        String compareSubString = compareString.substring(0, compareString.length() - 2);
        int sum = 0;
        for (int i = 0; i < compareSubString.length(); ++i) {
          sum += Character.getNumericValue(compareSubString.charAt(i));
        }
        return sum % numReduceTasks;
      }

    @Override
      public void configure(JobConf conf) {
      }
  }


  private static final class SortReducerByValuesValueGroupingComparator
    implements RawComparator<Text> {
      public int compare(byte[] text1, int start1, int length1, byte[] text2,
          int start2, int length2) {
        Text textObj1 = null, textObj2 = null;
        try {
          ObjectInputStream in = new ObjectInputStream(
              new ByteArrayInputStream(text1, start1, length1));
          textObj1 = (Text) in.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }

        try {
          ObjectInputStream in = new ObjectInputStream(
              new ByteArrayInputStream(text2, start2, length2));
          textObj2 = (Text) in.readObject();
        } catch (Exception e) {
          e.printStackTrace();
        }

        String text1Str = textObj1.toString();
        String text2Str = textObj2.toString();

        String cmpStr1 = text1Str.substring(0, text1Str.length() - 2);
        String cmpStr2 = text2Str.substring(0, text2Str.length() - 2);

        return cmpStr1.compareTo(cmpStr2);
      }

      @Override
        public int compare(Text textObj1, Text textObj2) {
          String text1Str = textObj1.toString();
          String text2Str = textObj2.toString();

          String cmpStr1 = text1Str.substring(0, text1Str.length() - 2);
          String cmpStr2 = text2Str.substring(0, text2Str.length() - 2);

          return cmpStr1.compareTo(cmpStr2);
        }
    }

  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.err.println("Usage: hadoop jar Cooccur.jar CondProbability"
          + " <in-files> <out-dir> <num-reducers>"); 
      return;
    }
    String inputPath = args[0];
    String outputPath = args[1];

    JobConf conf = new JobConf(CondProbability.class);

    conf.setJobName("CondProbability");
    conf.setNumReduceTasks(Integer.parseInt(args[2]));

    conf.setMapperClass(CondProbabilityMapper.class);
    conf.setReducerClass(CondProbabilityReducer.class);
    conf.setPartitionerClass(CondProbabilityPartitioner.class);
    conf.setMapOutputKeyClass(Text.class);
    conf.setMapOutputValueClass(Text.class);
    conf.setOutputKeyClass(Text.class);
    conf.setOutputValueClass(Text.class);
    conf.setOutputValueGroupingComparator(SortReducerByValuesValueGroupingComparator.class);

    FileInputFormat.setInputPaths(conf, new Path(inputPath));
    FileOutputFormat.setOutputPath(conf, new Path(outputPath));

    long startTime = System.currentTimeMillis();
    JobClient.runJob(conf);
    System.out.println("Job Finished in "
        + (System.currentTimeMillis() - startTime) / 1000.0
        + " seconds");
  }
}
