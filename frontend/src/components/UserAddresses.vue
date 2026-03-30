<template>
    <div class="address-page">
        <!-- 头部 -->
        <div class="page-header">
            <div class="back-icon" @click="goBack">
                <i class="el-icon-arrow-left"></i>
            </div>
            <div class="page-title">收货地址</div>
        </div>

        <!-- 地址列表 -->
        <div class="address-list">
            <div v-for="(address, index) in addresses" :key="index" class="address-card">
                <!-- 收件人信息 -->
                <div class="address-header">
                    <div class="recipient-info">
                        <span class="recipient-name">{{ address.name }}</span>
                        <span class="recipient-phone">{{ address.phone }}</span>
                        <span v-if="address.isDefault === true" class="address-tag 默认">默认</span>
                    </div>
                </div>

                <!-- 地址详情 -->
                <div class="address-detail">
                    {{ address.address }}
                </div>

                <!-- 操作区域 -->
                <div class="address-actions">
                    <div class="default-action">
                        <el-checkbox v-model="address.isDefault" @change="setAsDefault(index)">
                            <span class="default-text">{{ address.isDefault ? '已' : '设为' }}默认</span>
                        </el-checkbox>
                    </div>
                    <div class="action-buttons">
                        <el-button size="small" text @click="deleteAddressForUser(index)">删除</el-button>
                        <el-button size="small" text @click="editAddress(index)">修改</el-button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 新增地址按钮 -->
        <div class="add-address-btn">
            <el-button type="danger" @click="addNewAddress">新增收货地址</el-button>
        </div>

        <!-- 地址编辑弹窗 (集成已有的 AddressModal 组件) -->
        <address-modal v-model:visible="addressModalVisible" :address="currentEditAddress"
            @address-updated="handleAddressUpdated" />
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus';
import { getAddresses, createAddress, updateAddress, deleteAddress } from '../api/address';

interface CreateInfo {
    id?: number;
    name: string;
    phone: string;
    zipcode: string;
    address: string;
    isDefault?: boolean;  //需要添加这个字段判断是否是默认地址
}

interface UpdateInfo {
    shoppingAddressId: number;
    name?: string;
    phone?: string;
    zipcode?: string;
    address?: string;
    isDefault?: boolean;  //需要添加这个字段判断是否是默认地址
}

// 更新 RecieverInfo 接口
interface RecieverInfo {
    name: string;
    phone: string;
    zipcode: string;
    region: string[]; // 用于存储省市区的编码数组
    detailAddress: string; // 详细街道地址
}

// 地址弹窗控制
const addressModalVisible = ref(false);
const currentEditAddress = ref<RecieverInfo | null>(null);
const addresses = ref<CreateInfo[]>([]);
const defaultIndex = ref(-1);
const isAddNewAddress = ref(false);
const editAddressId = ref(-1);

// 返回上一页
const goBack = () => {
    window.history.back();
};

