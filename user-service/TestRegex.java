import java.util.regex.Pattern;
public class TestRegex {
    public static void main(String[] args) {
        String regex = "^\\+84[0-9]{9,10}$|^0[0-9]{9,10}$";
        String test = "0886150543";
        System.out.println("Matches: " + Pattern.matches(regex, test));
    }
}
