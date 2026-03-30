<template>
    <el-dialog :title="isNew ? '添加收货地址' : '编辑收货地址'" v-model="dialogVisible" width="500px" @closed="handleClose"
        :close-on-click-modal="false">
        <el-form :model="addressForm" ref="formRef" :rules="rules" label-width="80px">
            <el-form-item label="收货人" prop="name">
                <el-input v-model="addressForm.name" placeholder="请输入收货人姓名"></el-input>
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
                <el-input v-model="addressForm.phone" placeholder="请输入手机号码"></el-input>
            </el-form-item>
            <el-form-item label="所在地区" prop="region"> <!-- 修改 prop 指向 regionNames -->
                <el-cascader v-model="selectedRegionCodes" :options="regionOptions" placeholder="请选择省/市/区" clearable
                    style="width: 100%;" :props="{ checkStrictly: false }" @change="handleRegionChange"></el-cascader>
            </el-form-item>
            <el-form-item label="详细地址" prop="detailAddress">
                <el-input type="textarea" v-model="addressForm.detailAddress" :rows="3"
                    placeholder="请输入街道、楼牌号等详细信息"></el-input>
            </el-form-item>
        </el-form>
        <template #footer>
            <div class="dialog-footer">
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="submitForm" :loading="isSubmitting"
                    :disabled="!isNew && !isAddressChanged">确认</el-button>
            </div>
        </template>
    </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import type { FormRules, FormInstance, CascaderOption } from 'element-plus'; // 引入 CascaderOption, CascaderProps
import allRegionData from '../assets/districts_zipcodes.json'; // 确保路径正确

// 更新 RecieverInfo 接口
interface RecieverInfo {
    name: string;
    phone: string;
    zipcode: string;
    region: string[]; // 用于存储省市区的名称数组
    detailAddress: string;
}

const originalAddress = ref<RecieverInfo | null>(null);

const props = defineProps({
    visible: {
        type: Boolean,
        default: false
    },
    address: {
        type: Object as () => RecieverInfo | null,
        default: () => ({
            name: '',
            phone: '',
            zipcode: '',
            region: [], // 默认值也应为名称数组
            detailAddress: ''
        })
    }
});

const emit = defineEmits(['update:visible', 'address-updated']);

// 更新表单数据结构
const addressForm = ref<RecieverInfo>({
    name: '',
    phone: '',
    zipcode: '',
    region: [], // 主要存储名称
    detailAddress: ''
});

// 用于 el-cascader v-model 的 ref，存储选中的编码
const selectedRegionCodes = ref<string[]>([]);

const formRef = ref<FormInstance | null>(null);
const isSubmitting = ref(false);

const isNew = computed(() => {
    return !props.address;
});

const dialogVisible = computed({
    get: () => props.visible,
    set: (value) => emit('update:visible', value)
});

const regionOptions = ref<any[]>([]);

onMounted(() => {
    regionOptions.value = allRegionData;
});

// 添加计算属性检查地址是否被修改
const isAddressChanged = computed(() => {
    // 如果是新地址，总是允许保存
    if (isNew.value) return true;

    // 如果没有原始地址数据，不允许保存
    if (!originalAddress.value) return false;

    // 比较当前表单与原始地址信息
    return (
        addressForm.value.name !== originalAddress.value.name ||
        addressForm.value.phone !== originalAddress.value.phone ||
        addressForm.value.zipcode !== originalAddress.value.zipcode ||
        !areArraysEqual(addressForm.value.region, originalAddress.value.region) ||
        addressForm.value.detailAddress !== originalAddress.value.detailAddress
    );
});

// 辅助函数：比较两个数组是否相等
function areArraysEqual(arr1: any[] | undefined, arr2: any[] | undefined): boolean {
    if (!arr1 && !arr2) return true;
    if (!arr1 || !arr2) return false;
    if (arr1.length !== arr2.length) return false;

    for (let i = 0; i < arr1.length; i++) {
        if (arr1[i] !== arr2[i]) return false;
    }

    return true;
}

// 当地址数据变化时更新表单和级联选择器的选中值
watch(() => props.address, (newVal) => {
    if (newVal) {
        // 存储原始地址信息的深拷贝
        originalAddress.value = {
            name: newVal.name || '',
            phone: newVal.phone || '',
            zipcode: newVal.zipcode || '',
            region: [...(newVal.region || [])],
            detailAddress: newVal.detailAddress || ''
        };

        // 更新表单值（保持不变）
        addressForm.value = {
            name: newVal.name || '',
            phone: newVal.phone || '',
            zipcode: newVal.zipcode || '',
            region: newVal.region || [],
            detailAddress: newVal.detailAddress || ''
        };

        // 更新级联选择器的值（保持不变）
        selectedRegionCodes.value = findCodesByNames(newVal.region || [], regionOptions.value);
    } else {
        originalAddress.value = null;
        addressForm.value = {
            name: '',
            phone: '',
            zipcode: '',
            region: [],
            detailAddress: ''
        };
        selectedRegionCodes.value = []; // 清空级联选择器
    }
}, { immediate: true, deep: true });


