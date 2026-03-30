<template>
  <el-dialog
      v-model="dialogVisible"
      title="编辑商品信息"
      width="50%"
      @close="resetForm"
  >
    <el-form :model="formData" ref="formRef" :rules="rules" label-width="80px" label-position="right">
      <el-form-item label="商品标题" prop="title">
        <el-input v-model="formData.title" placeholder="请输入商品标题" maxlength="50" show-word-limit />
      </el-form-item>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="价格" prop="price">
            <el-input-number
                v-model="formData.price"
                :min="0"
                :max="10000"
                :precision="2"
                :step="1"
                label="价格"
                placeholder="请输入价格"
                style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="评分" prop="rate">
            <el-input-number
                v-model="formData.rate"
                :min="0"
                :max="10"
                :precision="1"
                :step="0.1"
                label="评分"
                placeholder="请输入评分"
                style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="描述" prop="description">
        <el-input
            v-model="formData.description"
            type="textarea"
            placeholder="请输入商品描述"
            maxlength="200"
            show-word-limit
            :rows="3"
        />
      </el-form-item>
      <el-form-item label="详细说明" prop="detail">
        <el-input
            v-model="formData.detail"
            type="textarea"
            placeholder="请输入商品详细说明"
            maxlength="500"
            show-word-limit
            :rows="5"
        />
      </el-form-item>
      
      <!-- 规格信息编辑部分 -->
      <el-form-item label="规格信息">
        <div class="specifications-container">
          <el-table :data="formData.specifications" style="width: 100%" border>
            <el-table-column label="规格名称" prop="item">
              <template #default="scope">
                <el-input v-model="scope.row.item" placeholder="请输入规格名称" readonly/>
              </template>
            </el-table-column>
            <el-table-column label="规格值" prop="value">
              <template #default="scope">
                <el-input v-model="scope.row.value" placeholder="请输入规格值" />
              </template>
            </el-table-column>
          </el-table>
          
        </div>
      </el-form-item>
      
      <el-form-item label="封面图片" prop="cover">
        <el-upload
            ref="uploadRef"
            class="cover-uploader"
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleChange"
            :before-upload="beforeUpload"
            accept="image/*"
            :limit="1"
        >
          <div v-if="imageUrl" class="image-container">
            <img :src="imageUrl" class="cover-image" />
            <div class="delete-icon" @click.stop="handleRemoveImage">
              <el-icon><Close /></el-icon>
            </div>
          </div>
          <div v-else class="cover-uploader-placeholder">
            <el-icon class="cover-uploader-icon"><Plus /></el-icon>
            <div class="upload-text">点击上传封面图片</div>
          </div>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <div class="form-actions">
          <el-button @click="handleCancel" plain>取消</el-button>
          <el-button @click="handleSubmit" type="primary" :loading="isSubmitting" :disabled="!isFormChanged">保存</el-button>
        </div>
      </el-form-item>
    </el-form>
  </el-dialog>
</template>

<script setup lang="ts">
import {computed, reactive, ref, watch} from 'vue'
import type {FormInstance, FormRules, UploadInstance} from 'element-plus'
import {
  ElButton,
  ElCol,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElInputNumber,
  ElLoading,
  ElMessage,
  ElRow,
  ElTable,
  ElTableColumn,
  ElUpload
} from 'element-plus'
import {Close, Plus} from '@element-plus/icons-vue'
import {updateProduct} from '../api/products'
import {uploadImage} from '../api/image'
import axios from 'axios'

// 类型定义
type Specification = {
  id: number
  item: string
  value: string
  productId: number
}

type Product = {
  id: number
  title: string
  price: number
  rate: number
  description: string
  cover: string
  detail: string
  specifications: Specification[]
}

const props = defineProps<{
  visible: boolean;
  product: Product;
}>();

const emit = defineEmits(['update:visible', 'product-updated'])

// 对话框和UI状态
const dialogVisible = ref(props.visible)
const imageUrl = ref(props.product.cover || '')
const imageFile = ref<File | null>(null)
const formRef = ref<FormInstance | null>(null)
const isSubmitting = ref(false)
const uploadRef = ref<UploadInstance | null>(null)

// 使用reactive管理表单数据
const formData = reactive({
  ...props.product,
  specifications: props.product.specifications?.map(spec => ({ ...spec })) || []
})

// 存储原始数据的副本（只在模态框打开时创建一次）
// 存储原始数据的副本（确保深拷贝规格数组）
const originalFormData = ref({
  ...props.product,
  specifications: props.product.specifications?.map(spec => ({ ...spec })) || []
})

