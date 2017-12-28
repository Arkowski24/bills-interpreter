import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class Parser {
    private List<ParserRule> billParserRules;

    public Parser() {
        billParserRules = new ArrayList<>();
    }

    public void addParserRule(ParserRule parserRule) {
        billParserRules.add(parserRule);
    }

    public void parseDocument(BillDocument billDocument) {
        List<String> lines = billDocument.getBillDocumentLines();
        BillFragment billFragment = new BillFragment();

        String content = "";
        for (String billLine : lines) {
            content += billLine + "\n";
        }

        billFragment.setParent(null);
        billFragment.setContent(content);

        parseBillFragment(new BillFragmentWithRules(billFragment, billParserRules, 0));

        billDocument.setBillFragment(billFragment);
    }

    public void parseBillFragment(BillFragmentWithRules parent) {
        if (parent == null || parent.billFragment == null
                || parent.parserRules == null) {
            throw new IllegalArgumentException("Parent fields cannot be null.");
        }

        List<BillFragmentWithRules> children = getChildrenFromParentContent(parent);
        //If no children then parsing is complete
        if (children == null)
            return;

        for (BillFragmentWithRules child : children) {
            child.billFragment.setParent(parent.billFragment);
            parent.billFragment.addChild(child.billFragment);
            parseBillFragment(child);
        }
    }

    public List<BillFragmentWithRules> getChildrenFromParentContent(BillFragmentWithRules parent) {
        //Create matcher for given parentContent using patterns from rules
        String parentContent = parent.billFragment.getContent();
        List<ParserMatcher> matchers =  createMatcherList(parent.parserRules, parentContent);

        //Find first pattern match
        for (ParserMatcher matcher : matchers) {
            matcher.availability = matcher.matcher.find();
        }

        //Find first fragment and separate parent parentContent from parsable children, no match - no parentContent to parse
        BillFragmentWithRules nextBillFragment = getNextFragment(parentContent, matchers);
        if (nextBillFragment == null) {
            return null;
        }
        int fragmentContentEnd = nextBillFragment.startPosition - 1;
        if (fragmentContentEnd < 0) {
            parent.billFragment.setContent(""); //All is parsable
        } else {
            parent.billFragment.setContent(parentContent.substring(0, fragmentContentEnd));
        }

        //Parse all children according to rules
        List<BillFragmentWithRules> fragmentList = new ArrayList<>();
        while (nextBillFragment != null) {
            fragmentList.add(nextBillFragment);
            nextBillFragment = getNextFragment(parentContent, matchers);
        }

        return fragmentList;
    }

    private BillFragmentWithRules getNextFragment(String content, List<ParserMatcher> matchers) {
        //Find the lowest starting position of the new fragment
        ParserMatcher chosenMatcher = getLowestMatcher(matchers);

        //If match was found- increase the counter
        if (chosenMatcher == null) {
            return null;
        }
        chosenMatcher.timesMatched++;

        int identifierStartPosition = chosenMatcher.matcher.start();
        int identifierEndPosition = chosenMatcher.matcher.end();

        //Check whether the rule still applies
        checkIfStillAvailable(chosenMatcher);

        int contentStartPosition = identifierEndPosition + 1;
        int contentEndPosition = getEndOfContentPosition(content, matchers);

        //Separate fragment identifier and content from the rest
        String newBillFragmentIdentifier = content.substring(identifierStartPosition, identifierEndPosition);
        String newBillFragmentContent = content.substring(contentStartPosition, contentEndPosition);

        //Creating new fragment and binding parsing rules to it
        List<ParserRule> newBillRules = chosenMatcher.rule.subRules;
        BillFragment newBillFragment = new BillFragment(newBillFragmentIdentifier, newBillFragmentContent);

        return new BillFragmentWithRules(newBillFragment, newBillRules, identifierStartPosition);
    }

    private List<ParserMatcher> createMatcherList(List<ParserRule> parserRules, String content) {
        List<ParserMatcher> matchers = new ArrayList<>();
        for (ParserRule parserRule : parserRules) {
            Matcher matcher = parserRule.pattern.matcher(content);
            matchers.add(new ParserMatcher(matcher, parserRule));
        }
        return matchers;
    }

    private ParserMatcher getLowestMatcher(List<ParserMatcher> matchers) {
        ParserMatcher chosenMatcher = null;
        int identifierStartPosition = Integer.MAX_VALUE;
        for (ParserMatcher parserMatcher : matchers) {
            if (parserMatcher.availability) {
                int parserEndPosition = parserMatcher.matcher.start();
                if (parserEndPosition < identifierStartPosition) {
                    identifierStartPosition = parserEndPosition;
                    chosenMatcher = parserMatcher;
                }
            }
        }
        return chosenMatcher;
    }

    private void checkIfStillAvailable(ParserMatcher chosenMatcher) {
        //Check if next match exists and move to it
        chosenMatcher.availability = chosenMatcher.matcher.find();
        //Check whether the limit is not exhausted
        if(chosenMatcher.rule.parserRuleType == ParserRuleType.Limited
                && chosenMatcher.timesMatched == chosenMatcher.rule.matchLimit){
            chosenMatcher.availability = false;
        }
    }

    private Integer getEndOfContentPosition(String content, List<ParserMatcher> matchers) {
        //End of content - next rule match position or end of document
        ParserMatcher chosenMatcher = getLowestMatcher(matchers);
        if (chosenMatcher == null){
            return content.length();
        }
        else {
            return chosenMatcher.matcher.start();
        }
    }
}