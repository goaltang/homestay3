<template>
    <div class="homestay-form-container">
        <el-card class="form-card">
            <template #header>
                <div class="card-header">
                    <h2>{{ isEdit ? '编辑房源' : '添加新房源' }}</h2>
                </div>
            </template>

            <el-form ref="formRef" :model="homestayForm" :rules="rules" label-width="120px" class="homestay-form">
                <!-- 基本信息 -->
                <div class="form-section">
                    <h3>基本信息</h3>
                    <el-form-item label="房源标题" prop="title">
                        <el-input v-model="homestayForm.title" placeholder="请输入房源标题" />
                    </el-form-item>

                    <el-form-item label="房源类型" prop="type">
                        <el-select v-model="homestayForm.type" placeholder="请选择房源类型" style="width: 100%">
                            <el-option v-for="item in homestayTypes" :key="item.value" :label="item.label"
                                :value="item.value" />
                        </el-select>
                        <div v-if="homestayTypes.length === 0" style="color: red; font-size: 12px; margin-top: 5px;">
                            警告：房源类型列表为空，请检查API
                        </div>
                    </el-form-item>

                    <el-form-item label="每晚价格" prop="price">
                        <el-input-number v-model="homestayForm.price" :min="0" :precision="2" :step="10"
                            controls-position="right" style="width: 100%" />
                    </el-form-item>

                    <el-form-item label="最大入住人数" prop="maxGuests">
                        <el-input-number v-model="homestayForm.maxGuests" :min="1" :max="20" controls-position="right"
                            style="width: 100%" />
                    </el-form-item>

                    <el-form-item label="最少入住晚数" prop="minNights">
                        <el-input-number v-model="homestayForm.minNights" :min="1" :max="30" controls-position="right"
                            style="width: 100%" />
                    </el-form-item>
                </div>

                <!-- 位置信息 -->
                <div class="form-section">
                    <h3>位置信息</h3>
                    <div class="location-debug" v-if="provinces.length === 0"
                        style="color: red; font-size: 12px; margin-bottom: 10px;">
                        警告：省份列表为空，请检查API
                    </div>
                    <el-button type="primary" size="small" @click="reloadLocationData" style="margin-bottom: 10px;">
                        重新加载位置数据
                    </el-button>

                    <el-form-item label="所在省份" prop="province">
                        <el-select v-model="homestayForm.province" placeholder="请选择省份" @change="handleProvinceChange"
                            style="width: 100%">
                            <el-option v-for="item in provinces" :key="item.code" :label="item.name"
                                :value="item.code" />
                        </el-select>
                    </el-form-item>

                    <el-form-item label="所在城市" prop="city">
                        <el-select v-model="homestayForm.city" placeholder="请选择城市" :disabled="!homestayForm.province"
                            @change="handleCityChange" style="width: 100%">
                            <el-option v-for="item in cities" :key="item.code" :label="item.name" :value="item.code" />
                        </el-select>
                    </el-form-item>

                    <el-form-item label="所在区县" prop="district">
                        <el-select v-model="homestayForm.district" placeholder="请选择区县" :disabled="!homestayForm.city"
                            style="width: 100%">
                            <el-option v-for="item in districts" :key="item.code" :label="item.name"
                                :value="item.code" />
                        </el-select>
                    </el-form-item>

                    <el-form-item label="详细地址" prop="address">
                        <el-input v-model="homestayForm.address" placeholder="请输入详细地址" type="textarea" :rows="2" />
                    </el-form-item>
                </div>

                <!-- 设施与服务 -->
                <div class="form-section">
                    <h3>设施与服务</h3>
                    <el-form-item label="设施服务" prop="amenities">
                        <el-checkbox-group v-model="homestayForm.amenities">
                            <el-checkbox v-for="item in amenities" :key="item.value" :label="item.value">
                                {{ item.label }}
                            </el-checkbox>
                        </el-checkbox-group>
                    </el-form-item>
                </div>

                <!-- 描述信息 -->
                <div class="form-section">
                    <h3>描述信息</h3>
                    <el-form-item label="详细描述" prop="description">
                        <el-input v-model="homestayForm.description" type="textarea" :rows="5"
                            placeholder="请详细描述您的房源特点、周边环境等信息" />
                    </el-form-item>
                </div>

                <!-- 图片上传 -->
                <div class="form-section">
                    <h3>房源图片</h3>
                    <el-form-item label="封面图片" prop="coverImage">
                        <div class="upload-wrapper">
                            <div class="preview-container" v-if="homestayForm.coverImage">
                                <img :src="homestayForm.coverImage" class="preview-image" />
                                <div class="image-actions">
                                    <el-button type="danger" icon="Delete" circle size="small"
                                        @click="removeCoverImage" />
                                </div>
                            </div>
                            <div v-else>
                                <!-- 添加手动上传按钮替代el-upload -->
                                <el-button type="primary" @click="triggerUpload('cover')" :loading="uploadingCover">
                                    选择封面图片
                                </el-button>
                                <input type="file" ref="coverUploadRef" style="display:none"
                                    @change="handleFileChange('cover')" accept="image/*">
                                <div class="upload-text" style="margin-top: 10px">点击按钮选择一张图片作为封面</div>
                            </div>
                        </div>
                        <div class="upload-tip">推荐尺寸 800x600像素，JPEG/PNG格式，不超过2MB</div>
                    </el-form-item>

                    <el-form-item label="房源图片集" prop="images">
                        <div class="gallery-container">
                            <div class="gallery-images" v-if="homestayForm.images && homestayForm.images.length > 0">
                                <div v-for="(image, index) in homestayForm.images" :key="index" class="gallery-item">
                                    <img :src="image" class="gallery-image" />
                                    <div class="image-actions">
                                        <el-button type="danger" icon="Delete" circle size="small"
                                            @click="removeGalleryImage(index)" />
                                    </div>
                                </div>
                            </div>
                            <div class="gallery-upload" v-if="!homestayForm.images || homestayForm.images.length < 9">
                                <el-button type="primary" @click="triggerUpload('gallery')" :loading="uploadingGallery">
                                    添加房源图片
                                </el-button>
                                <input type="file" ref="galleryUploadRef" style="display:none"
                                    @change="handleFileChange('gallery')" accept="image/*">
                            </div>
                        </div>
                        <div class="upload-tip">最多上传9张图片，每张不超过2MB</div>
                    </el-form-item>
                </div>

                <!-- 提交按钮 -->
                <div class="form-footer">
                    <div class="footer-actions">
                        <el-button @click="handleCancel" :disabled="loading">取消</el-button>
                        <el-button type="primary" @click="handleSubmit" :loading="loading">
                            {{ isEdit ? '保存修改' : '创建房源' }}
                        </el-button>
                    </div>
                </div>
            </el-form>
        </el-card>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, UploadUserFile } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
    getHomestayById,
    createHomestay,
    updateHomestay,
    getProvinces as getProvincesApi,
    getCities as getCitiesApi,
    getDistricts as getDistrictsApi,
    getHomestayTypes as getHomestayTypesApi,
    getHomestayAmenities as getHomestayAmenitiesApi,
    uploadHomestayImage
} from '@/api/homestay'
import type { Homestay } from '@/types/homestay'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const isEdit = computed(() => !!route.params.id)
const homestayId = computed(() => Number(route.params.id))