// 计算表单是否有变更
const isFormChanged = computed(() => {
  // 基本字段比较
  if (
    formData.title !== originalFormData.value.title ||
    formData.price !== originalFormData.value.price ||
    formData.rate !== originalFormData.value.rate ||
    formData.description !== originalFormData.value.description ||
    formData.detail !== originalFormData.value.detail ||
    formData.cover !== originalFormData.value.cover
  ) {
    return true;
  }

  // 规格比较
  if (formData.specifications.length !== originalFormData.value.specifications.length) {
    return true;
  }

  // 检查每个规格
  for (let i = 0; i < formData.specifications.length; i++) {
    const curr = formData.specifications[i];
    const orig = originalFormData.value.specifications[i];
    if (!orig || curr.item !== orig.item || curr.value !== orig.value) {
      return true;
    }
  }

  if (imageFile.value !== null) return true

  return false;
})

// 表单验证规则
const rules: FormRules = {
    title: [
      { required: true, message: '请输入商品标题', trigger: 'blur' },
      { min: 2, max: 50, message: '标题长度应在2-50个字符之间', trigger: 'blur' }
    ],
    price: [
      { required: true, message: '请输入价格', trigger: 'blur' },
      { type: 'number', min: 0, message: '价格必须大于等于0', trigger: 'blur' }
    ],
    rate: [
      { required: true, message: '请输入评分', trigger: 'blur' },
      { type: 'number', min: 0, max: 10, message: '评分必须在0-10之间', trigger: 'blur' }
    ],
    description: [
      { required: true, message: '请输入商品描述', trigger: 'blur' },
      { max: 200, message: '描述最多200个字符', trigger: 'blur' }
    ],
    detail: [
      { max: 500, message: '详细说明最多500个字符', trigger: 'blur' }
    ]
}

// 监听 props.visible 的变化
watch(() => props.visible, (newVal) => {
  dialogVisible.value = newVal
  if (newVal) {
    // 当对话框打开时，重置表单数据
    resetForm()
  }
})

// 监听 dialogVisible 的变化，通知父组件
watch(() => dialogVisible.value, (newVal) => {
  if (props.visible !== newVal) {
    emit('update:visible', newVal)
  }
})

const resetForm = () => {
  // 重置表单数据为当前product
  Object.assign(formData, {
    ...props.product,
    specifications: props.product.specifications?.map(spec => ({ ...spec })) || []
  })

  // 同时更新原始数据
  originalFormData.value = {
    ...props.product,
    specifications: props.product.specifications?.map(spec => ({ ...spec })) || []
  }

  // 重置其他状态
  imageUrl.value = props.product.cover || ''
  imageFile.value = null

  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 图片上传前的校验
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt1M = file.size / 1024 / 1024 < 1

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt1M) {
    ElMessage.error('图片大小不能超过 1MB!')
    return false
  }
  return true
}

// 处理文件变更
const handleChange = (file: any) => {
  if (file.raw && beforeUpload(file.raw)) {
    const reader = new FileReader()
    reader.onload = e => {
      if (e.target) {
        imageUrl.value = e.target.result as string
        imageFile.value = file.raw
      }
    }
    reader.readAsDataURL(file.raw)
  }
}

