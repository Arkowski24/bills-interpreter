package DocumentSystem;

import Cleaner.*;
import DocumentRepresentation.BillFragment;
import Parser.*;
import com.martiansoftware.jsap.JSAPResult;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;

public class ConsumersBillDocumentSystem extends PolishDocumentSystem {
    public void interpret(JSAPResult parsingResults){
        boolean showTableOfContents = parsingResults.getBoolean("showTableOfContents");
        if (showTableOfContents){
            interpretTableOfContents(parsingResults);
        }
        else {
            interpretShowSection(parsingResults);
        }
    }

    private void interpretTableOfContents(JSAPResult parsingResults){
        String section = parsingResults.getString("section");
        if (section == null){
            System.out.println(this.getTableOfContents());
        }
        else {
            try {
                System.out.println(this.getSectionTableOfContents(section));
            }
            catch (IllegalArgumentException e){
                System.out.println("No such section.");
                return;
            }
        }
    }

    private void interpretShowSection(JSAPResult parsingResults) {
        String sectionNumber = parsingResults.getString("section");
        String chapterNumber = parsingResults.getString("chapter");
        if (sectionNumber == null){
            interpretShowArticleRange(parsingResults);
        }
        else if (chapterNumber == null){
            try {
                System.out.println(this.getSectionContent(sectionNumber));
            }
            catch(IllegalArgumentException e){
                System.out.println("No such section.");
            }
        }
        else {
            try {
                System.out.println(this.getChapterContent(sectionNumber, chapterNumber));
            }
            catch(IllegalArgumentException e){
                System.out.println("No such section or chapter.");
            }
        }
    }

    public ConsumersBillDocumentSystem(String filepath) throws IOException {
        super();
        readDocument(filepath);
        fillCleanerRules();
        fillConsumersParser();
        cleaner.clearDocument(billDocument);
        cleaner.connectBrokenWords(billDocument);
        parser.parseDocument(billDocument);
    }

    public ConsumersBillDocumentSystem(List<String> fileLines){
        super();
        fillCleanerRules();
        fillConsumersParser();
        readDocument(fileLines);
        cleaner.clearDocument(billDocument);
        cleaner.connectBrokenWords(billDocument);
        parser.parseDocument(billDocument);
    }

    private void fillCleanerRules(){
        cleaner.addNewCleanRule(new CleanerRule("(?m)^©Kancelaria Sejmu", CleanerRuleType.DeleteLineWithPhrase));
        cleaner.addNewCleanRule(new CleanerRule("(?m)^[0-9]{4}-[0-9]{2}-[0-9]{2}$", CleanerRuleType.DeleteLineWithPhrase));
        cleaner.addNewCleanRule(new CleanerRule("(?m)^[a-zA-Z]$", CleanerRuleType.DeleteLineWithPhrase));
        cleaner.addNewCleanRule(new CleanerRule("(?m)^[0-9]$", CleanerRuleType.DeleteLineWithPhrase));
    }

