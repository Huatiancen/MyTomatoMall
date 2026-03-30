<template>
  <el-dialog v-model="dialogVisible" title="编辑广告" width="50%" :close-on-click-modal="false" @close="handleClose">
    <el-form :model="formData" ref="formRef" :rules="rules" label-width="100px" label-position="right">
      <el-form-item label="广告标题" prop="title">
        <el-input v-model="formData.title" placeholder="请输入广告标题" maxlength="50" show-word-limit />
      </el-form-item>

      <el-form-item label="广告内容" prop="content">
        <el-input v-model="formData.content" type="textarea" placeholder="请输入广告内容" maxlength="200" show-word-limit
          :rows="3" />
      </el-form-item>

      <el-form-item label="关联产品" prop="productId">
        <el-select v-model="formData.productId" placeholder="请选择或搜索关联产品" filterable remote
          :remote-method="filterProducts" :loading="productsLoading" style="width: 100%" clearable>
          <el-option v-for="item in filteredProducts" :key="item.id" :label="`${item.id} - ${item.title}`"
            :value="item.id">
            <div class="product-option">
              <span class="product-id">ID: {{ item.id }}</span>
              <span class="product-name">{{ item.title }}</span>
            </div>
          </el-option>
        </el-select>

        <div class="selected-product-info" v-if="selectedProductInfo">
          <div class="info-label">已选商品:</div>
          <div class="info-content">
            <span class="product-id">ID: {{ selectedProductInfo.id }}</span>
            <span class="product-title">{{ selectedProductInfo.title }}</span>
          </div>
        </div>

        <div class="form-tip">提示：选择产品可让广告链接到对应产品页面</div>
      </el-form-item>

      <el-form-item label="广告图片" prop="imgUrl">
        <el-upload ref="uploadRef" class="cover-uploader" action="#" :auto-upload="false" :show-file-list="false"
          :on-change="handleChange" :before-upload="beforeUpload" accept="image/*" :limit="1">
          <div v-if="imageUrl" class="image-container">
            <img :src="imageUrl" class="cover-image" />
            <div class="delete-icon" @click.stop="handleRemoveImage">
              <el-icon>
                <Close />
              </el-icon>
            </div>
          </div>
          <div v-else class="cover-uploader-placeholder">
            <el-icon class="cover-uploader-icon">
              <Plus />
            </el-icon>
            <div class="upload-text">点击上传广告图片</div>
          </div>
        </el-upload>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="isSubmitting" :disabled="!formChanged">保存</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import type { FormRules, UploadInstance, FormInstance } from 'element-plus'
import { Plus, Close } from '@element-plus/icons-vue'
import { updateAdvertisement } from '../api/advertisement'
import { uploadImage } from '../api/image'
import axios from 'axios'

// 定义props和事件
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  advertisement: {
    type: Object,
    default: () => ({
      id: null,
      title: '',
      content: '',
      imgUrl: '',
      productId: null
    })
  }
})

const emit = defineEmits(['update:visible', 'updated', 'close'])

// 计算属性用于双向绑定对话框可见状态
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// 初始化表单数据
const formData = reactive({
  id: null as number | null,
  title: '',
  content: '',
  imgUrl: '',
  productId: null as number | null
})

// UI状态
const imageUrl = ref('')
const imageFile = ref<File | null>(null)
const formRef = ref<FormInstance | null>(null)
const isSubmitting = ref(false)
const uploadRef = ref<UploadInstance | null>(null)

// 表单验证规则
const rules: FormRules = {
  title: [
    { required: true, message: '请输入广告标题', trigger: 'blur' },
    { min: 2, max: 50, message: '标题长度应在2-50个字符之间', trigger: 'blur' }
  ],
  content: [
    { max: 200, message: '内容最多200个字符', trigger: 'blur' }
  ],
  productId: [
    { required: true, message: '请输入关联商品ID', trigger: 'blur' }
  ],
  imgUrl: [
    { required: true, message: '请上传广告图片', trigger: 'change' }
  ]
}

// 添加商品相关的响应式数据
const products = ref<any[]>([])
const filteredProducts = ref<any[]>([])
const productsLoading = ref(false)
const selectedProductInfo = ref<any>(null)

// 添加在其他ref声明附近
const originalData = ref({
  id: null,
  title: '',
  content: '',
  imgUrl: '',
  productId: null
});

// 从sessionStorage获取产品数据
const loadProducts = () => {
  try {
    const productsStr = sessionStorage.getItem('products')
    if (productsStr) {
      products.value = JSON.parse(productsStr)
      filteredProducts.value = [...products.value].slice(0, 20) // 最初只显示前20条

      // 如果已有关联的商品ID，设置选中的商品信息
      if (formData.productId) {
        const product = products.value.find(p => p.id === formData.productId)
        if (product) {
          selectedProductInfo.value = {
            id: product.id,
            title: product.title
          }
        }
      }
    } else {
      console.warn('未找到产品数据，请确保已加载产品列表')
    }
  } catch (error) {
    console.error('解析产品数据失败:', error)
  }
}

// 根据输入筛选产品
const filterProducts = (query: string) => {
  if (!query) {
    filteredProducts.value = [...products.value].slice(0, 20)
    return
  }

  productsLoading.value = true

  setTimeout(() => {
    filteredProducts.value = products.value
      .filter(item => {
        // 匹配ID
        if (item.id.toString().includes(query)) return true

        // 匹配标题
        if (item.title && item.title.toLowerCase().includes(query.toLowerCase())) return true

        return false
      })
      .slice(0, 20) // 限制结果数量，避免过多导致性能问题
    productsLoading.value = false
  }, 200) // 添加短暂延迟，避免频繁筛选
}

