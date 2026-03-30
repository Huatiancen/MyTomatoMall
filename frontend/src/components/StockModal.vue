<template>
  <el-dialog
      v-model="dialogVisible"
      title="修改商品库存"
      width="40%"
      @close="resetForm"
      class="stock-modal"
  >
    <el-form :model="formData" ref="formRef" :rules="rules" label-width="100px" label-position="right">
      <el-form-item label="商品ID">
        <el-input v-model="formData.id" disabled />
      </el-form-item>

      <el-form-item label="当前库存" prop="stock">
        <el-input-number
            v-model="formData.stock"
            :min="0"
            :max="9999"
            :precision="0"
            :step="1"
            label="库存"
            placeholder="请输入库存数量"
            style="width: 100%"
        />
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
import { ref, watch, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormRules, FormInstance } from 'element-plus'
import { getStockByProductId, updateProductStock } from '../api/products'

// 类型定义
type Product = {
  id: number
  title: string
  stock?: number
  [key: string]: any
}

const props = defineProps<{
  visible: boolean;
  product: Product;
}>();

const emit = defineEmits(['update:visible', 'stock-updated'])

// 对话框和UI状态
const dialogVisible = ref(props.visible)
const formRef = ref<FormInstance | null>(null)
const isSubmitting = ref(false)

// 表单数据和原始数据
const formData = reactive({
  id: props.product.id,
  stock: props.product.stock || 0
})

const originalStock = ref(props.product.stock || 0)
let stockData: { amount: number, frozen?: number } | null = null

// 计算表单是否有变更
const isFormChanged = computed(() => formData.stock !== originalStock.value)

// 表单验证规则
const rules: FormRules = {
  stock: [
    { required: true, message: '请输入库存数量', trigger: 'blur' },
    { type: 'number', min: 0, message: '库存数量必须大于等于0', trigger: 'blur' }
  ]
}

// 监听 visible 属性变化
watch(() => props.visible, (newVal) => {
  dialogVisible.value = newVal
  if (newVal) resetForm()
})

// 监听对话框状态变化，同步更新父组件
watch(() => dialogVisible.value, (newVal) => {
  if (props.visible !== newVal) {
    emit('update:visible', newVal)
  }
})


// 重置表单数据
const resetForm = async () => {
  try {
      const res = await getStockByProductId(props.product.id)
      if (res.data.code === '200') {
        stockData = res.data.data
      } else {
        ElMessage.error('获取商品库存信息失败：' + res.data.msg)
        return
      }

    // 更新表单数据
    formData.id = props.product.id
    if (stockData) {
      formData.stock = stockData.amount
      originalStock.value = stockData.amount
    }
    else {
      formData.stock = 0
      originalStock.value = 0
    }

    if (formRef.value) {
      formRef.value.clearValidate()
    }
  } catch (error) {
    console.error('获取商品库存信息失败', error)
    ElMessage.error('获取商品库存信息失败，请稍后再试')
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value || !isFormChanged.value) return

  try {
    // 表单验证
    await formRef.value.validate()
    isSubmitting.value = true

    // 更新商品库存
    const res = await updateProductStock({
      id: formData.id,
      stock: formData.stock
    })

    // console.log(res)

    if (res.data.code === '200') {

      // 通知父组件
      emit('stock-updated', {
        id: formData.id,
        stock: formData.stock
      })
      emit('update:visible', false)
      ElMessage.success('商品库存更新成功')
    } else {
      ElMessage.error('更新失败：' + res.data.msg)
    }
  } catch (error) {
    // console.error('更新失败', error)
    ElMessage.error('更新失败，请稍后再试')
  } finally {
    isSubmitting.value = false
  }
}

const handleCancel = () => {
  dialogVisible.value = false
}
</script>

<style scoped>
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
}

.stock-modal {
  display: flex;
  height: 100vh;
}
</style>