    private void fillConsumersParser(){
        ParserRule litera = new ParserRule("((?m)^[a-z]{3}\\))|((?m)^[a-z]{2}\\))|((?m)^[a-z]{1}\\))", ParserRuleType.Unlimited);
        ParserRule punkt = new ParserRule("((?m)^[0-9]{3}[a-z]{1}\\))|((?m)^[0-9]{3}\\))" +
                "|((?m)^[0-9]{2}[a-z]{1}\\))|((?m)^[0-9]{2}\\))" +
                "|((?m)^[0-9]{1}[a-z]{1}\\))|((?m)^[0-9]{1}\\))", ParserRuleType.Unlimited);
        ParserRule punkt2 = new ParserRule("((?m)^[0-9]{3}[a-z]{1}\\))|((?m)^[0-9]{3}\\))" +
                "|((?m)^[0-9]{2}[a-z]{1}\\))|((?m)^[0-9]{2}\\))" +
                "|((?m)^[0-9]{1}[a-z]{1}\\))|((?m)^[0-9]{1}\\))", ParserRuleType.NoMatch);
        ParserRule ustep = new ParserRule("((?m)^[0-9]{3}[a-z]{1}\\.)|((?m)^[0-9]{3}\\.)" +
                "|((?m)^[0-9]{2}[a-z]{1}\\.)|((?m)^[0-9]{2}\\.)" +
                "|((?m)^[0-9]{1}[a-z]{1}\\.)|((?m)^[0-9]{1}\\.)", ParserRuleType.Unlimited);
        ParserRule artykul = new ParserRule("(Art.\\s[0-9]{3}–[0-9]{3}\\.)" +
                "|(Art.\\s[0-9]{3}[a-z]{1}\\.)|(Art.\\s[0-9]{3}\\.)" +
                "|(Art.\\s[0-9]{2}[a-z]{1}\\.)|(Art.\\s[0-9]{2}\\.)" +
                "|(Art.\\s[0-9]{1}[a-z]{1}\\.)|(Art.\\s[0-9]{1}\\.)", ParserRuleType.Unlimited);
        ParserRule artykul2 = new ParserRule("(Art.\\s[0-9]{3}–[0-9]{3}\\.)" +
                "|(Art.\\s[0-9]{3}[a-z]{1}\\.)|(Art.\\s[0-9]{3}\\.)" +
                "|(Art.\\s[0-9]{2}[a-z]{1}\\.)|(Art.\\s[0-9]{2}\\.)" +
                "|(Art.\\s[0-9]{1}[a-z]{1}\\.)|(Art.\\s[0-9]{1}\\.)", ParserRuleType.NoMatch);
        ParserRule rozdzial = new ParserRule("(Rozdział [0-9]{4})|(Rozdział [0-9]{3})|(Rozdział [0-9]{2})|(Rozdział [0-9])", ParserRuleType.Unlimited);
        ParserRule dzial = new ParserRule("(DZIAŁ [LCDMIVX]{4})|(DZIAŁ [LCDMIVX]{3})|(DZIAŁ [LCDMIVX]{2})|(DZIAŁ [LCDMIVX])", ParserRuleType.Unlimited);

        punkt.addSubRule(litera);
        punkt2.addSubRule(litera);
        ustep.addSubRule(punkt);
        artykul.addSubRule(ustep);
        artykul.addSubRule(punkt2);
        artykul2.addSubRule(ustep);
        artykul2.addSubRule(punkt2);
        rozdzial.addSubRule(artykul);
        dzial.addSubRule(rozdzial);
        dzial.addSubRule(artykul2);

        parser.addParserRule(dzial);
    }

    public String getTableOfContents(){
        BillFragment fragment = billDocument.getBillFragment();
        if (fragment == null){
            throw new IllegalStateException("Document hasn't been parsed, yet.");
        }

        Predicate<BillFragment> terminalPredicate = (BillFragment x) -> (x.getIdentifier() == null || x.getIdentifier().contains("Rozdział") || x.getIdentifier().contains("DZIAŁ"));
        Predicate<String> contentPredicate = (String x) -> x != null && x.replaceAll("([A-Z]+)|(\\W)+", "").length() < x.length();
        return appendList(fragment.getTableOfContentsWithEndingPredicateAndContentPredicate(2, terminalPredicate, contentPredicate));
    }

    public BillFragment getSection(String sectionNumber){
        String sectionIdentifier = "DZIAŁ " + getRomanNumber(sectionNumber);

        BillFragment section = billDocument.getBillFragment().findFirstFragmentWithIdentifier(sectionIdentifier);
        if (section == null){
            throw new IllegalArgumentException("Couldn't find: " + sectionIdentifier);
        }

        return section;
    }