// 设置默认地址
const setAsDefault = async (index: number) => {
    try {

        const currentAddress = addresses.value[index];
        let updateSuccessful = false;

        // 情况1: 没有默认地址，将当前地址设为默认
        if (defaultIndex.value === -1) {
            // 调用API设置默认地址
            const updateInfo: UpdateInfo = {
                shoppingAddressId: currentAddress.id!,
                isDefault: true
            };

            const response = await updateAddress(updateInfo);
            if (response.data.code === '200') {
                // 更新前端状态
                currentAddress.isDefault = true;
                defaultIndex.value = index;
                updateSuccessful = true;
                ElMessage.success('已设置为默认地址');
            }
        }
        // 情况2: 点击的是已经是默认地址，取消默认状态
        else if (index === defaultIndex.value) {
            // 调用API取消默认地址
            const updateInfo: UpdateInfo = {
                shoppingAddressId: currentAddress.id!,
                isDefault: false
            };

            const response = await updateAddress(updateInfo);
            if (response.data.code === '200') {
                // 更新前端状态
                currentAddress.isDefault = false;
                defaultIndex.value = -1;
                updateSuccessful = true;
                ElMessage.success('已取消默认地址');
            }
        }
        // 情况3: 点击非默认地址，且已有其他默认地址
        else if (index !== defaultIndex.value && defaultIndex.value !== -1) {
            // 先获取现有默认地址
            const oldDefaultAddress = addresses.value[defaultIndex.value];

            // 调用API设置新的默认地址
            const updateInfo: UpdateInfo = {
                shoppingAddressId: currentAddress.id!,
                isDefault: true
            };

            const response = await updateAddress(updateInfo);
            if (response.data.code === '200') {
                // 同时更新旧默认地址为非默认
                const updateOldInfo: UpdateInfo = {
                    shoppingAddressId: oldDefaultAddress.id!,
                    isDefault: false
                };

                const responseOld = await updateAddress(updateOldInfo);
                if (responseOld.data.code === '200') {
                    // 更新前端状态
                    oldDefaultAddress.isDefault = false;
                    currentAddress.isDefault = true;
                    defaultIndex.value = index;
                    updateSuccessful = true;
                    ElMessage.success('已更换默认地址');
                }
            }
        }

        // 更新成功后同步到本地存储
        if (updateSuccessful) {
            sessionStorage.setItem('userAddresses', JSON.stringify(addresses.value));
        }

    } catch (error) {
        console.error('设置默认地址时出错:', error);
        ElMessage.error('操作失败，请稍后再试');
    }
};

// 删除地址
const deleteAddressForUser = (index: number) => {
    // 确保地址有ID
    const addressId = addresses.value[index].id;
    if (!addressId) {
        ElMessage.error('无法删除没有ID的地址');
        return;
    }

    ElMessageBox.confirm('确定删除该收货地址吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(async () => {
        try {
            // 显示加载提示
            const loading = ElLoading.service({
                lock: true,
                text: '删除地址中...',
                background: 'rgba(255, 255, 255, 0.7)'
            });

            // 调用API删除地址
            const response = await deleteAddress(addressId);
            loading.close();
            // 检查API响应
            if (response.data.code === '200') {
                // 成功后从前端数组中移除
                addresses.value.splice(index, 1);
                sessionStorage.setItem('userAddresses', JSON.stringify(addresses.value));
                ElMessage.success('删除成功');
            } else {
                // 处理API错误
                ElMessage.error(response.data?.message || '删除失败，请稍后重试');
            }
        } catch (error) {
            console.error('删除地址时出错:', error);
            ElMessage.error('删除地址时出现错误，请稍后重试');
        }
    }).catch(() => {
        // 用户取消删除
        ElMessage.info('已取消删除');
    });
};

// 编辑地址
const editAddress = (index: number) => {
    // 获取原始地址对象
    const addressObj = addresses.value[index];

    // 分割地址字符串
    const addressParts = addressObj.address.split(' ').filter(part => part.trim() !== '');

    let region: string[] = [];
    let detailAddress: string = '';

    // 检查是否有足够的部分来分配给region
    if (addressParts.length >= 3) {
        // 提取region（前3部分）
        region = addressParts.slice(0, 3);
        // 提取详细地址（剩余部分）
        detailAddress = addressParts.slice(3).join(' ');
    } else {
        // 如果部分少于3，将所有部分放入region
        region = [...addressParts];
        // 确保region至少有3个元素
        while (region.length < 3) {
            region.push('');
        }
        detailAddress = '';
    }
    // 创建编辑地址对象
    currentEditAddress.value = {
        name: addressObj.name,
        phone: addressObj.phone,
        zipcode: addressObj.zipcode || '',
        region: region,
        detailAddress: detailAddress
    };

    addressModalVisible.value = true;
    isAddNewAddress.value = false;
    editAddressId.value = index;
};

