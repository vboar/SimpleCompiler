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
    private int state;

    // 存储的结果
    private StringBuilder result;
    private String buffer;
    private int position;

    // 符号表
    private List<String> symbolTable = new ArrayList<String>();

    // 常量表
    private List<String> constTable = new ArrayList<String>();

    public static void main(String[] args) {
        new Lex().scanner();
    }

    public Lex() {
        state = 0;
        result = new StringBuilder();
        buffer = "";
    }

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
                        buffer = "< " + c + " , 界符 >\n";
                        result.append(buffer);
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
                        position = inConstTable(buffer);
                        buffer = "< " + buffer + " , 整数常量 , " + position + " >\n";
                        result.append(buffer);
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
                        position = inConstTable(buffer);
                        buffer = "< " + buffer + " , 浮点数常量 , " + position + " >\n";
                        result.append(buffer);
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
                            buffer = "< " + buffer + " , 关键字 >\n";
                        } else {
                            position = inSymbolTable(buffer);
                            buffer = "< " + buffer + " , 标识符 , " + position + " >\n";
                        }
                        result.append(buffer);
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
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
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
                    buffer = "< " + buffer + " , 操作符 >\n";
                    result.append(buffer);
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
                    position = inConstTable(buffer);
                    buffer = "< " + buffer + " , 字符常量 , " + position + " >\n";
                    result.append(buffer);
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
                    position = inConstTable(buffer);
                    buffer = "< " + buffer + " , 字符串常量 , " + position + " >\n";
                    result.append(buffer);
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
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 27:
                    buffer = "< " + buffer + " , 操作符 >\n";
                    result.append(buffer);
                    state = 0;
                    buffer = "";
                    continue;
                case 7: // -
                    if (c == '-' || c == '=') {
                        buffer += c;
                        state = 28;
                        return;
                    } else {
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 28:
                    buffer = "< " + buffer + " , 操作符 >\n";
                    result.append(buffer);
                    state = 0;
                    buffer = "";
                    continue;
                case 8:
                    if (c == '=') {
                        buffer += c;
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
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
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 29:
                    buffer = "< " + buffer + " , 操作符 >\n";
                    result.append(buffer);
                    state = 0;
                    buffer = "";
                    continue;
                case 10: // |
                    if (c == '|' || c == '=') {
                        buffer += c;
                        state = 30;
                        return;
                    } else {
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 30:
                    buffer = "< " + buffer + " , 操作符 >\n";
                    result.append(buffer);
                    state = 0;
                    buffer = "";
                    continue;
                case 11:    // ^
                    if (c == '=') {
                        buffer += c;
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 12:    // !
                    if (c == '=') {
                        buffer += c;
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 13:    // >
                    if (c == '=' || c == '>') {
                        buffer += c;
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 14:    // <
                    if (c == '=' || c == '<') {
                        buffer += c;
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 15:    // =
                    if (c == '=') {
                        buffer += c;
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 32:    // %
                    if (c == '=') {
                        buffer += c;
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        return;
                    } else {
                        buffer = "< " + buffer + " , 操作符 >\n";
                        result.append(buffer);
                        state = 0;
                        buffer = "";
                        continue;
                    }
                case 99:
                    buffer = "< " + buffer + " , 特殊符号 >\n";
                    result.append(buffer);
                    state = 0;
                    buffer = "";
                    continue;
                case 100:
                    buffer = "< " + buffer + " , 特殊符号 >\n";
                    result.append(buffer);
                    state = 0;
                    buffer = "";
                    return;
            }
        }
    }

    private void scanner() {
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
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.txt")));
            String temp = "Token序列如下：\n" + result;
            System.out.println(temp);
            bw.write(temp);
            System.out.print("符号表如下:\n");
            bw.write("\n符号表如下:\n");
            for (int i = 0; i < symbolTable.size(); i++) {
                temp = i + "\t" + symbolTable.get(i) + "\n";
                System.out.print(temp);
                bw.write(temp);
            }
            System.out.print("\n常量表如下:\n");
            bw.write("\n常量表如下:\n");
            for (int i = 0; i < constTable.size(); i++) {
                temp = i + "\t" + constTable.get(i) + "\n";
                System.out.print(temp);
                bw.write(temp);
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fail(String error) {
        buffer = "Error: " + error + "\n";
        result.append(buffer);
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
     * 插入并返回常量在常量表的位置
     * @param s
     * @return
     */
    private int inConstTable(String s) {
        for (int i = 0; i < constTable.size(); i++) {
            if (s.equals(constTable.get(i))) return i;
        }
        constTable.add(s);
        return constTable.size()-1;
    }

}
