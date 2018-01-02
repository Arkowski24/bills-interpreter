package DocumentSystem;

import Cleaner.*;
import DocumentRepresentation.*;
import Parser.*;

import java.io.IOException;

public class ConstitutionDocumentSystem extends PolishDocumentSystem{

    public ConstitutionDocumentSystem(String filepath) throws  IOException{
        super();
        fillCleanerRules();
        fillConstitutionParser();
        readDocument(filepath);
        cleaner.clearDocument(billDocument);
        cleaner.connectBrokenWords(billDocument);
        parser.parseDocument(billDocument);
    }

    protected void fillCleanerRules(){
        cleaner.addNewCleanRule(new CleanerRule("^[0-9]{4}-[0-9]{2}-[0-9]{2}$", CleanerRuleType.DeleteLineWithPhrase));
        cleaner.addNewCleanRule(new CleanerRule("©Kancelaria Sejmu", CleanerRuleType.DeleteLineWithPhrase));
        cleaner.addNewCleanRule(new CleanerRule("^[a-zA-Z]$", CleanerRuleType.DeleteLineWithPhrase));
        cleaner.addNewCleanRule(new CleanerRule("^[0-9]$", CleanerRuleType.DeleteLineWithPhrase));
    }

    private void fillConstitutionParser(){
        ParserRule parserRule = new ParserRule("((?m)^[0-9]{3}\\.)|((?m)^[0-9]{2}\\.)|((?m)^[0-9]{1}\\.)", ParserRuleType.Unlimited);
        ParserRule parserRule1 = new ParserRule("(Art.\\s[0-9]{3}\\.)|(Art.\\s[0-9]{2}\\.)|(Art.\\s[0-9]{1}\\.)", ParserRuleType.Unlimited);
        ParserRule parserRule2 = new ParserRule("(Rozdział [LCDMIVX]{4})|(Rozdział [LCDMIVX]{3})|(Rozdział [LCDMIVX]{2})|(Rozdział [LCDMIVX])", ParserRuleType.Unlimited);
        parserRule1.subRules.add(parserRule);
        parserRule2.subRules.add(parserRule1);

        parser.addParserRule(parserRule2);
    }

    private BillFragment getChapter(int chapterNumber){
        if (chapterNumber <= 0){
            throw new IllegalArgumentException("Chapter number must be positive.");
        }
        String chapterIdentifier = "Rozdział " + toRoman(chapterNumber);

        BillFragment chapter = billDocument.getBillFragment().findFirstFragmentWithIdentifier(chapterIdentifier);
        if (chapter == null){
            throw new IllegalArgumentException("Couldn't find: " + chapterIdentifier);
        }

        return chapter;
    }

    //To Do - Implement Roman Numbers Converter
    private String toRoman(int number){
        switch (number){
            case 1:
                return "I";
            case 2:
                return "II";
            default:
                return "I";
        }
    }

    public String getChapterContent(int chapterNumber) {
        BillFragment chapter;
        try {
            chapter = getChapter(chapterNumber);
        }
        catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Couldn't get content. " + e);
        }

        return chapter.getFragmentContentWithChildren();
    }


    public String getChapterTableOfContents(int chapterNumber){
        BillFragment chapter;
        try {
            chapter = getChapter(chapterNumber);
        }
        catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Couldn't get table of contents. " + e);
        }
        return appendList(chapter.getTableOfContents(2));
    }
}