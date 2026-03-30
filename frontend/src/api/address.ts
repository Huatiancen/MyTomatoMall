import { axios } from '../utils/request.ts'
import { ADDRESS_MODULE } from './_prefix.ts'

export type AddressToCreate = {
    name: string;
    phone: string;
    zipcode: string;
    address: string;
}

export type AddressToUpdate = {
    shoppingAddressId: number;
    name?: string;
    phone?: string;
    zipcode?: string;
    address?: string;
    isDefault?: boolean;
}

export const getAddresses = () => {
    return axios.get(`${ADDRESS_MODULE}`).then(res => {
        return res
    });
}

export const createAddress = (address : AddressToCreate) => {
    return axios.post(`${ADDRESS_MODULE}`, address, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => {
        return res
    })
}

export const deleteAddress = (id: number) => {
    return axios.delete(`${ADDRESS_MODULE}/${id}`)
    .then(res => {
        return res
    })
}

export const updateAddress = (address: AddressToUpdate) => {
    return axios.put(`${ADDRESS_MODULE}`, address, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => {
        return res
    })
}
