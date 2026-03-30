import { axios } from '../utils/request.ts'
import { IMAGE_MODULE } from './_prefix.ts'

export const uploadImage = (image: File) => {
    const formData = new FormData()
    formData.append('file', image)
    return axios.post(`${IMAGE_MODULE}/upload`, formData, {
        headers: { 'Content-Type': 'multipart/form-data'}
    }).then(res => {
        return res
    })
}