// 上传文件相关
const fileList = ref<UploadUserFile[]>([])

// 表单数据
const homestayForm = reactive<Homestay>({
    title: '',
    type: '',
    price: '',
    status: 'INACTIVE', // 默认为未激活状态
    maxGuests: 1,
    minNights: 1,
    province: '',
    city: '',
    district: '',
    address: '',
    amenities: [],
    description: '',
    coverImage: '',
    images: [],
    featured: false
})

// 表单验证规则
const rules = {
    title: [{ required: true, message: '请输入房源标题', trigger: 'blur' }],
    type: [{ required: true, message: '请选择房源类型', trigger: 'change' }],
    price: [{ required: true, message: '请输入房源价格', trigger: 'blur' }],
    maxGuests: [{ required: true, message: '请设置最大入住人数', trigger: 'blur' }],
    minNights: [{ required: true, message: '请设置最少入住晚数', trigger: 'blur' }],
    province: [{ required: true, message: '请选择省份', trigger: 'change' }],
    city: [{ required: true, message: '请选择城市', trigger: 'change' }],
    district: [{ required: true, message: '请选择区县', trigger: 'change' }],
    address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
    description: [{ required: true, message: '请输入房源描述', trigger: 'blur' }],
    coverImage: [{ required: true, message: '请上传房源封面图片', trigger: 'change' }]
}

// 数据列表
const provinces = ref<any[]>([])
const cities = ref<any[]>([])
const districts = ref<any[]>([])
const homestayTypes = ref<any[]>([])
const amenities = ref<any[]>([])

// 上传headers
const uploadHeaders = computed(() => {
    return {
        Authorization: `Bearer ${localStorage.getItem('token')}`
    }
})

