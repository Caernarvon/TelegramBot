
/**
 * Class contains info about users.
 */
public class User {
    private String firstName;
    private String lastName;
    private String username;
    private Integer userId;
    private int warnings;

    User() {
        this.warnings = 0;
    }

    int getWarnings() {
        return warnings;
    }

    void setWarnings(int warnings) {
        this.warnings = warnings;
    }

    public String getFirstName() {
        return firstName;
    }

    void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    void setUsername(String username) {
        this.username = username;
    }

    Integer getUserId() {
        return userId;
    }

    void setUserId(Integer userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "User{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", username='" + username + '\'' +
                ", userId=" + userId +
                '}';
    }
}