// 加载广告数据到表单
const loadAdvertisementData = () => {
  const adData = props.advertisement
  formData.id = adData.id
  formData.title = adData.title || ''
  formData.content = adData.content || ''
  formData.imgUrl = adData.imgUrl || ''
  formData.productId = adData.productId || null

  // 保存原始数据的副本，用于后续比较
  originalData.value = {
    id: adData.id,
    title: adData.title || '',
    content: adData.content || '',
    imgUrl: adData.imgUrl || '',
    productId: adData.productId || null
  };
  
  // 设置图片预览
  if (adData.imgUrl) {
    imageUrl.value = adData.imgUrl
  }
  // 加载商品数据
  loadProducts()

  // 如果有关联产品ID，查找并设置选中的商品信息
  if (formData.productId) {
    handleProductChange(formData.productId)
  }
}

// 处理商品选择变化
const handleProductChange = (productId: number | null) => {
  if (productId) {
    const product = products.value.find(p => p.id === productId)
    if (product) {
      selectedProductInfo.value = {
        id: product.id,
        title: product.title || ''
      }
    }
  } else {
    selectedProductInfo.value = null
  }
}

// 监听productId变化
watch(() => formData.productId, (newValue) => {
  handleProductChange(newValue)
})

// 处理关闭
const handleClose = () => {
  emit('close')
  resetForm()
}

// 重置表单
const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  imageUrl.value = ''
  imageFile.value = null
  formData.id = null
  formData.title = ''
  formData.content = ''
  formData.imgUrl = ''
  formData.productId = null
  
  // 清除上传组件内部的文件列表
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  selectedProductInfo.value = null
}

// 添加一个计算属性，检查表单是否有变动
const formChanged = computed(() => {
  // 检查基本字段是否有变化
  if (
    formData.title !== originalData.value.title ||
    formData.content !== originalData.value.content ||
    formData.productId !== originalData.value.productId
  ) {
    return true;
  }

  // 检查图片是否有变化
  if (imageFile.value) {
    // 有新上传的图片文件
    return true;
  }

  // 处理删除图片的情况
  if (!formData.imgUrl && originalData.value.imgUrl) {
    return true;
  }

  // 检查图片URL是否变化（不是数据URL格式的情况）
  if (
    formData.imgUrl &&
    originalData.value.imgUrl &&
    formData.imgUrl !== originalData.value.imgUrl &&
    !formData.imgUrl.startsWith('data:')
  ) {
    return true;
  }

  return false;
});

// 图片上传前的校验
const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
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
        formData.imgUrl = imageUrl.value
        // 标记已有图片，清除imgUrl字段的验证错误
        if (formRef.value) {
          formRef.value.clearValidate('imgUrl')
        }
      }
    }
    reader.readAsDataURL(file.raw)
  }
}

// 组件挂载时初始化
onMounted(() => {
  if (dialogVisible.value) {
    loadAdvertisementData()
  }
})

// 删除图片
const handleRemoveImage = () => {
  imageUrl.value = ''
  imageFile.value = null
  formData.imgUrl = ''
  
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
  if (!formRef.value) return;
  
  try {
    // 表单验证
    await formRef.value.validate();
    isSubmitting.value = true;
    
    // 如果有上传新图片但formData.imgUrl为DataURL格式，则需要上传图片
    if (imageFile.value && formData.imgUrl.startsWith('data:')) {
      const loadingInstance = ElLoading.service({
        fullscreen: true,
        text: '图片上传中...',
        background: 'rgba(0, 0, 0, 0.7)'
      });
      
      try {
        // 使用带重试机制的上传函数
        const response = await uploadImageWithRetry(imageFile.value);
        
        if (response.data && response.data.code === '200') {
          formData.imgUrl = response.data.data
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
    
    // 确保数据包含ID
    if (!formData.id) {
      ElMessage.error('缺少广告ID，无法更新');
      isSubmitting.value = false;
      return;
    }
    
    // 确保productId是数字类型
    const adData = {
      ...formData,
      productId: formData.productId || 0 // 确保productId始终是数字
    };
    
    // 调用API更新广告
    const res = await updateAdvertisement(adData);
    if (res.code.toString() === '200') {
      ElMessage.success('广告更新成功');
      // 通知父组件广告已成功更新
      emit('updated', formData);
      // 关闭对话框
      dialogVisible.value = false;
    } else {
      console.error('更新失败响应:', res);
      ElMessage.error('更新失败：' + res.message);
    }
  } catch (error) {
    console.error('更新广告失败', error);
    ElMessage.error('更新失败，请稍后再试');
  } finally {
    isSubmitting.value = false;
  }
}

// 监听对话框打开状态，当打开时加载广告数据
watch(() => props.visible, (newValue) => {
  if (newValue) {
    loadAdvertisementData();
  }
});

// 监听广告数据变化
watch(() => props.advertisement, () => {
  if (dialogVisible.value) {
    loadAdvertisementData();
  }
}, { deep: true });
</script>

<style scoped>
.product-option {
  display: flex;
  align-items: center;
  width: 100%;
}

.product-id {
  width: 60px;
  color: #409EFF;
  font-weight: bold;
}

.product-name {
  flex: 1;
  margin: 0 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #606266;
}

.selected-product-info {
  margin-top: 8px;
  padding: 8px 12px;
  background-color: #f2f6fc;
  border-radius: 4px;
  display: flex;
  align-items: center;
}

.info-label {
  font-size: 13px;
  color: #606266;
  margin-right: 8px;
  font-weight: bold;
}

.info-content {
  display: flex;
  flex: 1;
}

.product-title {
  color: #606266;
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cover-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 300px;
  height: 150px;
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
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.image-container {
  position: relative;
  width: 300px;
  height: 150px;
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

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.4;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