// 处理省份变更
const handleProvinceChange = (provinceCode: string) => {
    homestayForm.city = ''
    homestayForm.district = ''
    cities.value = []
    districts.value = []
    fetchCities(provinceCode)
}

// 处理城市变更
const handleCityChange = (cityCode: string) => {
    homestayForm.district = ''
    districts.value = []
    fetchDistricts(cityCode)
}

// 处理封面图片上传成功
const handleCoverSuccess = (response: any) => {
    if (response.success) {
        homestayForm.coverImage = response.data.url
        ElMessage.success('封面图片上传成功')
    } else {
        ElMessage.error(response.message || '封面图片上传失败')
    }
}

// 处理图片集上传成功
const handleImageSuccess = (response: any) => {
    if (response.success) {
        if (!homestayForm.images) {
            homestayForm.images = []
        }

        // 检查是否已达到最大图片数量
        if (homestayForm.images.length >= 9) {
            ElMessage.warning('最多只能上传9张图片')
            return
        }

        homestayForm.images.push(response.data.url)
        ElMessage.success('图片上传成功')
    } else {
        ElMessage.error(response.message || '图片上传失败')
    }
}

// 移除封面图片
const removeCoverImage = () => {
    if (homestayForm.coverImage) {
        homestayForm.coverImage = ''
        ElMessage.success('已移除封面图片')
    }
}

// 上传前验证
const beforeUpload = (file: File) => {
    const isImage = file.type.startsWith('image/')
    const isLt2M = file.size / 1024 / 1024 < 2

    if (!isImage) {
        ElMessage.error('只能上传图片文件！')
        return false
    }
    if (!isLt2M) {
        ElMessage.error('图片大小不能超过 2MB!')
        return false
    }

    return true
}

// 移除图片
const handleRemove = (file: any) => {
    const index = homestayForm.images?.findIndex(img => img === file.url)
    if (index !== -1) {
        const images = [...homestayForm.images || []]
        images.splice(index, 1)
        homestayForm.images = images
    }
}

// 提交表单
const handleSubmit = async () => {
    if (!formRef.value) return

    try {
        // 验证表单
        await formRef.value.validate()

        // 检查必须的图片
        if (!homestayForm.coverImage) {
            ElMessage.error('请上传房源封面图片')
            return
        }

        loading.value = true

        // 准备提交数据
        const submitData = {
            ...homestayForm,
            // 确保省市区使用名称而非代码（如果API需要）
            provinceName: provinces.value.find(p => p.code === homestayForm.province)?.name,
            cityName: cities.value.find(c => c.code === homestayForm.city)?.name,
            districtName: districts.value.find(d => d.code === homestayForm.district)?.name,
        }

        console.log('准备提交的房源数据:', submitData)

        let response
        if (isEdit.value) {
            response = await updateHomestay(Number(route.params.id), submitData)
        } else {
            response = await createHomestay(submitData)
        }

        console.log('服务器响应:', response)

        if (response.data) {
            ElMessage.success(isEdit.value ? '房源更新成功' : '房源创建成功')
            router.push('/host/homestay')
        } else {
            throw new Error('操作失败，服务器未返回数据')
        }
    } catch (error: any) {
        console.error('提交表单失败:', error)
        ElMessage.error('操作失败: ' + (error.message || '未知错误'))
    } finally {
        loading.value = false
    }
}

// 取消
const handleCancel = () => {
    ElMessageBox.confirm('确定要取消操作吗？未保存的数据将丢失', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        router.push('/host/homestay')
    }).catch(() => { })
}

// 获取下拉选项
const fetchOptions = async () => {
    try {
        // 获取省份列表
        const provinceRes = await getProvincesApi()
        provinces.value = provinceRes?.data?.data || []

        // 获取房源类型
        const typeRes = await getHomestayTypesApi()
        homestayTypes.value = typeRes?.data?.data || []

        // 获取设施列表
        const amenitiesRes = await getHomestayAmenitiesApi()
        amenities.value = amenitiesRes?.data?.data || []
    } catch (error) {
        console.error('获取选项数据失败', error)
        ElMessage.error('获取选项数据失败')
    }
}

// 获取城市列表
const fetchCities = async (provinceCode: string) => {
    try {
        const res = await getCitiesApi(provinceCode)
        cities.value = res?.data?.data || []
    } catch (error) {
        console.error('获取城市列表失败', error)
    }
}

// 获取区县列表
const fetchDistricts = async (cityCode: string) => {
    try {
        const res = await getDistrictsApi(cityCode)
        districts.value = res?.data?.data || []
    } catch (error) {
        console.error('获取区县列表失败', error)
    }
}

