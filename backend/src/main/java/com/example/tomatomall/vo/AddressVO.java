package com.example.tomatomall.vo;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Address;
import com.example.tomatomall.repository.AccountRepository;
import com.example.tomatomall.util.UserContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressVO {
    private Integer shoppingAddressId;
    private String name;
    private String phone;
    private String zipcode;
    private String address;
    private Boolean isDefault;

    public Address toPO(UserContext userContext) {
        Address address = new Address();
        address.setShoppingAddressId(this.shoppingAddressId);
        address.setAccount(userContext.getCurrentUser());
        address.setName(this.name);
        address.setPhone(this.phone);
        address.setZipcode(this.zipcode);
        address.setAddress(this.address);
        address.setIsDefault(this.isDefault);
        return address;
    }
}