package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Address;
import com.example.tomatomall.repository.AddressRepository;
import com.example.tomatomall.repository.AccountRepository;
import com.example.tomatomall.service.AddressService;
import com.example.tomatomall.util.TokenUtil;
import com.example.tomatomall.util.UserContext;
import com.example.tomatomall.vo.AddressVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.tomatomall.vo.AddressVO;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    UserContext userContext;

    @Override
    public List<AddressVO> getAddresses() {
        Account account = userContext.getCurrentUser();
        List<Address> addresses = addressRepository.getAllByAccount(account);
        return addresses.stream()
                .map(address -> {
                    AddressVO addressVO = new AddressVO();
                    BeanUtils.copyProperties(address, addressVO);
                    return addressVO;
                })
                .collect(Collectors.toList());
    }

    @Override
    public AddressVO createAddress(AddressVO addressVO) {
        // 设置非默认地址
        addressVO.setIsDefault(false);

        // 保存地址
        Address address = addressVO.toPO(userContext);
        address = addressRepository.save(address);

        return address.toVO();
    }

    @Override
    public String deleteAddress(Integer shoppingAddressId) {
        Optional<Address> addressOpt = addressRepository.findById(shoppingAddressId);
        if (!addressOpt.isPresent()) {
            throw new RuntimeException("收货地址不存在");
        }

        addressRepository.delete(addressOpt.get());
        return "删除成功";
    }

    @Override
    public String updateAddress(AddressVO addressVO) {
        if (addressVO.getShoppingAddressId() == null) {
            throw new RuntimeException("地址ID不能为空");
        }

        Optional<Address> addressOpt = addressRepository.findById(addressVO.getShoppingAddressId());
        if (!addressOpt.isPresent()) {
            throw new RuntimeException("收货地址不存在");
        }

        Address address = addressOpt.get();

        // 只更新非空字段
        if (addressVO.getName() != null) {
            address.setName(addressVO.getName());
        }
        if (addressVO.getPhone() != null) {
            address.setPhone(addressVO.getPhone());
        }
        if (addressVO.getZipcode() != null) {
            address.setZipcode(addressVO.getZipcode());
        }
        if (addressVO.getAddress() != null) {
            address.setAddress(addressVO.getAddress());
        }
        if (addressVO.getIsDefault() != null) {
            address.setIsDefault(addressVO.getIsDefault());
        }

        addressRepository.save(address);
        return "更新成功";
    }
}