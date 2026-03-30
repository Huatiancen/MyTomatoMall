import { axios } from '../utils/request.ts'
import { CART_MODULE } from './_prefix.ts'

// 购物车商品信息
export type CartItem = {
  cartItemId?: number
  productId: number
  quantity: number
  title?: string
  price?: number
  description?: string
  cover?: string
  productStatus?: string
}

// 购物车信息
export type Cart = {
  items: CartItem[]
}

export type ShoppingAddress = {
  name: string;
  phone: string;
  zipcode: string;
  address: string;
}

// 结算信息
export type Checkout = {
  cartItemIds: number[]
  shoppingAddress: ShoppingAddress
  paymentMethod: string
}

// 订单信息
export type Order = {
  orderId: number
  username: string
  totalAmount: number
  paymentMethod: string
  createTime: string
  status: string
}

// 通用响应类型
export type Response<T> = {
  code: number
  message: string
  data: T
}

// 获取购物车内容
export const getCart = (): Promise<Response<Cart>> => {
  return axios.get(`${CART_MODULE}`).then(res => res.data)
}

// 添加商品到购物车
export const addToCart = (cart: CartItem): Promise<Response<CartItem>> => {
  return axios.post(`${CART_MODULE}`, cart, {
    headers: { 'Content-Type': 'application/json' }
  }).then(res => res.data)
}

// 从购物车中删除商品
export const removeFromCart = (cartItemId: number): Promise<Response<string>> => {
  return axios.delete(`${CART_MODULE}/${cartItemId}`)
      .then((res) => res.data)  // 提取返回的 data
}

// 更新购物车中商品的数量
export const updateCartItem = (cartItemId: number, quantity: number): Promise<Response<string>> => {
  return axios.patch(`${CART_MODULE}/${cartItemId}`, { quantity }, {
    headers: { 'Content-Type': 'application/json' }
  }).then(res => res.data)
}

// 结算购物车
export const checkout = (checkoutInfo: Checkout): Promise<Response<Order>> => {
  // console.log(checkoutInfo)
  return axios.post(`${CART_MODULE}/checkout`, checkoutInfo, {
    headers: { 'Content-Type': 'application/json' }
  }).then(res => res.data)
}
