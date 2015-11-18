import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vboar on 2015/11/11.
 */
public class Lex {

    // ANSI C 定义的32个关键字
    private static final String[] keywords = {"auto", "break", "case", "char", "const",
            "continue", "default", "do", "double", "else", "enum", "extern", "float", "for",
            "goto", "if", "int", "long", "register", "return", "short", "signed", "static",
            "sizeof", "struct", "switch", "typedef", "union", "unsigned", "void", "volatile",
            "while"};

    // 界符
    private static final char[] delimiters = {';', ',', '(', ')', '{', '}', '[', ']', '.', ':'};

    // 转义字符
    private static final char[] escapes = {'\'', '\"','\\', 'b', 'n', 't'};

    // 当前状态，初始状态为0
    private int state = 0;

    // 临时变量
    private int position;
    private String buffer = "";

    // 存储的结果
    private List<Token> tokenList = new ArrayList<Token>(); // Token序列
    private List<String> symbolTable = new ArrayList<String>(); // 符号表（标识符表）

    private void tokenizer(char c) {
        while (true) {
            switch (state) {
                case 0:
                    if (isBlank(c)) {
                        state = 0;
                        return;
                    } else if (isDigit(c)) {
                        buffer += c;
                        state = 1;
                        return;
                    } else if (isLetter(c) || c == '_') {
                        buffer += c;
                        state = 2;
                        return;
                    } else if (isDelimiter(c)) {
                        buffer += c;
                        tokenList.add(new Token("_DELIMITER", buffer));
                        buffer = "";
                        state = 0;
                        return;
                    } else if (c == '/') {
                        buffer += c;
                        state = 3;
                        return;
                    } else if (c == '\'') {
                        buffer += c;
                        state = 4;
                        return;
                    } else if (c == '\"') {
                        buffer += c;
                        state = 5;
                        return;
                    } else if (c == '+') {
                        buffer += c;
                        state = 6;
                        return;
                    } else if (c == '-') {
                        buffer += c;
                        state = 7;
                        return;
                    } else if (c == '*') {
                        buffer += c;
                        state = 8;
                        return;
                    } else if (c == '&') {
                        buffer += c;
                        state = 9;
                        return;
                    } else if (c == '|') {
                        buffer += c;
                        state = 10;
                        return;
                    } else if (c == '^') {
                        buffer += c;
                        state = 11;
                        return;
                    } else if (c == '!') {
                        buffer += c;
                        state = 12;
                        return;
                    } else if (c == '>') {
                        buffer += c;
                        state = 13;
                        return;
                    } else if (c == '<') {
                        buffer += c;
                        state = 14;
                        return;
                    } else if (c == '=') {
                        buffer += c;
                        state = 15;
                        return;
                    } else if (c == '%') {
                        buffer += c;
                        state = 32;
                        return;
                    } else if (c == '#') {
                        buffer += c;
                        state = 99;
                        return;
                    } else if (c == '$') {
                        buffer += c;
                        state = 100;
                        continue;
                    } else {
                        fail("不能识别的字符");
                        return;
                    }
                case 1: // 整数或浮点数
                    if (isDigit(c)) {
                        buffer += c;
                        state = 1;
                        return;
                    } else if (c == '.') {
                        buffer += c;
                        state = 16;
                        return;
                    } else {
                        tokenList.add(new Token("_NUM", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 16:    // 浮点数 a.
                    if (isDigit(c)) {
                        buffer += c;
                        state = 17;
                        return;
                    } else {
                        fail("非法的浮点数常量");
                        return;
                    }
                case 17:    // 浮点数 a.a
                    if (isDigit(c)) {
                        buffer += c;
                        state = 17;
                        return;
                    } else {
                        tokenList.add(new Token("_NUM", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 2: // 标识符或者关键字
                    if (isLetter(c) || isDigit(c) || c == '_') {
                        buffer += c;
                        state = 2;
                        return;
                    } else {
                        if (isKeyword(buffer)) {
                            tokenList.add(new Token("_KEYWORD", buffer));
                        } else {
                            position = inSymbolTable(buffer);
                            tokenList.add(new Token("_ID", buffer, position));
                        }
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 3: // '/'开头
                    if (c == '*') { // 注释
                        buffer = buffer.substring(0, buffer.length()-1);
                        state = 18;
                        return;
                    } else if (c == '/') {  // 单行注释
                        buffer = buffer.substring(0, buffer.length()-1);
                        state = 19;
                        return;
                    } else if (c == '=') {  // '/='
                        buffer += c;
                        state = 20;
                        return;
                    } else {    // '/'
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 18:
                    if (c == '*') {
                        state = 21;
                        return;
                    } else {
                        state = 18;
                        return;
                    }
                case 21:
                    if (c == '/') {
                        state = 0;
                        return;
                    } else {
                        state = 18;
                        return;
                    }
                case 19:
                    if (c == '\n') {
                        state = 0;
                        return;
                    } else {
                        state = 19;
                        return;
                    }
                case 20:
                    tokenList.add(new Token("_OP", buffer));
                    state = 0;
                    buffer = "";
                    continue;
                case 4: // 字符常量 '
                    if (c == '\\') {
                        buffer += c;
                        state = 22;
                        return;
                    } else if (c != '\'') {
                        buffer += c;
                        state = 23;
                        return;
                    } else {
                        fail("非法的字符常量");
                        return;
                    }
                case 22:
                    if (isEscape(c)) {
                        buffer += c;
                        state = 23;
                        return;
                    } else {
                        fail("非法的转义字符");
                        state = 33;
                        return;
                    }
                case 23:
                    if (c == '\'') {
                        buffer += c;
                        state = 24;
                        continue;
                    } else {
                        buffer += c;
                        state = 31;
                        return;
                    }
                case 33:
                    if (c == '\'') {
                        state = 0;
                        return;
                    } else {
                        state = 33;
                        return;
                    }
                case 31:
                    if (c == '\'') {
                        fail("字符常量长度大于1");
                        return;
                    } else {
                        buffer += c;
                        state = 31;
                        return;
                    }
                case 24:
                    tokenList.add(new Token("_CHAR", buffer));
                    state = 0;
                    buffer = "";
                    return;
                case 5: // 字符串常量
                    if (c == '\\') {
                        buffer += c;
                        state = 26;
                        return;
                    } else if (c == '\"') {
                        buffer += c;
                        state = 25;
                        continue;
                    } else {
                        buffer += c;
                        state = 5;
                        return;
                    }
                case 25:
                    tokenList.add(new Token("_STRING", buffer));
                    state = 0;
                    buffer = "";
                    return;
                case 26:
                    if (isEscape(c)) {
                        buffer += c;
                        state = 5;
                        return;
                    } else {
                        fail("非法的转义字符");
                        state = 34;
                        return;
                    }
                case 34:
                    if (c == '\"') {
                        state = 0;
                        return;
                    } else {
                        state = 34;
                        return;
                    }
                case 6: // +
                    if (c == '+' || c == '=') {
                        buffer += c;
                        state = 27;
                        return;
                    } else {
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 27:
                    tokenList.add(new Token("_OP", buffer));
                    state = 0;
                    buffer = "";
                    continue;
                case 7: // -
                    if (c == '-' || c == '=') {
                        buffer += c;
                        state = 28;
                        return;
                    } else {
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 28:
                    tokenList.add(new Token("_OP", buffer));
                    state = 0;
                    buffer = "";
                    continue;
                case 8:
                    if (c == '=') {
                        buffer += c;
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 9: // &
                    if (c == '&' || c == '=') {
                        buffer += c;
                        state = 29;
                        return;
                    } else {
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 29:
                    tokenList.add(new Token("_OP", buffer));
                    state = 0;
                    buffer = "";
                    continue;
                case 10: // |
                    if (c == '|' || c == '=') {
                        buffer += c;
                        state = 30;
                        return;
                    } else {
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 30:
                    tokenList.add(new Token("_OP", buffer));
                    state = 0;
                    buffer = "";
                    continue;
                case 11:    // ^
                    if (c == '=') {
                        buffer += c;
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 12:    // !
                    if (c == '=') {
                        buffer += c;
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 13:    // >
                    if (c == '=' || c == '>') {
                        buffer += c;
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 14:    // <
                    if (c == '=' || c == '<') {
                        buffer += c;
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 15:    // =
                    if (c == '=') {
                        buffer += c;
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 32:    // %
                    if (c == '=') {
                        buffer += c;
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        tokenList.add(new Token("_OP", buffer));
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 99:
                    tokenList.add(new Token("_SPECIAL", buffer));
                    state = 0;
                    buffer = "";
                    continue;
                case 100:
                    tokenList.add(new Token("_SPECIAL", buffer));
                    state = 0;
                    buffer = "";
                    return;
            }
        }
    }

    public void scanner() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("program.c")));
            String line;
            while ((line = br.readLine()) != null) {
                for (char c: line.toCharArray()) {
                    tokenizer(c);
                }
                tokenizer('\n');
            }
            tokenizer('$');
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void output() {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt")));
            String temp;
            System.out.print("Token序列如下：\n");
            bw.write("Token序列如下：\n");
            for (Token token: tokenList) {
                if (token.getType().equals("_ID")) {
                    temp = "< " + token.getValue() + " , " + token.getType() + " , " + token.getLocation() + " >\n";
                } else {
                    temp = "< " + token.getValue() + " , " + token.getType() + " , _ >\n";
                }
                System.out.print(temp);
                bw.write(temp);
            }
            System.out.print("\n符号表如下:\n");
            bw.write("\n符号表如下:\n");
            for (int i = 0; i < symbolTable.size(); i++) {
                temp = i + "\t" + symbolTable.get(i) + "\n";
                System.out.print(temp);
                bw.write(temp);
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fail(String error) {
        tokenList.add(new Token("_ERROR", error));
        state = 0;
        buffer = "";
    }

    /**
     * 判断是否为字母
     * @param c
     * @return
     */
    private boolean isLetter(char c) {
        return ((c >= 'a' && c <='z') || (c >= 'A' && c <= 'Z'));
    }

    /**
     * 判断是否为数字
     * @param c
     * @return
     */
    private boolean isDigit(char c) {
        return (c >= '0' && c <='9');
    }

    /**
     * 判断是否为关键字
     * @param s
     * @return
     */
    private boolean isKeyword(String s) {
        for (String str: keywords) {
            if (str.equals(s)) return true;
        }
        return false;
    }

    /**
     * 判断是否为空白字符
     * @param c
     * @return
     */
    private boolean isBlank(char c) {
        return c == ' ' || c == '\t' || c == '\n';
    }

    /**
     * 判断是否为分隔符
     * @param c
     * @return
     */
    private boolean isDelimiter(char c) {
        for (char ch: delimiters) {
            if (c == ch) return true;
        }
        return false;
    }

    /**
     * 判断是否为转义字符
     * @param c
     * @return
     */
    private boolean isEscape(char c) {
        for (char ch: escapes) {
            if (c == ch) return true;
        }
        return false;
    }

    /**
     * 插入并返回标识符在符号表的位置
     * @param s
     * @return
     */
    private int inSymbolTable(String s) {
        for (int i = 0; i < symbolTable.size(); i++) {
            if (s.equals(symbolTable.get(i))) return i;
        }
        symbolTable.add(s);
        return symbolTable.size()-1;
    }

    /**
     * 获得简化后的Token序列
     * @return
     */
    public List<String> getToken() {
        List<String> list = new ArrayList<String>();
        for (Token token: tokenList) {
            if (token.getType().equals("_NUM")) {
                list.add("NUM");
            } else if (token.getType().equals("_CHAR")) {
                list.add("CHAR");
            } else if (token.getType().equals("_STRING")) {
                list.add("STRING");
            } else if (token.getType().equals("_ID")) {
                list.add("ID");
            } else if (token.getType().equals("_ERROR")) {
                continue;
            } else {
                list.add(token.getValue());
            }
        }
        return list;
    }

}
