package edu.arizona.cs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

// Class that builds index from resources
public class IndexProcessor {
	private int analyzerMode = 0;
	private int lemmaMode = 0;
	
	public IndexProcessor(int analyzerMode, int lemmaMode) {
		this.analyzerMode = analyzerMode;
		System.out.println(this.analyzerMode);
		this.lemmaMode = lemmaMode;
	}
	public void mutiple_file_indexBuilder()  throws java.io.FileNotFoundException,java.io.IOException {
		
    	File dir = new File("./src/main/resources");

    	File[] foundFiles = dir.listFiles(new FilenameFilter() {
    	    public boolean accept(File dir, String name) {
    	        return name.startsWith("enwiki-20140602-pages-articles.xml-")
    	        		&&name.endsWith(".txt");	    
			}
    	});
    	
    	// clean index
        Directory index = FSDirectory.open(Paths.get("./index"));
        IndexWriter w = new IndexWriter(index, new IndexWriterConfig(new StandardAnalyzer()));
        w.deleteAll();
        w.close();
        
    	for (File file : foundFiles) {
    	   	indexBuilder(file);
    	}   
		
		
	}
    public void indexBuilder(File file)  throws java.io.FileNotFoundException,java.io.IOException {
    	
        StringBuilder result = new StringBuilder("");

        try (Scanner scanner = new Scanner(file)) {

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result.append(line);
            }

            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        
   // INDEX  
   // put
       

            
        WhitespaceAnalyzer whitespaceAnalyzer = new WhitespaceAnalyzer();
        EnglishAnalyzer englishAnalyzer = new EnglishAnalyzer();
        StandardAnalyzer standardAnalyzer = new StandardAnalyzer();
     //   ShingleAnalyzerWrapper shingleAnalyzerWrapper = new ShingleAnalyzerWrapper(standardAnalyzer,2,2);
        
        IndexWriterConfig config;
        if(this.analyzerMode == 0)
        	config = new IndexWriterConfig(whitespaceAnalyzer);
        else if(this.analyzerMode == 1)
        	config = new IndexWriterConfig(englishAnalyzer);
        else
        	config = new IndexWriterConfig(standardAnalyzer);

        Directory index = FSDirectory.open(Paths.get("./index"));
        
        IndexWriter w = new IndexWriter(index, config);        


        
  // PARSE EACH PAGE
        
        String test=result.toString();
        
        //remove anything between [tpl] and [\tpl]
        test = test.replaceAll("(?s)\\[tpl\\].*?\\[/tpl\\]", "");
        // remove "==" "==="
        test= test.replaceAll("=={1,}", " ");
              
        // split wikipges by "[[", store each wikipage in array.
        String[] wikipages = test.split("\\[\\[");
        
        List<String[]> wikipageList = new LinkedList<String[]>();
        
        // split each wikipage into title, content by split first ]]
        for(String wikipage: wikipages) {
        	
        	String[] parts = wikipage.split("\\]\\]",2);
        	// in case this document is blank
        	if(parts.length!=2) 
        		continue;
        	wikipageList.add(parts);
        }
        
        
        // get rid of File: image: and put relative content into previous page
        String[] prev = null;
        
        for (String[] parts: wikipageList) {
        	if(parts[0].startsWith("File:") || parts[0].startsWith("Image:")) {

        		parts[0] = "";
        		prev[1] = prev[1] + " " + parts[1];
        		parts[1] = "";
            }
            prev = parts;
        }
        
        //

        

        
        for(String[] parts: wikipageList) {
        	// skip blank [] created in last step
        	if(parts[0].length()==0) 
        		continue;
        	
    		StanfordLemmatizer slem = new StanfordLemmatizer();
    		String s = parts[1];
    		
//    		s = s.replaceAll("[^A-Za-z0-9 ]" ,"");

        	if(lemmaMode == 1)
        		 s = slem.lemmatize(parts[1]).toString() ;
        	
        	
    		addDoc(w, s, parts[0]);
    		
//         	System.out.println( parts[0]);
//        	System.out.println( parts[1]);   
//        	System.out.println("\n\n\n"); 
        	
        }
    

        w.close();
    }
    

    
    
    private static void addDoc(IndexWriter w, String content, String title) throws IOException {
  	  Document doc = new Document();
  	  doc.add(new TextField("content", content, Field.Store.YES));
  	  doc.add(new StringField("title", title, Field.Store.YES));
  	  w.addDocument(doc);
  	}
}