// 获取房源详情
const fetchHomestayDetail = async () => {
    if (!isEdit.value) return

    try {
        loading.value = true
        console.log('正在获取房源详情，ID:', homestayId.value)
        const res = await getHomestayById(homestayId.value)
        console.log('获取到的房源详情数据:', res)

        // 改变解析方式，检查不同的数据路径
        let homestay = null

        // 尝试不同的数据路径
        if (res?.data?.data) {
            homestay = res.data.data
            console.log('使用标准数据路径 res.data.data')
        } else if (res?.data) {
            homestay = res.data
            console.log('使用简化数据路径 res.data')
        }

        // 检查homestay是否包含必要字段
        if (homestay && homestay.id) {
            console.log('找到有效的房源数据:', homestay)

            // 直接批量赋值主要字段，添加更多的默认值处理
            homestayForm.title = homestay.title || '';
            homestayForm.type = homestay.type || '';
            homestayForm.price = typeof homestay.price === 'string' ? homestay.price : String(homestay.price || 0);
            homestayForm.status = homestay.status || 'INACTIVE';
            homestayForm.maxGuests = typeof homestay.maxGuests === 'number' ? homestay.maxGuests : 1;
            homestayForm.minNights = typeof homestay.minNights === 'number' ? homestay.minNights : 1;
            homestayForm.province = homestay.province || '';
            homestayForm.city = homestay.city || '';
            homestayForm.district = homestay.district || '';
            homestayForm.address = homestay.address || '';
            homestayForm.description = homestay.description || '';
            homestayForm.coverImage = homestay.coverImage || '';
            homestayForm.featured = !!homestay.featured;

            // 确保amenities是数组
            if (homestay.amenities && Array.isArray(homestay.amenities)) {
                homestayForm.amenities = [...homestay.amenities];
            } else {
                homestayForm.amenities = [];
                console.warn('房源没有amenities数组或格式不正确');
            }

            // 确保images是数组
            if (homestay.images && Array.isArray(homestay.images)) {
                // 过滤掉空值
                const validImages = homestay.images.filter((url: any) => url != null && url !== '');
                homestayForm.images = [...validImages];

                // 更新文件列表显示
                fileList.value = validImages.map((url: string) => {
                    return {
                        name: url.includes('/') ? url.substring(url.lastIndexOf('/') + 1) : `图片${Math.random().toString(36).substring(2, 8)}`,
                        url
                    }
                });
                console.log('处理后的图片列表:', fileList.value);
            } else {
                homestayForm.images = [];
                fileList.value = [];
                console.warn('房源没有images数组或格式不正确');
            }

            // 加载当前城市和区县的选项
            if (homestay.province) {
                console.log('加载城市列表，省份代码:', homestay.province);
                await fetchCities(homestay.province);
            }
            if (homestay.city) {
                console.log('加载区县列表，城市代码:', homestay.city);
                await fetchDistricts(homestay.city);
            }

            console.log('表单数据填充完成:', homestayForm);
        } else {
            console.error('房源数据格式不正确:', res);
            throw new Error('房源数据格式不正确');
        }
    } catch (error: any) {
        console.error('获取房源详情失败', error);
        ElMessage.error(error.message || '获取房源详情失败');
        router.push('/host/homestay');
    } finally {
        loading.value = false;
    }
}

// 添加引用和状态
const coverUploadRef = ref<HTMLElement | null>(null)
const galleryUploadRef = ref<HTMLElement | null>(null)
const uploadingCover = ref(false)
const uploadingGallery = ref(false)

// 添加重新加载位置数据函数
const reloadLocationData = async () => {
    try {
        // 清除现有数据
        provinces.value = []
        cities.value = []
        districts.value = []

        // 重新加载省份数据
        const provinceRes = await getProvincesApi()
        console.log('省份数据响应:', provinceRes)

        if (provinceRes?.data?.data && Array.isArray(provinceRes.data.data)) {
            provinces.value = provinceRes.data.data
            ElMessage.success(`成功加载 ${provinces.value.length} 个省份`)
        } else {
            ElMessage.error('省份数据格式不正确')
            console.error('省份数据格式不正确:', provinceRes)
        }
    } catch (error) {
        console.error('重新加载位置数据失败:', error)
        ElMessage.error('位置数据加载失败')
    }
}

// 触发文件选择
const triggerUpload = (type: 'cover' | 'gallery') => {
    if (type === 'cover' && coverUploadRef.value) {
        (coverUploadRef.value as HTMLInputElement).click()
    } else if (type === 'gallery' && galleryUploadRef.value) {
        (galleryUploadRef.value as HTMLInputElement).click()
    }
}

