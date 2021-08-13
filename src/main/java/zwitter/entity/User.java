package zwitter.entity;

import lombok.Data;

@Data
public class User {

    private String loginName;
    private long userId;
    private String userName;
    private int followers;
    private int following;
    private int posts;
    private long signup;
}
