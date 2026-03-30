package com.example.tomatomall.service;

import com.example.tomatomall.vo.AccountVO;

public interface AccountService {
    public AccountVO getUser(String username); // 按照接口要求，使用username进行查找，则要保证username字段Unique
    public String createUser(AccountVO accountVO);
    public String updateUser(AccountVO accountVO);
    public String login(String username, String password);
    public Boolean checkPassword(String username, String password); // 检查密码是否变化
}
