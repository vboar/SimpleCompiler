/**
 * Created by Vboar on 2015/11/14.
 */
public class Token {

    private String type; // _ID, _NUM, _OP, _CHAR, _String, _DELIMITER, _KEYWORD, _ERROR, _SPECIAL

    private String value;

    private int location;   // 标识符在符号表中的位置

    public Token(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public Token(String type, String value, int location) {
        this.type = type;
        this.value = value;
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public int getLocation() {
        return location;
    }

}
