package com.example.tomatomall.po;

import com.example.tomatomall.vo.AddressVO;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "shopping_addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shopping_address_id")
    private Integer shoppingAddressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private Account account;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "phone", length = 11, nullable = false)
    private String phone;

    @Column(name = "zipcode", length = 6, nullable = false)
    private String zipcode;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    public AddressVO toVO() {
        AddressVO addressVO = new AddressVO();
        addressVO.setShoppingAddressId(this.shoppingAddressId);
        addressVO.setName(this.name);
        addressVO.setPhone(this.phone);
        addressVO.setZipcode(this.zipcode);
        addressVO.setAddress(this.address);
        addressVO.setIsDefault(this.isDefault);
        return addressVO;
    }
}