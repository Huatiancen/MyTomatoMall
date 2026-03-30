package com.example.tomatomall.vo;

import com.example.tomatomall.po.Account;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountVO {
    private String username;
    private String password;
    private String name; // 真实姓名
    private String avatar; // 头像URL
    private String role;
    private String telephone;
    private String email;
    private String location;

    public Account toPO() {
        Account account = new Account();
        account.setUsername(this.username);
        account.setPassword(this.password);
        account.setName(this.name);
        account.setAvatar(this.avatar);
        account.setRole(this.role);
        account.setTelephone(this.telephone);
        account.setEmail(this.email);
        account.setLocation(this.location);
        return account;
    }

}
