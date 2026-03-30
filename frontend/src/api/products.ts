import { axios } from '../utils/request.ts'
import { PRODUCT_MODULE } from './_prefix.ts'

// 商品规格
export type Specification = {
    id?: number
    item: string
    value: string
    productId?: number
}

// 商品信息
export type Product = {
    id: number
    title: string
    price: number
    rate: number
    description: string
    cover: string
    detail: string
    specifications: Specification[]
    tags?: [string, string] // 一级 + 二级 标签
    status?: string
}

// 商品库存信息
export type Stockpile = {
    id: number
    amount: number
    frozen: number
    productId: number
}

// 获取所有商品
export const getAllProducts = () => {
    return axios.get(`${PRODUCT_MODULE}`)
        .then(res => {
            return res
        })
}

// 获取单个商品详情
export const getProductById = (id: number | string) => {
    return axios.get(`${PRODUCT_MODULE}/${id}`)
        .then(res => {
            return res
        })
}

// 删除商品
export const deleteProductById = (id: number | string) => {
    return axios.delete(`${PRODUCT_MODULE}/${id}`)
        .then(res => {
            return res
        })
}

// 新增商品
export const createProduct = (product: Omit<Product, 'id'>) => {
    return axios.post(`${PRODUCT_MODULE}`, product, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => {
        return res
    })
}

// 更新商品信息
export const updateProduct = (product: Product) => {
    return axios.put(`${PRODUCT_MODULE}`, product, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => {
        return res
    })
}

// 获取商品库存信息
export const getStockByProductId = (productId: number | string) => {
    return axios.get(`${PRODUCT_MODULE}/stockpile/${productId}`)
        .then(res => {
            return res
        })
}

// 修改商品库存
export const updateStockByProductId = (productId: number | string, stock: { amount: number, frozen: number }) => {
    return axios.patch(`${PRODUCT_MODULE}/stockpile/${productId}`, stock, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => {
        return res
    })
}

// 更新商品库存
// 更新商品库存（简化参数）
export const updateProductStock = (data: { id: string | number, stock: number }) => {
    return axios.patch(`${PRODUCT_MODULE}/stockpile/${data.id}`, {
        amount: data.stock,
        frozen: 0, // 默认冻结数量为0
        productId: data.id
    }, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => {
        return res
    })

}

// 设置产品标签（覆盖）
export const updateProductTags = (productId: number | string, tags: [string, string]) => {
    return axios.put(`${PRODUCT_MODULE}/${productId}/tags`, { tags }, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => res)
}

// 标签筛选产品（支持一级 或 一级+二级）
export const filterProductsByTags = (tags: string[]) => {
    const query = tags.map(encodeURIComponent).join(',')
    return axios.get(`${PRODUCT_MODULE}/filter/?tags=${query}`)
        .then(res => res)
}

export const getTopProductsByRecommend = (count: number) => {
    return axios.get(`${PRODUCT_MODULE}/ranking/recommend?count=${count}`)
        .then(res => res)
}

// 获取创建时间榜单（最新上架）
export const getTopProductsByCreatedTime = (count: number) => {
    return axios.get(`${PRODUCT_MODULE}/ranking/latest?count=${count}`)
        .then(res => res)
}

// 获取销量榜单（热销产品）
export const getTopProductsBySales = (count: number) => {
    return axios.get(`${PRODUCT_MODULE}/ranking/sales?count=${count}`)
        .then(res => res)
}

// 修改上下架状态
export const publishProduct = (productId: number | string) => {
    return axios.put(`${PRODUCT_MODULE}/${productId}/publish`, {
        headers: { 'Content-Type': 'application/json' }
    }).then(res => res)
}