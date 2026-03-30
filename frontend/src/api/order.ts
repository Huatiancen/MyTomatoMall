import { axios } from '../utils/request.ts'
import { ORDER_MODULE } from './_prefix.ts'

export type Order  = {
  orderId: number;
  createTime: string;
  totalAmount: number;
  paymentMethod: string;
  status: string;
  username?: string;
}

export const createPayment = (orderId: number): Promise<Record<string, any>> => {
  return axios.post(`${ORDER_MODULE}/${orderId}/pay`, {
    headers: { 'Content-Type': 'application/json' }
  }).then(res => res.data)
}

export const getOrders = () => {
  return axios.get(`${ORDER_MODULE}`)
        .then(res => {
            return res
        })
}

export const getOrderById = (orderId: number | string) => {
  return axios.get(`${ORDER_MODULE}/${orderId}`)
      .then(res => {
          return res
      })
}

export const getCartItemsById = (orderId: number | string) => {
  return axios.get(`${ORDER_MODULE}/${orderId}/cart`)
      .then(res => {
          return res
      })
}

export const deleteOrder = (orderId: number | string) => {
  return axios.delete(`${ORDER_MODULE}/${orderId}`)
      .then(res => {
          return res
      })
}

export const cancelOrder = (orderId: number | string) => {
  return axios.put(`${ORDER_MODULE}/${orderId}`)
  .then(res => {
      return res
  })
}