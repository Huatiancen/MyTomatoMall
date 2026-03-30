package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.repository.AccountRepository;
import com.example.tomatomall.po.Account;
import com.example.tomatomall.service.AccountService;
import com.example.tomatomall.util.TokenUtil;
import com.example.tomatomall.vo.AccountVO;
import com.example.tomatomall.exception.TomatoMallException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private TokenUtil tokenUtil;

    @Override
    public AccountVO getUser(String username) {
        Account account = accountRepository.findByUsername(username);
        if (account == null) {
            throw TomatoMallException.userNotExist();
        }
        return account.toVO();
    }

    @Override
    public String createUser(AccountVO accountVO) {
        if (accountRepository.findByUsername(accountVO.getUsername()) != null) {
            throw TomatoMallException.userAlreadyExist();
        }
        // 获取用户输入的原始密码
        String rawPassword = accountVO.getPassword();

        // 使用 BCryptPasswordEncoder 对密码进行加密
        String encodedPassword = passwordEncoder.encode(rawPassword);
        Account account = accountVO.toPO();
        account.setPassword(encodedPassword);
        accountRepository.save(account);
        return "注册成功";
    }

    @Override
    public String updateUser(AccountVO accountVO) {
        Account existingAccount = accountRepository.findByUsername(accountVO.getUsername());
        if (existingAccount == null) {
            throw TomatoMallException.userNotExist();
        }
        
        // 更新现有账户的信息
        existingAccount.setName(accountVO.getName());
        existingAccount.setAvatar(accountVO.getAvatar());
        existingAccount.setRole(accountVO.getRole());
        existingAccount.setTelephone(accountVO.getTelephone());
        existingAccount.setEmail(accountVO.getEmail());
        existingAccount.setLocation(accountVO.getLocation());
        
        // 如果密码不为空，则更新密码
        if (accountVO.getPassword() != null && !accountVO.getPassword().isEmpty()) {
            String encodedPassword = passwordEncoder.encode(accountVO.getPassword());
            existingAccount.setPassword(encodedPassword);
        }
        
        accountRepository.save(existingAccount);
        return "更新成功";
    }

    @Override
    public String login(String username, String password) {
        Account account = accountRepository.findByUsername(username);
        if (account == null) {
            throw TomatoMallException.userNotExist();
        }
        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw TomatoMallException.passwordError();
        }
        return tokenUtil.getToken(account);
    }

    @Override
    public Boolean checkPassword(String username, String password) {
        Account account = accountRepository.findByUsername(username);
        if (account == null) {
            throw TomatoMallException.userNotExist();
        }
        return passwordEncoder.matches(password, account.getPassword());
    }
}