// 处理文件选择
const handleFileChange = async (type: 'cover' | 'gallery') => {
    let fileInput: HTMLInputElement | null = null

    if (type === 'cover') {
        fileInput = coverUploadRef.value as HTMLInputElement
        uploadingCover.value = true
    } else if (type === 'gallery') {
        fileInput = galleryUploadRef.value as HTMLInputElement
        uploadingGallery.value = true
    }

    if (fileInput && fileInput.files && fileInput.files.length > 0) {
        const file = fileInput.files[0]

        // 验证文件
        if (!beforeUpload(file)) {
            resetUploadState(type, fileInput)
            return
        }

        try {
            console.log(`开始上传${type === 'cover' ? '封面' : '图片集'}:`, file.name)

            // 使用API直接上传，传递图片类型参数
            const response = await uploadHomestayImage(file, type)
            console.log('上传响应:', response)

            if (response.data && response.data.success) {
                const fileUrl = response.data.data.url
                const imageType = response.data.data.imageType || type

                // 根据图片类型处理
                if (imageType === 'cover') {
                    homestayForm.coverImage = fileUrl
                    console.log('封面图片URL设置为:', fileUrl)
                    ElMessage.success('封面图片上传成功')
                } else {
                    if (!homestayForm.images) {
                        homestayForm.images = []
                    }

                    if (homestayForm.images.length >= 9) {
                        ElMessage.warning('最多只能上传9张图片')
                    } else {
                        homestayForm.images.push(fileUrl)
                        console.log('图片集新增图片URL:', fileUrl)
                        console.log('当前图片集合:', homestayForm.images)
                        ElMessage.success('房源图片上传成功')
                    }
                }
            } else {
                throw new Error(response.data?.message || '上传失败')
            }
        } catch (error: any) {
            console.error(`${type === 'cover' ? '封面' : '图片集'}上传失败:`, error)
            ElMessage.error('上传失败: ' + (error.message || '未知错误'))
        } finally {
            resetUploadState(type, fileInput)
        }
    }
}

// 重置上传状态
const resetUploadState = (type: 'cover' | 'gallery', fileInput: HTMLInputElement) => {
    if (type === 'cover') {
        uploadingCover.value = false
    } else {
        uploadingGallery.value = false
    }

    // 清除文件选择
    if (fileInput) {
        fileInput.value = ''
    }
}

// 移除图库图片
const removeGalleryImage = (index: number) => {
    if (homestayForm.images && index >= 0 && index < homestayForm.images.length) {
        homestayForm.images.splice(index, 1)
        ElMessage.success('已移除图片')
    }
}

onMounted(async () => {
    await fetchOptions()
    await fetchHomestayDetail()
})
</script>

<style scoped>
.homestay-form-container {
    padding: 20px;
}

.form-card {
    margin-bottom: 20px;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.form-section {
    margin-bottom: 30px;
    padding-bottom: 20px;
    border-bottom: 1px solid #ebeef5;
}

.form-section h3 {
    margin-bottom: 20px;
    font-weight: 600;
    color: #303133;
}

.upload-container {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    text-align: center;
}

.upload-icon {
    font-size: 28px;
    color: #8c939d;
    width: 178px;
    height: 178px;
    line-height: 178px;
}

.upload-text {
    color: #606266;
    font-size: 14px;
    text-align: center;
}

.upload-image {
    width: 178px;
    height: 178px;
    display: block;
    object-fit: cover;
}

.form-footer {
    display: flex;
    justify-content: center;
    margin-top: 30px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;
}

.footer-actions {
    display: flex;
    gap: 15px;
}

.footer-actions .el-button {
    min-width: 120px;
    padding: 12px 20px;
    font-size: 16px;
}

.upload-wrapper {
    position: relative;
}

.preview-container {
    position: relative;
}

.preview-image {
    width: 178px;
    height: 178px;
    display: block;
    object-fit: cover;
    border-radius: 6px;
    border: 1px solid #d9d9d9;
}

.image-actions {
    position: absolute;
    top: 5px;
    right: 5px;
    background-color: rgba(255, 255, 255, 0.7);
    border-radius: 50%;
    padding: 0;
}

.upload-tip {
    margin-top: 10px;
    font-size: 12px;
    color: #909399;
}

.gallery-container {
    display: flex;
    flex-direction: column;
    gap: 15px;
}

.gallery-images {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.gallery-item {
    position: relative;
    width: 178px;
    height: 178px;
}

.gallery-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 6px;
    border: 1px solid #d9d9d9;
}

.gallery-upload {
    margin-top: 10px;
}
</style>