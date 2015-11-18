import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by Vboar on 2015/11/15.
 */
public class Syntax {

    // 终结符
    private List<String> terminals = new ArrayList<String>();

    // 非终结符
    private List<String> nonTerminals = new ArrayList<String>();

    // 产生式
    private List<Production> productions = new ArrayList<Production>();

    // Token序列
    private List<String> tokens;

    // First
    private List<FirstFollow> firsts = new ArrayList<FirstFollow>();

    // Follow
    private List<FirstFollow> follows = new ArrayList<FirstFollow>();

    // 预测分析表 PPT
    private Production[][] parsingTable;

    private Stack<String> stack = new Stack<String>();

    int pointer = 0;

    public void start() {
        Lex lex = new Lex();
        lex.scanner();
        tokens = lex.getToken();
        cfgScanner();
        getFirst();
        getFollow();
        getParsingTable();
        prepareToFile();
        syntaxParsing();
    }

    // 解析CFG，获取文法、终结符和非终结符
    private void cfgScanner() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("CFG.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                char[] lineChars = line.toCharArray();
                char bracket = '_';
                char last = '_';
                String tmp = "";
                List<String> terminalsTemp = new ArrayList<String>();
                List<String> nonTerminalsTemp = new ArrayList<String>();
                List<String> sequence = new ArrayList<String>();
                for (int i = 0; i < lineChars.length; i++) {
                    if (bracket == '_') {
                        if (last == ']' && lineChars[i] ==']') {
                            terminalsTemp.set(terminalsTemp.size()-1,
                                    terminalsTemp.get(terminalsTemp.size()-1) + "]");
                            sequence.set(sequence.size()-1, sequence.get(sequence.size()-1) + "]");
                        } else if (lineChars[i] =='[') {
                            bracket = ']';
                        } else if (lineChars[i] =='<') {
                            bracket = '>';
                        }
                    } else {
                        if (lineChars[i] == bracket) {
                            if (lineChars[i] == ']') {
                                terminalsTemp.add(tmp);
                                sequence.add(tmp);
                                tmp = "";
                                bracket = '_';
                            } else {
                                nonTerminalsTemp.add(tmp);
                                sequence.add(tmp);
                                tmp = "";
                                bracket = '_';
                            }
                        } else {
                            tmp = tmp + lineChars[i];
                        }
                    }
                    last = lineChars[i];
                }
                for (String s: terminalsTemp) {
                    if (!inTerminals(s)) {
                        terminals.add(s);
                    }
                }
                for (String s: nonTerminalsTemp) {
                    if (!inNonTerminals(s)) {
                        nonTerminals.add(s);
                    }
                }
                Production production = new Production();
                production.left = sequence.get(0);
                for (int i = 1; i < sequence.size(); i++) {
                    production.right.add(sequence.get(i));
                }
                productions.add(production);
            }
            terminals.add("$");
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 获得First集
    private void getFirst() {

        // 如果X是一个终结符，那么First(X) = X
        for (String s: terminals) {
            FirstFollow firstFollow = new FirstFollow();
            firstFollow.left = s;
            firstFollow.right.add(s);
            firsts.add(firstFollow);
        }

        // 对于非终结符 初始化
        for (String s: nonTerminals) {
            addLeftInFirsts(s);
        }

        // 如果X→ε是一个产生式，则ε加入First(X)
        for (String s: nonTerminals) {
            for (Production production: productions) {
                if (production.left.equals(s) && production.right.size() == 1
                        && production.right.get(0).equals("ε")) {

                    firsts.get(getPositionInFirsts(s)).right.add("ε");
                }
            }
        }

        // 如果X是一个非终结符...
        boolean flag = false;
        while (!flag) {
            flag = true;
            for (String s: nonTerminals) {
                for (Production production: productions) {
                    if (production.left.equals(s)) {    // 产生式左边是该非终结符
                        int counter = 0;
                        for (String r: production.right) {
                            FirstFollow f = firsts.get(getPositionInFirsts(r));
                            for (String er: f.right) {
                                if (!er.equals("ε") && !isRightInFirsts(s, er)) {
                                    addRightInFirsts(s, er);
                                    flag = false;
                                }
                            }
                            if (!isRightInFirsts(r, "ε")) {
                                break;
                            } else {
                                counter++;
                            }
                        }
                        if (counter == production.right.size() && !isRightInFirsts(s, "ε")) {
                            addRightInFirsts(s, "ε");
                            flag = false;
                        }
                    }
                }
            }
        }
    }