// 级联选择器 change 事件处理函数
const handleRegionChange = (codes: string[] | undefined) => {
    if (codes && codes.length > 0) {
        // codes 是选中项的 value 数组
        selectedRegionCodes.value = codes; // 更新编码 ref
        // 根据 codes 查找对应的名称
        const names: string[] = [];
        let currentOptions: CascaderOption[] | undefined = regionOptions.value;
        let lastSelectedOption: CascaderOption | undefined = undefined;
        for (const code of codes) {
            const foundOption: CascaderOption | undefined = currentOptions?.find(option => option.value === code);
            if (foundOption) {
                names.push(foundOption.label || '');
                currentOptions = foundOption.children;
                lastSelectedOption = foundOption; // Update lastSelectedOption with the latest found option
            } else {
                break; // 如果中途找不到，停止
            }
        }
        addressForm.value.region = names;
        // 将最后一个选中项的 zipcode 赋值给 addressForm.zipcode
        if (lastSelectedOption && 'zipcode' in lastSelectedOption) {
            addressForm.value.zipcode = (lastSelectedOption as any).zipcode;
        } else {
            // 如果最后一级没有zipcode，或者中途断开，可以选择清空或保留旧值
            addressForm.value.zipcode = ''; // 或者根据需求处理
        }
    } else {
        // 清空处理
        addressForm.value.region = [];
        addressForm.value.zipcode = '',
        selectedRegionCodes.value = [];
    }
    // 手动触发校验，因为我们改变了实际用于校验的 addressForm.regionNames
    formRef.value?.validateField('region');
};

// 辅助函数：根据名称数组查找编码数组 (用于编辑时回显)
function findCodesByNames(names: string[], options: CascaderOption[]): string[] {
    if (!names || names.length === 0 || !options || options.length === 0) {
        return [];
    }
    const codes: string[] = [];
    let currentOptions: CascaderOption[] | undefined = options;
    for (const name of names) {
        const foundOption: CascaderOption | undefined = currentOptions?.find(option => option.label === name);
        if (foundOption) {
            codes.push(foundOption.value as string); // 假设 value 总是 string
            currentOptions = foundOption.children;
        } else {
            return []; // 如果任何一级名称未找到，则无法准确回显
        }
    }
    return codes;
}


const rules: FormRules = {
    name: [
        { required: true, message: '请输入收货人姓名', trigger: 'blur' },
        { min: 2, max: 20, message: '姓名长度应在2-20个字符之间', trigger: 'blur' }
    ],
    phone: [
        { required: true, message: '请输入手机号码', trigger: 'blur' },
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
    ],
    regionNames: [ // 验证规则针对 regionNames
        { required: true, type: 'array', message: '请选择所在地区', trigger: 'change' }, // 确保是数组
        {
            validator: (_rule, value, callback) => {
                // value 现在是名称数组
                if (!value || value.length === 0) {
                    callback(new Error('请选择完整的省市区'));
                } else if (value.length < 3) { // 假设必须选到第三级（区/县）
                    callback(new Error('请选择完整的省市区')); // 根据实际需求取消注释
                }
                callback();
            }, trigger: 'change'
        }
    ],
    detailAddress: [
        { required: true, message: '请输入详细地址', trigger: 'blur' },
        { min: 5, max: 100, message: '详细地址长度应在5-100个字符之间', trigger: 'blur' }
    ]
};

const handleClose = () => {
    formRef.value?.resetFields();
    if (isNew.value) {
        addressForm.value = {
            name: '',
            phone: '',
            zipcode: '',
            region: [],
            detailAddress: ''
        };
        selectedRegionCodes.value = []; // 清空级联选择器
    }
};

const submitForm = async () => {
    if (!formRef.value) return;
    try {
        await formRef.value.validate();
        isSubmitting.value = true;
        setTimeout(() => {
            // 提交时，确保传递的是包含 regionNames 的对象
            const submittedAddress: RecieverInfo = {
                name: addressForm.value.name,
                phone: addressForm.value.phone,
                zipcode: addressForm.value.zipcode,
                region: addressForm.value.region,
                detailAddress: addressForm.value.detailAddress,
            };
            // console.log(submittedAddress)
            emit('address-updated', submittedAddress);
            ElMessage.success('地址保存成功');
            dialogVisible.value = false;
            isSubmitting.value = false;
        }, 300);
    } catch (error) {
        console.error('表单验证失败', error);
        isSubmitting.value = false;
    }
};
</script>

<style scoped>
.dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}
</style>