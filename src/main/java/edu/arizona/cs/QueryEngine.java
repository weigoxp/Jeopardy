package edu.arizona.cs;


import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
import java.util.Scanner;


public class QueryEngine {
    String questionfile = "questions.txt";
    String indexDir = "./index";
 /* *********************************************************************************************************************/
    public static void main(String[] args ) {
    	
    	int para =Integer.parseInt(args[0]);
        try {
        	
            System.out.println("********Welcome to  project 1!");
            QueryEngine objQueryEngine = new QueryEngine();
            Directory index = FSDirectory.open(Paths.get("./index"));
            
            // mode 0: whitespace
            // mode 1: english
            // mode 2: standard
            IndexProcessor indexProcessor = new IndexProcessor(1,1);
            if(para == 0)
            	indexProcessor.mutiple_file_indexBuilder();
            
            List<String[]> questionList= objQueryEngine.questionReader(); 
            
            
            float matched = 0;
            float intop10 =0;
            double count =0;
            
            // loop all questions and run lucene searcher
            for(String[] singlequestion :questionList) {
            	String question = singlequestion[1] + " " +singlequestion[0].replaceAll("\\(.*?\\)", "");
            	String answer = singlequestion[2];
            	
                StanfordLemmatizer slem = new StanfordLemmatizer();
            	           	
            	
                String lemmaquestion = slem.lemmatize(question).toString() ;
            	String tokenedquestion = objQueryEngine.questionTokenizer(lemmaquestion);
                
            	String finalquery = tokenedquestion;
            	

     //       	System.out.println(finalquery);
 //           	String[] fi = finalquery.split(" ");
//            	String ff = "";
//            	for(int i =0; i<fi.length-1;i++) {
//            		String tmp = "\""+fi[i]+" +"+fi[i+1]+"\" ";
//            		ff+= tmp;
//            	}
            	List<ResultClass> top10ofwikis = 
            			objQueryEngine.actualSearcher(finalquery, index);
            	

            	// amazing happens, rerank top10
            	
            	for(ResultClass eachhit:top10ofwikis) {
            		String tmp = eachhit.DocName.get("title");
            		String[] answerarray = objQueryEngine.questionTokenizer( slem.lemmatize(tmp).toString()).split(" ");
            		
            		for(String term : answerarray) {
            			if(finalquery.contains(term))
            				eachhit.doc_score *=0.75;
            		}
            	}
            	
            	// resort the reranked result
            	Collections.sort(top10ofwikis, new Comparator<ResultClass>(){

            		  public int compare(ResultClass r1, ResultClass r2)
            		  {
            		     return Double.compare(r2.doc_score, r1.doc_score); 
            		  }
            		});
            	
            	
            	
            	
            	boolean flag = false;
            	int rank =9999;
            	// loop over top 10 hits
            	for(int i =0; i <top10ofwikis.size();i++) {
            		ResultClass result = top10ofwikis.get(i);
            		// answer found in top10 hits
            		if(answer.toLowerCase().contains(result.DocName.get("title").toLowerCase())) {
            			rank = i;
            			intop10 ++;  
            			flag = true;
            			
            			// answer not in top 1. 
            			if(i!=0) {
            				count+=i;
            					
            			}
            				
            		}
            			
            			
            	}
            	System.out.println("should be:  "+answer +"  rank: "+ rank);
            	System.out.println("answered:   "+top10ofwikis.get(0).DocName.get("title"));
            	if(answer.toLowerCase().contains(top10ofwikis.get(0).DocName.get("title").toLowerCase()))
            		matched++;

//            	if(flag==false)
//            		System.out.println(answer);

            }
            
        	System.out.println(count/intop10);
            System.out.printf("Perfect matched: %.2f%%%n", 100*matched/questionList.size());
            System.out.printf("Top 10 matched: %.2f%%%n", 100*intop10/questionList.size());

        }
        
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /* *********************************************************************************************************************/    
    public List<ResultClass> actualSearcher(String query, Directory index ) throws Exception{

        
        List<ResultClass> doc_score_list = new ArrayList<ResultClass>();
        EnglishAnalyzer standardAnalyzer = new EnglishAnalyzer();
       // ShingleAnalyzerWrapper shingleAnalyzerWrapper = new ShingleAnalyzerWrapper(standardAnalyzer,2,2);
   // QUERY
 
        Query q = new QueryParser("content", standardAnalyzer).parse(query);

   // SEARCH
		
		int hitsPerPage = 10;
		IndexReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);
		//1.18 0.15
		searcher.setSimilarity(new BM25Similarity((float) 1.18, (float) 0.15));
		
		TopDocs docs = searcher.search(q, hitsPerPage);
		ScoreDoc[] hits = docs.scoreDocs;

		 
		
		
   // DISPLAY
//		System.out.println("Found " + hits.length + " hits.");
		for(int i=0;i<hits.length;++i) {
		    int docId = hits[i].doc;
		    float score = hits[i].score;
		    
		    Document d = searcher.doc(docId);
		    
		    
			// PUT IN CLASS    
	        ResultClass objResultClass= new ResultClass();
	        objResultClass.DocName= d;
	        objResultClass.doc_score = score;
	        doc_score_list.add(objResultClass);   
		}
		
		for (ResultClass doc_score: doc_score_list) {
//			 System.out.println(doc_score.DocName.get("title")+"   "+doc_score.doc_score);
		}
		return doc_score_list;
    	
    }

    /* *********************************************************************************************************************/    
    private List<String[]>  questionReader() {
    	List<String[]> questions = new ArrayList<String[]>();
        
        //Get file from resources folder
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(questionfile).getFile());

        try (Scanner scanner = new Scanner(file)) {

            while (scanner.hasNextLine()) {
                String category = scanner.nextLine();
                String description = scanner.nextLine();
                // note this title can be title1|title2
                String title = scanner.nextLine();
                
                String[] singleQuestion = {category, description, title};
                questions.add(singleQuestion);
                
                scanner.nextLine();
            }

            scanner.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return questions;
    }
 /**********************************************************************************************************************/    

    public  String questionTokenizer(String question) {
    //	question = question.replaceAll("[^A-Za-z0-9 '-]" ,"");
      
        StringBuilder tokenedquestion = new StringBuilder();
        StandardAnalyzer  qAnalyzer = new  StandardAnalyzer ();  
        
        try {
          TokenStream stream  = qAnalyzer.tokenStream(null, new StringReader(question));
          stream.reset();
          while (stream.incrementToken()) {
        	  String term = stream.getAttribute(CharTermAttribute.class).toString();
        	  if(term.length()>1) {
            	  tokenedquestion.append(stream.getAttribute(CharTermAttribute.class).toString());
            	  tokenedquestion.append(" ");
        	  }

          }
        } catch (IOException e) {
          // not thrown b/c we're using a string reader...
          throw new RuntimeException(e);
        }
        
        // trim last space
        tokenedquestion.deleteCharAt(tokenedquestion.length()-1);

        return tokenedquestion.toString();
        
    }

    
}
