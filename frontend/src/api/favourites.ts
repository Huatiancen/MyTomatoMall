import { axios } from '../utils/request.ts'
import { FAVOURITE_MODULE } from './_prefix.ts'

// 收藏项信息
export type FavouriteItem = {
    id: number
    title: string
    price: number
    description: string
    cover: string
    detail: string
    status?: string
}

// 通用响应类型
export type Response<T> = {
    code: number
    message: string
    data: T
}

// 获取当前用户的收藏列表
export const getFavourites = (): Promise<Response<FavouriteItem[]>> => {
    return axios.get(`${FAVOURITE_MODULE}`).then(res => res.data)
}

// 添加商品到收藏夹
export const addToFavourites = (productId: number): Promise<Response<string>> => {
    return axios.post(`${FAVOURITE_MODULE}`, { productId: productId }, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => res.data)
}

// 从收藏夹中删除商品
export const removeFromFavourites = (productId: number): Promise<Response<string>> => {
    return axios.delete(`${FAVOURITE_MODULE}/${productId}`)
        .then((res) => res.data)
}

// 检查商品是否已收藏（可选）
export const isFavourite = (productId: number): Promise<Response<boolean>> => {
    return axios.get(`${FAVOURITE_MODULE}/check/${productId}`)
        .then((res) => res.data)
}
