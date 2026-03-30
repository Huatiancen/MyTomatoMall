<template>
  <div class="add-product-container">
    <div class="page-header">
      <h2>添加新商品</h2>
      <el-button @click="goBack" plain :icon="ArrowLeft">返回</el-button>
    </div>
    
    <el-card class="form-card">
      <el-form :model="formData" ref="formRef" :rules="rules" label-width="100px" label-position="right">
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
                  <el-input v-model="scope.row.item" placeholder="请输入规格名称" />
                </template>
              </el-table-column>
              <el-table-column label="规格值" prop="value">
                <template #default="scope">
                  <el-input v-model="scope.row.value" placeholder="请输入规格值" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120">
                <template #default="scope">
                  <el-button 
                    type="danger" 
                    size="small" 
                    @click="removeSpecification(scope.$index)" 
                    v-if="scope.$index >= 7"
                  >删除</el-button>
                  <span v-else class="preset-tag">预设项</span>
                </template>
              </el-table-column>
            </el-table>
            <div class="add-specification">
              <el-button type="primary" plain size="small" @click="addSpecification">添加规格</el-button>
            </div>
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
            <el-button @click="goBack" plain>取消</el-button>
            <el-button @click="handleSubmit" type="primary" :loading="isSubmitting">创建</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElLoading } from 'element-plus'
import type { FormRules, UploadInstance, FormInstance } from 'element-plus'
import { Plus, Close, ArrowLeft } from '@element-plus/icons-vue'
import { createProduct } from '../../api/products'
import { uploadImage } from '../../api/image'
import axios from 'axios'

// 路由
const router = useRouter()

// 初始化空表单数据
const formData = reactive({
  title: '',
  price: 0,
  rate: 0,
  description: '',
  cover: '',
  detail: '',
  specifications: [
    { item: '作者', value: '' },
    { item: '副标题', value: '' },
    { item: 'ISBN', value: '' },
    { item: '帧装', value: '' },
    { item: '页数', value: '' },
    { item: '出版社', value: '' },
    { item: '出版日期', value: '' }
  ] as Array<{ item: string, value: string }>
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
    { message: '请输入商品描述', trigger: 'blur' },
    { max: 200, message: '描述最多200个字符', trigger: 'blur' }
  ],
  detail: [
    { max: 500, message: '详细说明最多500个字符', trigger: 'blur' }
  ],
  cover: [
    { message: '请上传封面图片', trigger: 'change' }
  ]
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 添加规格
const addSpecification = () => {
  formData.specifications.push({ item: '', value: '' })
}

// 删除规格
const removeSpecification = (index: number) => {
  // 预设的六个规格项不允许删除
  if (index < 6) {
    ElMessage.warning('预设规格项不能删除')
    return
  }
  formData.specifications.splice(index, 1)
}

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
        formData.cover = imageUrl.value
        // 标记已有图片，清除cover字段的验证错误
        if (formRef.value) {
          formRef.value.clearValidate('cover')
        }
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
  if (!formRef.value) return;
  
  try {
    // 表单验证
    await formRef.value.validate();
    isSubmitting.value = true;
    
    // 如果有上传图片但formData.cover为空，上传图片
    if (imageFile.value) {
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
      spec => spec.item.trim() !== ''
    );
    
    // 创建提交的数据对象
    const submitData = {
      ...formData,
      specifications: filteredSpecs.map(spec => ({
        item: spec.item.trim(),
        value: spec.value.trim(),
      }))
    };
    
    // 调用API创建商品
    const res = await createProduct(submitData);
    if (res.data.code === '200') {
      // 创建成功后，添加到sessionStorage
      try {
        const productsJson = sessionStorage.getItem('products');
        if (productsJson) {
          const products = JSON.parse(productsJson);
          // 添加新创建的商品
          const newProduct = {
            id: res.data.data.id,
            title: res.data.data.title,
            price: res.data.data.price,
            cover: res.data.data.cover,
            specifications: res.data.data.specifications || [],
            tags: ['', ''],
            status: 'ACTIVE'
          };
          products.push(newProduct);
          // 保存回sessionStorage
          sessionStorage.setItem('products', JSON.stringify(products));
          // console.log('已同步更新sessionStorage中的商品数据');
        }
      } catch (storageError) {
        console.error('更新sessionStorage失败:', storageError);
        // 不影响主流程，只记录错误
      }
      
      ElMessage.success('商品创建成功');
      // 重置表单或跳转到商品列表
      router.replace({path: '/displayBoard'});
    } else {
      console.error('创建失败响应:', res.data);
      ElMessage.error('创建失败：' + res.data.msg);
    }
  } catch (error) {
    console.error('创建失败', error);
    ElMessage.error('创建失败，请稍后再试');
  } finally {
    isSubmitting.value = false;
  }
}
</script>

<style scoped>
.add-product-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.form-card {
  background-color: #fff;
  border-radius: 4px;
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
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

.add-specification {
  margin-top: 10px;
  display: flex;
  justify-content: center;
}

.preset-tag {
  color: #909399;
  font-size: 12px;
  padding: 2px 6px;
  background-color: #f0f0f0;
  border-radius: 4px;
}
</style>
