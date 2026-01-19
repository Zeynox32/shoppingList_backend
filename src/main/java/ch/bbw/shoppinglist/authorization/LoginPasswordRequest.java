package ch.bbw.shoppinglist.authorization;

import lombok.Data;

@Data
public class LoginPasswordRequest {
    private String username;
    private String password;

    public LoginPasswordRequest() {}
}