// 新增地址
const addNewAddress = () => {
    currentEditAddress.value = {
        name: '',
        phone: '',
        zipcode: '',
        region: [],
        detailAddress: ''
    };
    addressModalVisible.value = true;
    isAddNewAddress.value = true;
};

// 处理地址更新
const handleAddressUpdated = async (updatedAddress: RecieverInfo) => {
    try {
        // 显示加载提示
        const loading = ElLoading.service({
            lock: true,
            text: '保存地址中...',
            background: 'rgba(255, 255, 255, 0.7)'
        });

        // 如果是编辑现有地址
        if (isAddNewAddress.value === false) {
            const originalAddress = addresses.value[editAddressId.value];
            const addressId = originalAddress.id;

            if (!addressId) {
                ElMessage.error('无法更新没有ID的地址');
                loading.close();
                return;
            }

            // 创建一个只包含已修改字段的更新对象
            const updateInfo: UpdateInfo = {
                shoppingAddressId: addressId
            };

            // 比较每个字段，只添加已修改的字段
            if (originalAddress.name !== updatedAddress.name) {
                updateInfo.name = updatedAddress.name;
            }

            if (originalAddress.phone !== updatedAddress.phone) {
                updateInfo.phone = updatedAddress.phone;
            }

            if (originalAddress.zipcode !== updatedAddress.zipcode) {
                updateInfo.zipcode = updatedAddress.zipcode;
            }

            // 合并region和detailAddress为address，然后比较
            const newAddress = updatedAddress.region.join(' ') + ' ' + updatedAddress.detailAddress;
            if (originalAddress.address !== newAddress) {
                updateInfo.address = newAddress;
            }

            // 如果有变化的字段，则调用更新API
            if (Object.keys(updateInfo).length > 1) { // 大于1是因为id字段总是包含的
                // 调用API更新地址
                const response = await updateAddress(updateInfo);

                // 处理响应
                if (response.data.code === '200') {
                    // 更新本地地址数据
                    addresses.value[editAddressId.value] = {
                        ...originalAddress,
                        name: updatedAddress.name,
                        phone: updatedAddress.phone,
                        zipcode: updatedAddress.zipcode,
                        address: newAddress
                    };

                    // 更新会话存储
                    sessionStorage.setItem('userAddresses', JSON.stringify(addresses.value));
                    ElMessage.success('地址更新成功');
                } else {
                    ElMessage.error(response.data?.message || '地址更新失败，请稍后重试');
                }
            } else {
                // 如果没有变化，显示提示信息
                ElMessage.info('地址信息未发生变化');
            }

            // 重置编辑状态
            editAddressId.value = -1;
        } else {
            // 如果是新增地址
            // 构造要提交的地址对象
            const createInfo: CreateInfo = {
                name: updatedAddress.name,
                phone: updatedAddress.phone,
                zipcode: updatedAddress.zipcode,
                address: updatedAddress.region.join(' ') + ' ' + updatedAddress.detailAddress,
            };

            // 调用API创建地址
            const response = await createAddress(createInfo);

            // 处理响应
            if (response.data.code === '200') {
                // 添加服务器返回的ID
                const newAddress = {
                    ...createInfo,
                    id: response.data.data.shoppingAddressId,
                    isDefault: false
                };

                // 添加到地址列表
                addresses.value.push(newAddress);

                // 如果是默认地址，更新其他地址
                if (newAddress.isDefault) {
                    setAsDefault(addresses.value.length - 1);
                }

                // 更新会话存储
                sessionStorage.setItem('userAddresses', JSON.stringify(addresses.value));
                ElMessage.success('地址添加成功');
            } else {
                ElMessage.error(response.data?.message || '地址添加失败，请稍后重试');
            }
        }
        isAddNewAddress.value = false;

    } catch (error) {
        console.error('保存地址时出错:', error);
        ElMessage.error('保存地址时出现错误，请稍后重试');
    } finally {
        if (ElLoading) {
            ElLoading.service().close();
        }
    }
};