    public List<BillFragment> getSectionsInRange(String rangeStart, String rangeEnd){
        String rangeStartIdentifier = "DZIAŁ " + getRomanNumber(rangeStart);
        String rangeEndIdentifier = "DZIAŁ " + getRomanNumber(rangeEnd);

        Predicate<BillFragment> sectionPredicate = (x) -> x.getIdentifier() != null && x.getIdentifier().contains("DZIAŁ ");

        List<BillFragment> sections = getPartsInRange(billDocument.getBillFragment(), sectionPredicate, rangeStartIdentifier, rangeEndIdentifier);
        return sections;
    }

    public BillFragment getChapter(String sectionNumber, String chapterNumber){
        String chapterIdentifier = "Rozdział" + chapterNumber;

        BillFragment chapter = billDocument.getBillFragment().findFirstFragmentWithIdentifier(chapterIdentifier);
        if (chapter == null){
            throw new IllegalArgumentException("Couldn't find: " + chapterIdentifier);
        }

        return chapter;
    }

    public List<BillFragment> getChaptersInRange(String sectionNumber, String rangeStart, String rangeEnd){
        BillFragment section;
        try {
            section = getSection(sectionNumber);
        }
        catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Couldn't find section. " + e);
        }
        String rangeStartIdentifier = "Rozdział " + rangeStart;
        String rangeEndIdentifier = "Rozdział  " + rangeEnd;

        Predicate<BillFragment> chapterPredicate = (x) -> x.getIdentifier() != null && x.getIdentifier().contains("Rozdział ");

        List<BillFragment> sections = getPartsInRange(billDocument.getBillFragment(), chapterPredicate, rangeStartIdentifier, rangeEndIdentifier);
        return sections;
    }

    public BillFragment getLetter(String articleNumber, String paragraphNumber, String pointNumber, String letterNumber){
        BillFragment point = getPoint(articleNumber, paragraphNumber, pointNumber);

        String letterIdentifier = letterNumber + ")";
        BillFragment letter = point.findFirstFragmentWithIdentifier(letterIdentifier);
        if (point == null){
            throw new IllegalArgumentException("Couldn't find: " + letterIdentifier);
        }

        return letter;
    }

    public List<BillFragment> getLetterInRange(String articleNumber, String paragraphNumber, String pointNumber, String rangeStart, String rangeEnd){
        BillFragment point;
        try {
            point = getPoint(articleNumber, paragraphNumber, pointNumber);
        }
        catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Couldn't find section. " + e);
        }
        String rangeStartIdentifier =  rangeStart + ")";
        String rangeEndIdentifier = rangeEnd + ")";

        Predicate<BillFragment> letterPredicate = (x) -> x.getIdentifier() != null && x.getIdentifier().matches("[a-z]+\\)");

        List<BillFragment> letter = getPartsInRange(point, letterPredicate, rangeStartIdentifier, rangeEndIdentifier);
        return letter;
    }

    public String getSectionTableOfContents(String sectionNumber){
        BillFragment section;
        try {
            section = getSection(sectionNumber);
        }
        catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Couldn't get table of contents.");
        }

        Predicate<BillFragment> terminalPredicate = (BillFragment x) -> (x.getIdentifier() == null || x.getIdentifier().contains("Rozdział") || x.getIdentifier().contains("DZIAŁ"));
        Predicate<String> contentPredicate = (String x) -> true;
        return appendList(section.getTableOfContentsWithEndingPredicateAndContentPredicate(2, terminalPredicate, contentPredicate));
    }

    public String getSectionContent(String sectionNumber) {
        BillFragment section;
        try {
            section = getSection(sectionNumber);
        }
        catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Couldn't get content. " + e);
        }

        return section.getFragmentContentWithChildren();
    }

    public String getChapterContent(String sectionNumber, String chapterNumber) {
        BillFragment chapter;
        try {
            chapter = getChapter(sectionNumber, chapterNumber);
        }
        catch (IllegalArgumentException e){
            throw new IllegalArgumentException("Couldn't get content. " + e);
        }

        return chapter.getFragmentContentWithChildren();
    }
}
