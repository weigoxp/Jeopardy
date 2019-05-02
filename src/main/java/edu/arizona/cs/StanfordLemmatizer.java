package edu.arizona.cs;


import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.simple.*;

public class StanfordLemmatizer {
	
    public StringBuilder lemmatize(String text)
    {
        Document doc = new Document(text);
        StringBuilder lemma = new StringBuilder();
        for (Sentence sent : doc.sentences()) {
        	List<String> l = sent.lemmas();
        	for( String word: l) {
                lemma.append(word);
                lemma.append(" " );
        	}
        }

        return lemma;
    }
    

}