// 删除图片
const handleRemoveImage = () => {
  imageUrl.value = ''
  imageFile.value = null
  formData.cover = ''
  
  // 清除上传组件内部的文件列表
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

// 上传图片，增加重试机制
const uploadImageWithRetry = async (file: File, maxRetries = 3, retryDelay = 1000) => {
  let retries = 0;
  
  const uploadWithRetry = async () => {
    try {
      const response = await uploadImage(file);
      return response;
    } catch (error) {
      if (retries < maxRetries) {
        retries++;
        ElMessage.warning(`上传失败，正在进行第${retries}次重试...`);
        await new Promise(resolve => setTimeout(resolve, retryDelay));
        return uploadWithRetry();
      }
      throw error;
    }
  };
  
  return uploadWithRetry();
};

const handleSubmit = async () => {
  if (!formRef.value || !isFormChanged.value) return;
  
  try {
    // 表单验证
    await formRef.value.validate();
    isSubmitting.value = true;
    
    // 如果有新上传的图片，先上传图片
    if (imageFile.value && imageFile.value instanceof File) {
      const loadingInstance = ElLoading.service({
        fullscreen: true,
        text: '图片上传中...',
        background: 'rgba(0, 0, 0, 0.7)'
      });
      
      try {
        // 使用带重试机制的上传函数
        const response = await uploadImageWithRetry(imageFile.value);
        
        if (response.data && response.data.code === '200') {
          formData.cover = response.data.data
          ElMessage.success('图片上传成功');
        } else {
          loadingInstance.close();
          ElMessage.error(response.data?.msg || '图片上传失败');
          isSubmitting.value = false;
          return;
        }
      } catch (error) {
        loadingInstance.close();
        if (axios.isCancel(error)) {
          ElMessage.warning('上传已取消');
        } else {
          ElMessage.error('网络错误，图片上传失败');
          console.error('图片上传错误:', error);
        }
        isSubmitting.value = false;
        return;
      } finally {
        loadingInstance.close();
      }
    }
    
    // 过滤空规格项
    const filteredSpecs = formData.specifications.filter(
      spec => spec.item.trim() !== '' && spec.value.trim() !== ''
    );

    // 处理规格数据，只提交已修改的
    const modifiedSpecs = filteredSpecs
      .filter(spec => {
        const origSpec = originalFormData.value.specifications.find(o => o.id === spec.id);
        return !origSpec || spec.item.trim() !== origSpec.item || spec.value.trim() !== origSpec.value;
      })
      .map(spec => ({
        id: spec.id,
        item: spec.item.trim(),
        value: spec.value.trim(),
        productId: formData.id
      }));

    // 提交更新数据
    const updatedFormData = {
      ...formData,
      specifications: modifiedSpecs || null

    };

    // console.log('提交的规格数据:', JSON.stringify(updatedFormData.specifications));
    
    // 更新商品信息
    const res = await updateProduct(updatedFormData);
    if (res.data.code === '200') {
      // 更新成功后，同步更新sessionStorage中的商品数据
      try {
        const productsJson = sessionStorage.getItem('products');
        if (productsJson) {
          const products = JSON.parse(productsJson);
          // 找到并更新匹配ID的商品
          const productIndex = products.findIndex((p: any) => p.id === updatedFormData.id);
          if (productIndex !== -1) {
            // 创建更新后的商品对象，包含所有规格（不只是修改的）
            // 更新商品列表中的商品
            products[productIndex] = {
              ...updatedFormData,
              // 合并现有规格和修改的规格
              specifications: filteredSpecs.map(spec => ({
                id: spec.id,
                item: spec.item.trim(),
                value: spec.value.trim(),
                productId: updatedFormData.id
              }))
            };
            // 保存回sessionStorage
            sessionStorage.setItem('products', JSON.stringify(products));
            // console.log('已同步更新sessionStorage中的商品数据');
          }
        }
      } catch (storageError) {
        console.error('更新sessionStorage失败:', storageError);
        // 不影响主流程，只记录错误
      }
      emit('product-updated', updatedFormData);
      emit('update:visible', false);
      ElMessage.success('商品信息更新成功');
    } else {
      console.error('更新失败响应:', res.data);
      ElMessage.error('更新失败：' + res.data.msg);
    }
  } catch (error) {
    console.error('更新失败', error);
    ElMessage.error('更新失败，请稍后再试');
  } finally {
    isSubmitting.value = false;
  }
}

const handleCancel = () => {
  dialogVisible.value = false
}
</script>

<style scoped>
.el-dialog__header {
  background-color: #f39c12;
  color: white;
  padding: 15px 20px;
  border-radius: 4px 4px 0 0;
}

.cover-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 178px;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.3s;
}

.cover-uploader:hover {
  border-color: #409eff;
}

.cover-uploader-placeholder {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
}

.cover-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 12px;
  color: #8c939d;
}

.cover-image {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

.image-container {
  position: relative;
  width: 178px;
  height: 178px;
}

.delete-icon {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 22px;
  height: 22px;
  background-color: #f56c6c;
  color: white;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  z-index: 10;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
  transition: transform 0.2s;
}

.delete-icon:hover {
  transform: scale(1.1);
}

.image-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}

:deep(.el-dialog__body) {
  padding: 20px 30px;
}

:deep(.el-input-number.is-controls-right .el-input__wrapper) {
  padding-right: 0;
}

.specifications-container {
  width: 100%;
}

:deep(.el-table .el-input__wrapper) {
  padding: 0 11px;
}

:deep(.el-table .el-input__inner) {
  height: 30px;
}
</style>

