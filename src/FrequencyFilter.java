import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.TreeSet;


public class FrequencyFilter {
  @SuppressWarnings("rawtypes")
  private static class WordFreq implements Comparable {
    public String word;
    public Integer freq;
    
    public WordFreq(String word, Integer freq) {
      super();
      this.word = word;
      this.freq = freq;
    }

    @Override
    public int compareTo(Object other) {
      WordFreq otherWordFreq = (WordFreq)other;
      if (this.freq < otherWordFreq.freq) {
        return -1;
      } else if (this.freq > otherWordFreq.freq) {
        return 1;
      } else {
        return this.word.compareTo(otherWordFreq.word);
      }
    }
    
  }
  
  private TreeSet<WordFreq> wordFreqSet = new TreeSet<FrequencyFilter.WordFreq>();
  
  public void addWord(String word, Integer freq) {
    wordFreqSet.add(new WordFreq(word, freq));
    if (wordFreqSet.size() > 10000) {
      wordFreqSet.remove(wordFreqSet.first());
    }
  }
  
  public void dumpWords() {
    for (WordFreq wf : wordFreqSet) {
      System.out.println(wf.word.toLowerCase() + "\t" + wf.freq);
    }
  }
  
  public static void main(String[] args) {
      File file = new File(args[0]);
      FileInputStream fis = null;
      BufferedInputStream bis = null;
      DataInputStream dis = null;
      FrequencyFilter ffilter = new FrequencyFilter();

      try {
        fis = new FileInputStream(file);
        bis = new BufferedInputStream(fis);
        dis = new DataInputStream(bis);

        String line = null;
        while ((line = dis.readLine()) != null) {
          String[] parts = line.split("\t");
          ffilter.addWord(parts[0], Integer.parseInt(parts[1]));
        }
        
        fis.close();
        bis.close();
        dis.close();

      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      
      ffilter.dumpWords();
  }
}