    // 获得Follow集
    private void getFollow() {

        // 初始化
        for (String s: terminals) {
            addLeftInFollows(s);
        }
        for (String s: nonTerminals) {
            addLeftInFollows(s);
        }
        addRightInFollows("Program", "$");

        for (String s: nonTerminals) {
            for (Production production: productions) {
                if (production.left.equals(s)) {
                    for (int i = 0; i < production.right.size()-1; i++) {
                        for (String t: firsts.get(getPositionInFirsts(production.right.get(i+1))).right) {
                            if (!isRightInFollows(production.right.get(i), t) && !t.equals("ε")) {
                                addRightInFollows(production.right.get(i), t);
                            }
                        }
                    }
                }
            }
        }

        boolean flag = false;
        while (!flag) {
            flag = true;
            for (String s: nonTerminals) {
                for (Production production: productions) {
                    if (production.left.equals(s)) {
                        for (int i = production.right.size()-1; i > -1; i--) {
                            for (String t: follows.get(getPositionInFollows(s)).right) {
                                if (!isRightInFollows(production.right.get(i), t)) {
                                    addRightInFollows(production.right.get(i), t);
                                    flag = false;
                                }
                            }
                            if (!isRightInFirsts(production.right.get(i), "ε")) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    // 获得预测分析表PPT
    private void getParsingTable() {
        parsingTable = new Production[nonTerminals.size()][terminals.size()];

        for (String s: nonTerminals) {
            for (Production production: productions) {
                if (production.left.equals(s)) {
                    int counter = 0;
                    for (String t: production.right) {
                        FirstFollow firstFollow = firsts.get(getPositionInFirsts(t));
                        for (String r: firstFollow.right) {
                            addInPPT(s, r, production);
                        }
                        if (!isRightInFirsts(t, "ε")) {
                            break;
                        } else {
                            counter++;
                        }
                    }
                    if (counter == production.right.size()) {
                        FirstFollow firstFollow = follows.get(getPositionInFollows(s));
                        for (String t: firstFollow.right) {
                            if (inTerminals(t)) {
                                addInPPT(s, t, production);
                            }
                        }
                    }
                }
            }
        }

    }

    // 进行语法分析
    private void syntaxParsing() {
        stack.push("$");
        stack.push("Program");
        String action = "";
        while (!stack.isEmpty()) {
            if (inTerminals(stack.peek())) {
                if (tokens.get(pointer).equals(stack.pop())) {
                    action = "匹配 " + tokens.get(pointer);
                } else {
                    action = "不可接受的终结符: " + tokens.get(pointer);
                    outputProcess(action);
                    break;
                }
                pointer++;
            } else if (inNonTerminals(stack.peek())) {
                Production p = getInPPT(stack.peek(), tokens.get(pointer));
                if (p == null) {
                    action = "无对应的产生式";
                    outputProcess(action);
                    break;
                } else {
                    stack.pop();
                    for (int i = 0; i < p.right.size(); i++) {
                        String temp = p.right.get(p.right.size()-i-1);
                        if (!temp.equals("ε")) {
                            stack.push(temp);
                        }
                    }
                    action = "输出: " + p.left + " -> ";
                    for (String s: p.right) {
                        action += s + " ";
                    }
                }
            }
            outputProcess(action);
        }
    }

    public void prepareToFile() {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt")));
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void outputProcess(String action) {
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt", true)));

            System.out.print("Match:\t\t");
            bw.write("Match:\t\t");
            for (int i = 0; i <= pointer-1; i++) {
                System.out.print(tokens.get(i) + " ");
                bw.write(tokens.get(i) + " ");
            }
            System.out.print("\nStack:\t\t");
            bw.write("\nStack:\t\t");
            for (String s: stack) {
                System.out.print(s + " ");
                bw.write(s + " ");
            }
            System.out.print("\nInput:\t\t");
            bw.write("\nInput:\t\t");
            for (int i = pointer; i < tokens.size(); i++) {
                System.out.print(tokens.get(i) + " ");
                bw.write(tokens.get(i) + " ");
            }
            System.out.print("\nAction:\t\t" + action + "\n\n");
            bw.write("\nAction:\t\t" + action + "\n\n");
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    public void outputPPT() {
        for (int i = 0; i < nonTerminals.size(); i++) {
            for (int j = 0; j < terminals.size(); j++) {
                Production p = parsingTable[i][j];
                System.out.print("<" + nonTerminals.get(i) + "> [" + terminals.get(j) + "]\t");
                if (p == null) {
                    System.out.print("_");
                } else {
                    System.out.print(p.left + " -> ");
                    for (String t: p.right) {
                        System.out.print(t + " ");
                    }
                }
                System.out.print("\n");
            }
        }
    }

    public void outputFirstFollows() {
        for (FirstFollow firstFollow: firsts) {
            System.out.print("First( " + firstFollow.left + " ) → ");
            for (String s: firstFollow.right) {
                System.out.print(s + " ");
            }
            System.out.println();
        }
        for (FirstFollow firstFollow: follows) {
            System.out.print("Follow( " + firstFollow.left + " ) → ");
            for (String s: firstFollow.right) {
                System.out.print(s + " ");
            }
            System.out.println();
        }
    }

    public void outputProductions() {
        for (Production production: productions) {
            System.out.print(production.left + " -> ");
            for (String s: production.right) {
                System.out.print(s + " ");
            }
            System.out.println();
        }
    }

    public void outputTerminals() {
        for (String s: terminals) {
            System.out.print(s + " ");
        }
        System.out.println();
    }

    public void outputNonTerminals() {
        for (String s: nonTerminals) {
            System.out.print(s + " ");
        }
        System.out.println();
    }

    private boolean inTerminals(String s) {
        for (String str: terminals) {
            if (str.equals(s)) return true;
        }
        return false;
    }

    private boolean inNonTerminals(String s) {
        for (String str: nonTerminals) {
            if (str.equals(s)) return true;
        }
        return false;
    }

    private int getPositionInFirsts(String s) {
        for (int i = 0; i < firsts.size(); i++) {
            if (firsts.get(i).left.equals(s)) return i;
        }
        return -1;
    }

    private int getPositionInFollows(String s) {
        for (int i = 0; i < follows.size(); i++) {
            if (follows.get(i).left.equals(s)) return i;
        }
        return -1;
    }

    private boolean isRightInFirsts(String left, String s) {
        for (FirstFollow firstFollow: firsts) {
            if (firstFollow.left.equals(left)) {
                for (String str: firstFollow.right) {
                    if (str.equals(s)) return true;
                }
                break;
            }
        }
        return false;
    }

    private void addLeftInFirsts(String s) {
        if (getPositionInFirsts(s) == -1) {
            FirstFollow firstFollow = new FirstFollow();
            firstFollow.left = s;
            firsts.add(firstFollow);
        }
    }

    private void addRightInFirsts(String s, String r) {
        firsts.get(getPositionInFirsts(s)).right.add(r);
    }

    private boolean isRightInFollows(String left, String s) {
        for (FirstFollow firstFollow: follows) {
            if (firstFollow.left.equals(left)) {
                for (String str: firstFollow.right) {
                    if (str.equals(s)) return true;
                }
                return false;
            }
        }
        return false;
    }

    private void addLeftInFollows(String s) {
        if (getPositionInFollows(s) == -1) {
            FirstFollow firstFollow = new FirstFollow();
            firstFollow.left = s;
            follows.add(firstFollow);
        }
    }

    private void addRightInFollows(String s, String r) {
        follows.get(getPositionInFollows(s)).right.add(r);
    }

    private void addInPPT(String n, String t, Production p) {
        int x = 0,y = 0;
        for (int i = 0; i < nonTerminals.size(); i++) {
            if (nonTerminals.get(i).equals(n)) {
                x = i;
                break;
            }
        }
        for (int i = 0; i < terminals.size(); i++) {
            if (terminals.get(i).equals(t)) {
                y = i;
                break;
            }
        }
        parsingTable[x][y] = p;
    }

    private Production getInPPT(String n, String t) {
        int x = 0,y = 0;
        for (int i = 0; i < nonTerminals.size(); i++) {
            if (nonTerminals.get(i).equals(n)) {
                x = i;
                break;
            }
        }
        for (int i = 0; i < terminals.size(); i++) {
            if (terminals.get(i).equals(t)) {
                y = i;
                break;
            }
        }
        return parsingTable[x][y];
    }
}