// 加载地址数据
const loadAddresses = async () => {
    // 显示加载提示
    const loading = ElLoading.service({
        lock: true,
        text: '加载地址信息...',
        background: 'rgba(255, 255, 255, 0.7)'
    });

    try {
        // 从后端API获取地址列表
        const response = await getAddresses();

        if (response.data && response.data.code === '200' && response.data.data) {
            // 处理API返回的地址数据
            const fetchedAddresses = response.data.data.map((addr: any) => ({
                id: addr.shoppingAddressId,
                name: addr.name,
                phone: addr.phone,
                zipcode: addr.zipcode || '',
                address: addr.address || '',
                isDefault: addr.isDefault || false
            }));

            // 更新地址列表
            addresses.value = fetchedAddresses;

            // 更新本地存储
            sessionStorage.setItem('userAddresses', JSON.stringify(addresses.value));
            // console.log('已从后端加载地址数据:', addresses.value);

            // 找出默认地址索引
            defaultIndex.value = addresses.value.findIndex(addr => addr.isDefault === true);
        } else {
            console.warn('从后端获取地址失败', response);
            ElMessage.warning('获取地址数据失败，请稍后再试');
        }
    } catch (error) {
        console.error('获取地址时出错:', error);
        ElMessage.error('加载地址数据时出现错误，请稍后再试');
    } finally {
        loading.close();
    }
};

// 组件挂载时加载地址
onMounted(() => {
    // 尝试从sessionStorage获取
    const storedAddresses = sessionStorage.getItem('userAddresses');

    if (storedAddresses) {
        try {
            // 解析并使用缓存数据
            addresses.value = JSON.parse(storedAddresses);
            // console.log('已从本地缓存加载地址:', addresses.value);
            // 找出默认地址
            defaultIndex.value = addresses.value.findIndex(addr => addr.isDefault === true);
        } catch (e) {
            console.error('解析缓存地址数据失败:', e);
            // 如果解析失败，从服务器获取
            loadAddresses();
        }
    } else {
        // 没有缓存数据，直接从服务器获取
        loadAddresses();
    }
});
</script>

<style scoped>
.address-page {
    display: flex;
    flex-direction: column;
    height: 100%;
    background-color: #f5f5f5;
}

.page-header {
    display: flex;
    align-items: center;
    padding: 15px;
    background-color: white;
    position: relative;
    border-bottom: 1px solid #eee;
}

.back-icon {
    font-size: 20px;
    cursor: pointer;
    position: absolute;
    left: 15px;
}

.page-title {
    flex: 1;
    text-align: center;
    font-size: 18px;
    font-weight: 500;
}

.address-list {
    padding: 15px;
    flex: 1;
    overflow-y: auto;
}

.address-card {
    background-color: white;
    border-radius: 8px;
    padding: 15px;
    margin-bottom: 15px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.address-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
}

.recipient-info {
    display: flex;
    align-items: center;
}

.recipient-name {
    font-size: 16px;
    font-weight: bold;
    margin-right: 10px;
}

.recipient-phone {
    font-size: 16px;
}

.address-tag {
    margin-left: 10px;
    padding: 2px 6px;
    font-size: 12px;
    border-radius: 4px;
    color: #fff;
}

.address-tag.默认 {
    background-color: #ff4d4f;
}

.address-detail {
    font-size: 14px;
    line-height: 1.5;
    color: #333;
    margin-bottom: 12px;
}

.address-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid #f0f0f0;
    padding-top: 12px;
}

.default-text {
    font-size: 14px;
    color: #666;
}

.action-buttons {
    display: flex;
    gap: 10px;
}

.add-address-btn {
    padding: 15px;
    margin-top: -15px;
    margin-bottom: 10px;
}

.add-address-btn .el-button {
    width: 20%;
    height: 44px;
    font-size: 16px;
    border-radius: 22px;
}

/* 提示信息样式
.address-card .warning-text {
    color: #ff4d4f;
    font-size: 12px;
    margin-top: 5px;
} */
</style>