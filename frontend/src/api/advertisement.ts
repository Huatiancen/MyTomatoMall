import { axios } from '../utils/request.ts'
import { ADVERTISEMENT_MODULE } from './_prefix.ts'

// 广告信息
export type Advertisement = {
    id: number
    title: string
    content: string
    imgUrl: string
    productId: number
}

// 广告信息
export type AdvertisementToSubmit = {
    title: string
    content: string
    imgUrl: string
    productId: number
}

// 通用响应类型
export type Response<T> = {
    code: number
    message: string
    data: T
}

// 获取所有广告信息
export const getAdvertisements = (): Promise<Response<Advertisement[]>> => {
    return axios.get(`${ADVERTISEMENT_MODULE}`).then(res => res.data)
}

// 更新广告信息
export const updateAdvertisement = (advertisement: AdvertisementToSubmit): Promise<Response<string>> => {
    return axios.put(`${ADVERTISEMENT_MODULE}`, advertisement, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => res.data)
}

// 创建广告
export const addAdvertisement = (advertisement: AdvertisementToSubmit): Promise<Response<Advertisement>> => {
    return axios.post(`${ADVERTISEMENT_MODULE}`, advertisement, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => res.data)
}

// 删除广告
export const deleteAdvertisement = (id: number): Promise<Response<string>> => {
    return axios.delete(`${ADVERTISEMENT_MODULE}/${id}`).then(res => res.data)
}
