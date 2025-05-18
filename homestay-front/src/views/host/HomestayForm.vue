<template>
    <div class="homestay-form-container">
        <el-card class="form-card">
            <template #header>
                <div class="card-header">
                    <h2>{{ isEdit ? '编辑房源' : '添加新房源' }}</h2>
                </div>
            </template>

            <!-- 添加进度指示器 -->
            <div class="form-progress">
                <el-steps :active="activeStep" finish-status="success" simple>
                    <el-step icon="HomeFilled">
                        <template #title><span @click="goToStep(1)" class="step-title">基本信息</span></template>
                    </el-step>
                    <el-step icon="Location">
                        <template #title><span @click="goToStep(2)" class="step-title">位置信息</span></template>
                    </el-step>
                    <el-step icon="Setting">
                        <template #title><span @click="goToStep(3)" class="step-title">设施与服务</span></template>
                    </el-step>
                    <el-step icon="Document">
                        <template #title><span @click="goToStep(4)" class="step-title">描述信息</span></template>
                    </el-step>
                    <el-step icon="Picture">
                        <template #title><span @click="goToStep(5)" class="step-title">房源图片</span></template>
                    </el-step>
                </el-steps>
            </div>

            <el-form ref="formRef" :model="homestayForm" :rules="rules" label-width="120px" class="homestay-form">
                <!-- 步骤内容区域 -->
                <div v-if="activeStep === 1">
                    <!-- 基本信息 -->
                    <div class="form-section">
                        <h3>基本信息 <span class="section-subtitle">— 创建吸引人的房源</span></h3>

                        <el-form-item label="房源标题" prop="title">
                            <el-input v-model="homestayForm.title" placeholder="请输入能吸引客人的标题，突出您房源的亮点" />
                            <div class="field-tips">
                                <div class="tip-title">创建好标题的技巧：</div>
                                <ul>
                                    <li>突出房源位置（如"临近西湖"、"地铁5分钟"）</li>
                                    <li>强调特色设施（如"带私人泳池"、"观景阳台"）</li>
                                    <li>理想长度为15-25个字符</li>
                                </ul>
                                <div class="examples">
                                    <div class="example-title">高质量标题示例：</div>
                                    <div class="example-item">💫 紧邻西湖景区 | 现代简约两居室 | 步行可达商圈</div>
                                </div>
                            </div>
                        </el-form-item>

                        <el-form-item label="房源类型" prop="type">
                            <PropertyTypeSelector v-model="homestayForm.type" />
                            <div class="field-tip">选择最能准确描述您整个房源的类型</div>
                        </el-form-item>

                        <el-form-item label="每晚价格" prop="price">
                            <div class="price-input-container">
                                <span class="price-symbol">¥</span>
                                <el-input-number v-model="homestayForm.price" :min="0" :precision="2" :step="10"
                                    controls-position="right" style="width: 100%" />
                            </div>

                            <div class="price-assistant">
                                <div class="market-insight">
                                    <span class="insight-title">💡 价格参考：</span>
                                    <span class="insight-content">您所在区域类似房源的价格范围为 ¥198-¥368/晚</span>
                                </div>

                                <div class="pricing-tips">
                                    <div class="tip-title">定价建议：</div>
                                    <ul>
                                        <li>新房源可设置略低于市场的价格，获得首批评价</li>
                                        <li>周末和节假日可考虑设置更高价格</li>
                                        <li>长期入住可提供折扣以增加预订率</li>
                                    </ul>
                                </div>
                            </div>
                        </el-form-item>

                        <div class="guests-nights-container">
                            <el-form-item label="最大入住人数" prop="maxGuests" class="half-width">
                                <div class="guest-selector">
                                    <el-input-number v-model="homestayForm.maxGuests" :min="1" :max="20"
                                        controls-position="right" />
                                    <div class="selector-description">包括成人和儿童总数</div>
                                </div>
                                <div class="field-tip">设置合理的人数上限有助于保护您的房屋</div>
                            </el-form-item>

                            <el-form-item label="最少入住晚数" prop="minNights" class="half-width">
                                <div class="nights-selector">
                                    <el-input-number v-model="homestayForm.minNights" :min="1" :max="30"
                                        controls-position="right" />
                                    <div class="selector-description">建议1-3晚可提高预订率</div>
                                </div>
                                <div class="field-tip">过长的最少预订晚数可能会降低订单量</div>
                            </el-form-item>
                        </div>
                    </div>
                </div>

                <div v-if="activeStep === 2">
                    <!-- 位置信息 -->
                    <div class="form-section">
                        <h3>位置信息 <span class="section-subtitle">— 帮助客人找到您的房源</span></h3>

                        <el-alert v-if="locationDataError" :title="locationDataError" type="error" :closable="false"
                            style="margin-bottom: 15px;" />

                        <div class="location-actions" v-if="provinces.length === 0">
                            <el-button type="primary" size="small" @click="reloadLocationData"
                                :loading="loadingLocationData" style="margin-bottom: 15px;">
                                重新加载位置数据
                            </el-button>
                        </div>

                        <!-- 使用级联选择器替换省市区下拉框 -->
                        <el-form-item label="所在地区" prop="provinceCode"> <!-- 或者验证 selectedAreaCodes -->
                            <el-cascader v-model="selectedAreaCodes" :options="areaData" placeholder="请选择省/市/区"
                                clearable style="width: 100%;" @change="handleAreaChange" />
                        </el-form-item>

                        <el-form-item label="详细地址" prop="addressDetail"> <!-- 修改 prop -->
                            <el-input v-model="homestayForm.addressDetail" placeholder="请输入街道名称、门牌号等详细地址"
                                type="textarea" :rows="2" />
                            <div class="field-tip">详细地址仅在客人预订后显示，以保护您的隐私</div>
                        </el-form-item>
                    </div>
                </div>

                <div v-if="activeStep === 3">
                    <!-- 设施与服务 -->
                    <div class="form-section">
                        <h3>设施与服务 <span class="section-subtitle">— 突出您的房源特色</span></h3>
                        <div class="amenities-tip">
                            选择您房源提供的所有设施和服务，这将帮助客人了解您房源的舒适度和便利性。
                            提供更多设施可能会提高您的房源吸引力！
                        </div>
                        <el-form-item label="设施服务" prop="amenities">
                            <AmenitySelector v-model="homestayForm.amenities" />
                        </el-form-item>
                    </div>
                </div>

                <div v-if="activeStep === 4">
                    <!-- 描述信息 -->
                    <div class="form-section description-section">
                        <h3>房源描述 <span class="section-subtitle">— 展示您房源的独特魅力</span></h3>

                        <div class="description-guide">
                            <p>详细、真实的描述能让客人了解您的房源特色，提高预订率和客人满意度。</p>
                        </div>

                        <el-form-item label="房源亮点" prop="highlights">
                            <el-input v-model="homestayForm.highlights" type="textarea" :rows="3"
                                placeholder="列出3-5个房源最吸引人的特点，如'270度观景落地窗'、'步行可达地铁站'等" />
                        </el-form-item>

                        <el-form-item label="详细描述" prop="description">
                            <div class="description-tips">
                                <span class="tips-title">描述建议包含：</span>
                                <span>房间布局、床型详情、设施特色、周边环境、交通便利性、适合人群等</span>
                            </div>
                            <el-input v-model="homestayForm.description" type="textarea" :rows="5"
                                placeholder="详细描述您的房源，让客人对入住体验有清晰的预期" />
                        </el-form-item>

                        <el-form-item label="周边环境" prop="surroundings">
                            <el-input v-model="homestayForm.surroundings" type="textarea" :rows="3"
                                placeholder="描述房源周边环境，如商场、餐厅、交通、景点等信息" />
                        </el-form-item>
                    </div>
                </div>

                <div v-if="activeStep === 5">
                    <!-- 图片上传 -->
                    <div class="form-section">
                        <h3>房源图片 <span class="section-subtitle">— 展示房源的视觉魅力</span></h3>

                        <!-- 图片上传问题调试工具 -->
                        <el-alert v-if="uploadError" type="error" :title="uploadErrorTitle"
                            :description="uploadErrorMessage" show-icon closable @close="clearUploadError">
                            <template #default>
                                <div class="upload-error-actions">
                                    <el-button size="small" type="primary" @click="retryLastUpload">
                                        重试上传
                                    </el-button>
                                    <el-button size="small" @click="toggleDebugInfo">
                                        {{ showDebugInfo ? '隐藏详情' : '显示详情' }}
                                    </el-button>
                                </div>
                                <div v-if="showDebugInfo" class="debug-info">
                                    <div class="debug-section">
                                        <h4>错误详情</h4>
                                        <pre>{{ JSON.stringify(uploadErrorDetails, null, 2) }}</pre>
                                    </div>
                                </div>
                            </template>
                        </el-alert>

                        <div class="image-upload-guide">
                            <p>高质量的图片能极大提升您房源的吸引力。建议上传明亮、整洁的房间照片，包括卧室、客厅、卫生间等主要空间。</p>
                        </div>

                        <el-form-item label="封面图片" prop="coverImage">
                            <div class="upload-wrapper">
                                <div class="preview-container" v-if="homestayForm.coverImage">
                                    <img :src="homestayForm.coverImage" class="preview-image" />
                                    <div class="image-actions">
                                        <el-button type="danger" icon="Delete" circle size="small"
                                            @click="removeCoverImage" />
                                    </div>
                                </div>
                                <div v-else class="cover-upload-guide">
                                    <el-upload class="upload-cover" :http-request="handleCustomUpload('cover')"
                                        :show-file-list="false" :disabled="loading">
                                        <div class="upload-icon"><el-icon>
                                                <Picture />
                                            </el-icon></div>
                                        <div class="upload-text">封面图片是您房源的第一印象</div>
                                        <el-button type="primary" :loading="uploadingCover">
                                            选择封面图片
                                        </el-button>
                                    </el-upload>
                                </div>
                            </div>
                            <div class="upload-tip">推荐选择能展示房源整体特色的照片，建议尺寸800x600像素，不超过2MB</div>
                        </el-form-item>

                        <el-form-item label="房源图片集" prop="images">
                            <div class="gallery-container">
                                <div class="gallery-images"
                                    v-if="homestayForm.images && homestayForm.images.length > 0">
                                    <div v-for="(image, index) in homestayForm.images" :key="index"
                                        class="gallery-item">
                                        <img :src="image" class="gallery-image" />
                                        <div class="image-actions">
                                            <el-button type="danger" icon="Delete" circle size="small"
                                                @click="removeGalleryImage(index)" />
                                        </div>
                                    </div>
                                </div>
                                <div class="gallery-upload"
                                    v-if="!homestayForm.images || homestayForm.images.length < 9">
                                    <div class="upload-guide"
                                        v-if="!homestayForm.images || homestayForm.images.length === 0">
                                        <p>请添加至少5张不同角度的房源照片，包括：</p>
                                        <ul>
                                            <li>每个卧室的照片</li>
                                            <li>客厅和厨房区域</li>
                                            <li>每个卫生间</li>
                                            <li>特色设施或空间（如阳台、花园等）</li>
                                        </ul>
                                    </div>
                                    <el-upload class="upload-gallery" :http-request="handleCustomUpload('gallery')"
                                        :show-file-list="false" :disabled="loading">
                                        <el-button type="primary" :loading="uploadingGallery">
                                            添加房源图片
                                        </el-button>
                                    </el-upload>
                                </div>
                            </div>
                            <div class="upload-tip">最多上传9张图片，每张不超过2MB。上传清晰、明亮的照片能吸引更多客人</div>
                        </el-form-item>
                    </div>
                </div>

                <!-- 步骤切换按钮 -->
                <div class="form-step-actions">
                    <el-button @click="prevStep" :disabled="activeStep === 1 || loading">上一步</el-button>
                    <el-button v-if="activeStep < 5" type="primary" @click="nextStep">下一步</el-button>
                    <el-button v-else type="success" @click="handleSubmit" :loading="loading">
                        {{ isEdit ? '保存修改' : '发布房源' }}
                    </el-button>
                    <el-button type="info" @click="saveDraft" :loading="savingDraft">保存草稿</el-button>

                    <!-- 认证检查按钮 -->
                    <el-button type="warning" @click="checkAuthentication" :loading="checkingAuth"
                        style="margin-left: 10px;">
                        检查认证状态
                    </el-button>

                    <div class="save-status" v-if="lastSaved">
                        <el-tooltip :content="'上次保存时间: ' + lastSaved?.toLocaleString()" placement="top">
                            <span><i class="el-icon-time"></i> {{ lastSavedText }}</span>
                        </el-tooltip>
                    </div>
                </div>

                <!-- 完成度指示器 -->
                <div class="form-completion">
                    <div class="completion-text">表单完成度: <strong>{{ formCompletionPercentage }}%</strong></div>
                    <el-progress :percentage="formCompletionPercentage"
                        :status="formCompletionPercentage === 100 ? 'success' : ''"></el-progress>
                </div>
            </el-form>
        </el-card>

        <!-- 侧边预览面板 -->
        <div class="preview-panel" v-if="showPreview">
            <div class="preview-header">
                <h3>房源预览</h3>
                <el-button type="text" @click="showPreview = false">关闭</el-button>
            </div>
            <div class="preview-content">
                <div class="preview-image">
                    <img :src="homestayForm.coverImage || '/img/no-image.png'" alt="房源封面" />
                </div>
                <div class="preview-info">
                    <h4>{{ homestayForm.title || '房源标题' }}</h4>
                    <div class="preview-type">{{ getHomestayTypeText(homestayForm.type) }}</div>
                    <div class="preview-price">¥{{ homestayForm.price || '0' }}/晚</div>
                    <div class="preview-location">
                        {{ getLocationText() }}
                    </div>
                    <div class="preview-highlights">
                        {{ homestayForm.highlights || '房源亮点' }}
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, FormInstance } from 'element-plus'
import type { UploadUserFile } from 'element-plus'
import { regionData, codeToText } from 'element-china-area-data' // 导入省市区数据 和 codeToText (小写c)
import {
    Plus, Delete, Edit, Search, Setting, Location,
    Check, Star, HomeFilled, Van, SwitchButton,
    Connection, Compass, Document, Picture
} from '@element-plus/icons-vue'
import {
    getHomestayById,
    getHomestayTypes, // 这个API可能不再需要，或者需要调整用途
    uploadHomestayImage,
    updateHomestay as updateHomestayApi,
    createHomestay as createHomestayApi
} from '@/api/homestay'
import {
    getAmenitiesByCategoryApi,
    addAmenityToHomestayApi,
    addAllAmenitiesToHomestayApi,
    removeAllAmenitiesFromHomestayApi
} from '@/api/amenities'
import AmenitySelector from '@/components/AmenitySelector.vue'
import type { Homestay } from '@/types/homestay'
import PropertyTypeSelector from '@/components/PropertyTypeSelector.vue'
import { detailedAuthCheck, getCurrentUser, isLoggedIn } from "@/utils/auth"
import MarkdownEditor from "@/components/MarkdownEditor/index.vue"
import UploadImage from "@/components/Upload/single-upload.vue"

// 导入本地使用的checkAuthentication以避免命名冲突
import { checkAuthentication as checkAuthAPI, ensureUserLoggedIn } from "@/utils/auth"

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const isEdit = computed(() => !!route.params.id)
const homestayId = computed(() => Number(route.params.id))

// 步骤和预览相关
const activeStep = ref(1);
const showPreview = ref(true);
const savingDraft = ref(false);

// --- 新增：跳转到指定步骤 --- 
const goToStep = (stepIndex: number) => {
    // 简单的跳转，不进行验证
    activeStep.value = stepIndex;
};

// 上传文件相关
const fileList = ref<UploadUserFile[]>([])

// 添加位置数据加载状态
const loadingLocationData = ref(false)
const locationDataError = ref('')

// 添加自动保存提示和状态
const lastSaved = ref<Date | null>(null)
const autoSaveInterval = ref<number | null>(null)

// 添加上传错误处理相关状态
const uploadError = ref(false)
const uploadErrorTitle = ref('')
const uploadErrorMessage = ref('')
const uploadErrorDetails = ref(null)
const showDebugInfo = ref(false)
const lastUploadType = ref<'cover' | 'gallery' | null>(null)
const lastUploadFile = ref<File | null>(null)

// 级联选择器状态
const selectedAreaCodes = ref<string[]>([])
const areaData = regionData // 将导入的数据赋值给模板中使用的变量

// 扩展表单数据 - 修改地址相关字段
const homestayForm = reactive<Partial<Homestay> & { // 使用 Partial 允许部分字段缺失
    highlights?: string;
    surroundings?: string;
    provinceCode?: string; // 新增
    cityCode?: string;     // 新增
    districtCode?: string; // 新增
    addressDetail?: string;// 新增
}>({ // 移除旧字段，添加新字段并设置初始值
    id: undefined,
    title: '',
    type: '',
    price: '',
    status: 'INACTIVE',
    maxGuests: 1,
    minNights: 1,
    provinceCode: '',
    cityCode: '',
    districtCode: '',
    addressDetail: '',
    amenities: [],
    description: '',
    coverImage: '',
    images: [],
    featured: false,
    highlights: '',
    surroundings: ''
})

// 更新表单验证规则
const rules = {
    title: [{ required: true, message: '请输入房源标题', trigger: 'blur' }],
    type: [{ required: true, message: '请选择房源类型', trigger: 'change' }],
    price: [{ required: true, message: '请输入房源价格', trigger: 'blur' }],
    maxGuests: [{ required: true, message: '请设置最大入住人数', trigger: 'blur' }],
    minNights: [{ required: true, message: '请设置最少入住晚数', trigger: 'blur' }],
    // 修改地址校验规则
    provinceCode: [{ required: true, message: '请选择所在地区', trigger: 'change' }], // 间接校验级联选择器
    addressDetail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
    description: [{ required: true, message: '请输入房源描述', trigger: 'blur' }],
    coverImage: [{ required: true, message: '请上传房源封面图片', trigger: 'change' }],
    highlights: [{ required: false, message: '请输入房源亮点', trigger: 'blur' }],
    surroundings: [{ required: false, message: '请描述周边环境', trigger: 'blur' }],
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

// 步骤导航方法
const nextStep = async () => {
    if (activeStep.value < 5) {
        try {
            let validateSuccess = true;

            if (activeStep.value === 1) {
                // 验证基本信息字段
                try {
                    // 逐个验证字段
                    await formRef.value?.validateField('title');
                    await formRef.value?.validateField('type');
                    await formRef.value?.validateField('price');
                    await formRef.value?.validateField('maxGuests');
                    await formRef.value?.validateField('minNights');
                } catch (error) {
                    validateSuccess = false;
                }
            } else if (activeStep.value === 2) {
                // 验证位置信息
                try {
                    await formRef.value?.validateField('provinceCode');
                    await formRef.value?.validateField('cityCode');
                    await formRef.value?.validateField('districtCode');
                    await formRef.value?.validateField('addressDetail');
                } catch (error) {
                    validateSuccess = false;
                }
            } else if (activeStep.value === 4) {
                // 验证描述信息
                try {
                    await formRef.value?.validateField('description');
                } catch (error) {
                    validateSuccess = false;
                }
            }

            if (validateSuccess) {
                activeStep.value++;
            } else {
                ElMessage.warning('请完成必填字段后再继续');
            }
        } catch (error) {
            console.error('表单验证错误:', error);
            ElMessage.warning('请完成必填字段后再继续');
            return;
        }
    }
};

const prevStep = () => {
    if (activeStep.value > 1) {
        activeStep.value--;
    }
};

/**
 * 预处理表单数据
 */
const preprocessFormData = () => {
    const processedData = JSON.parse(JSON.stringify(homestayForm)) as typeof homestayForm;

    // 添加用户名(所有者) - 逻辑保持不变
    try {
        const userInfoStr = localStorage.getItem('userInfo')
        if (userInfoStr) {
            const userInfo = JSON.parse(userInfoStr)
            if (userInfo && userInfo.username) {
                (processedData as any).ownerUsername = userInfo.username // 使用 any 断言避免 TS 错误
            } else {
                (processedData as any).ownerUsername = '' // 确保字段存在
            }
        } else {
            (processedData as any).ownerUsername = ''
        }
    } catch (e) {
        console.error('获取用户信息失败', e);
        (processedData as any).ownerUsername = ''
    }

    // 处理价格 - 逻辑保持不变
    if (processedData.price && typeof processedData.price === 'string') {
        processedData.price = String(parseFloat(processedData.price))
    }

    // 处理最大/最小入住 - 逻辑保持不变
    if (processedData.maxGuests) {
        processedData.maxGuests = Number(processedData.maxGuests)
    }
    if (processedData.minNights) {
        processedData.minNights = Number(processedData.minNights)
    }

    // 处理设施数据 - 逻辑保持不变 (但需要修复 Linter Error)
    if (processedData.amenities) {
        if (Array.isArray(processedData.amenities)) {
            const amenityValues = processedData.amenities.map((amenity: any) => { // 添加类型注解
                if (typeof amenity === 'string') return amenity
                if (typeof amenity === 'object' && amenity !== null) return amenity.value || ''
                return ''
            }).filter(Boolean)
            processedData.amenities = amenityValues
        } else {
            processedData.amenities = []
        }
    } else {
        processedData.amenities = []
    }

    // 移除对旧地址字段的处理
    // delete processedData.province;
    // delete processedData.city;
    // delete processedData.district;
    // delete processedData.address;

    // 确保发送的是 provinceCode, cityCode, districtCode, addressDetail
    // 这些字段已经在 homestayForm 中，无需额外处理

    return processedData
}

/**
 * 保存草稿
 */
const saveDraft = async () => {
    try {
        savingDraft.value = true; // 使用 savingDraft 而不是 loading
        const processedData = preprocessFormData();
        processedData.status = 'DRAFT';

        let result;
        if (isEdit.value && homestayId.value) {
            result = await updateHomestayApi(Number(homestayId.value), processedData);
        } else {
            result = await createHomestayApi(processedData);
        }

        if (result.data) { // 检查 result.data 是否存在
            ElMessage.success('房源草稿保存成功');
            lastSaved.value = new Date(); // 更新保存时间
            if (!isEdit.value && result.data.id) {
                // 如果是新创建的房源，导航到编辑页面，而不是直接修改 computed 属性
                router.replace(`/host/homestay/edit/${result.data.id}`);
            }
        } else {
            ElMessage.error(result.message || '保存失败，请稍后重试');
        }
    } catch (error) {
        console.error('保存草稿出错:', error);
        // 修复 Linter Error: error 类型
        ElMessage.error('保存失败: ' + ((error as Error).message || '未知错误'));
    } finally {
        savingDraft.value = false;
    }
}

/**
 * 提交表单
 */
const handleSubmit = async () => {
    try {
        const form = formRef.value;
        if (!form) {
            ElMessage.error('表单不存在');
            return;
        }

        // 验证表单
        await form.validate();

        // 深拷贝，避免直接修改formData
        const submitData = JSON.parse(JSON.stringify(homestayForm));

        // 处理设施数据
        const amenitiesData = processAmenities(submitData.amenities || []);
        console.log('处理后的设施数据:', amenitiesData);

        // 先移除设施数据，减少保存时的错误风险
        const dataWithoutAmenities = { ...submitData };
        delete dataWithoutAmenities.amenities;

        console.log('第一步：准备提交不含设施的数据:', dataWithoutAmenities);

        loading.value = true;

        // 根据模式确定提交方法
        const formToSubmit = {
            ...dataWithoutAmenities,
            // 确保其他必要的字段存在
            status: dataWithoutAmenities.status || "INACTIVE",
            maxGuests: Number(dataWithoutAmenities.maxGuests) || 1,
            minNights: Number(dataWithoutAmenities.minNights) || 1
        };

        try {
            let response;

            if (isEdit.value) {
                // 编辑模式 - 先提交不含设施的数据
                const homestayId = Number(route.params.id);
                console.log('编辑房源(不含设施)', homestayId, formToSubmit);

                // 确保当前有有效token和用户名
                const token = localStorage.getItem('token');
                let username = '';
                try {
                    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
                    username = userInfo.username || '';
                } catch (e) {
                    console.warn('无法获取用户信息', e);
                }

                // 确保设置了所有者用户名，这对于权限验证很重要
                formToSubmit.ownerUsername = username;

                // 发送请求前再次检查权限
                const hasPermission = await checkEditPermission(homestayId);
                if (!hasPermission) {
                    ElMessage.error('没有权限执行此操作');
                    loading.value = false;
                    return;
                }

                // 设置明确的请求头
                const headers = {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`,
                    'X-Username': username
                };

                // 发送请求
                response = await updateHomestayApi(homestayId, formToSubmit);
                console.log('服务器响应(编辑基本信息):', response.data);

                // 第二步：设施单独处理（如果有设施）
                if (amenitiesData && amenitiesData.length > 0) {
                    console.log('第二步：开始更新设施数据');

                    // 导入设施相关API
                    const { removeAllAmenitiesFromHomestayApi, addAmenityToHomestayApi } = await import('@/api/amenities');

                    // 先移除所有设施
                    await removeAllAmenitiesFromHomestayApi(homestayId);
                    console.log('已移除所有旧设施');

                    // 逐个添加新设施
                    for (const amenity of amenitiesData) {
                        try {
                            if (amenity && typeof amenity === 'object' && 'value' in amenity) {
                                await addAmenityToHomestayApi(homestayId, String(amenity.value));
                                console.log(`已添加设施: ${amenity.value}`);
                            } else {
                                console.warn('跳过无效的设施数据:', amenity);
                            }
                        } catch (err) {
                            console.error(`添加设施失败:`, err);
                        }
                    }

                    console.log('所有设施更新完成');
                }

                ElMessage.success('房源更新成功！');
            } else {
                // 新建模式 - 先提交不含设施的数据
                console.log('创建新房源(不含设施)', formToSubmit);

                response = await createHomestayApi(formToSubmit);
                console.log('服务器响应(创建基本信息):', response.data);

                // 如果创建成功且有设施数据，逐个添加设施
                if (response.data && response.data.id && amenitiesData && amenitiesData.length > 0) {
                    const newHomestayId = response.data.id;
                    console.log(`房源已创建，ID: ${newHomestayId}，开始添加设施`);

                    // 导入设施相关API
                    const { addAmenityToHomestayApi } = await import('@/api/amenities');

                    // 逐个添加设施
                    for (const amenity of amenitiesData) {
                        try {
                            if (amenity && typeof amenity === 'object' && 'value' in amenity) {
                                await addAmenityToHomestayApi(newHomestayId, String(amenity.value));
                                console.log(`已添加设施: ${amenity.value}`);
                            } else {
                                console.warn('跳过无效的设施数据:', amenity);
                            }
                        } catch (err) {
                            console.error(`添加设施失败:`, err);
                        }
                    }

                    console.log('所有设施添加完成');
                }

                ElMessage.success('房源创建成功！');
            }

            console.log('服务器响应:', response);

            if (response.data) {
                // 成功提交后，清除草稿
                localStorage.removeItem('homestayDraft');

                // 显示成功信息
                ElMessageBox.alert(
                    `房源${isEdit.value ? '更新' : '创建'}成功！您可以在房源管理页面查看并管理您的房源。`,
                    '操作成功',
                    {
                        confirmButtonText: '返回房源管理',
                        type: 'success',
                        callback: () => {
                            router.push('/host/homestay');
                        }
                    }
                );
            } else {
                throw new Error('操作失败，服务器未返回数据');
            }
        } catch (error: any) {
            console.error('提交表单失败:', error);

            // 增强错误处理，显示更详细的错误信息
            let errorMessage = '操作失败: ' + (error.message || '未知错误');

            // 如果有详细的服务器响应，尝试提取更有用的错误信息
            if (error.response && error.response.data) {
                const serverError = error.response.data;
                console.log('服务器返回的错误详情:', serverError);

                if (serverError.error) {
                    errorMessage = `服务器错误: ${serverError.error}`;
                } else if (serverError.message) {
                    errorMessage = `服务器错误: ${serverError.message}`;
                }
            }

            ElMessage.error(errorMessage);
        } finally {
            loading.value = false;
        }
    } catch (error) {
        console.error('表单验证失败:', error);
        ElMessage.error('表单验证失败，请检查输入内容');
    }
}

// 取消
const handleCancel = () => {
    ElMessageBox.confirm('确定要取消操作吗？未保存的数据将丢失，您可以选择"保存草稿"保留已填写的内容。', '提示', {
        confirmButtonText: '确定离开',
        cancelButtonText: '继续编辑',
        distinguishCancelAndClose: true,
        type: 'warning'
    }).then(() => {
        router.push('/host/homestay')
    }).catch(() => { })
}

// 获取下拉选项
const fetchOptions = async () => {
    // 保留加载类型和设施的逻辑
    try {
        // 获取房源类型
        const typeRes = await getHomestayTypes()
        // 修复 Linter Error: 假设 getHomestayTypes 直接返回数组
        homestayTypes.value = typeRes || []

        // 获取设施列表
        const amenitiesRes = await getAmenitiesByCategoryApi()
        amenities.value = amenitiesRes?.data?.data || []
    } catch (error) {
        console.error('加载选项数据失败', error)
        ElMessage.warning('加载基础数据失败，部分功能可能受限');
    }
}

// 根据省份获取城市列表
const fetchCities = async (provinceCode: string) => {
    try {
        const res = await getCities(provinceCode)
        cities.value = res?.data?.data || []
    } catch (error) {
        console.error('获取城市列表失败', error)
        ElMessage.error('获取城市列表失败，请稍后再试')
    }
}

// 根据城市获取区县列表
const fetchDistricts = async (cityCode: string) => {
    try {
        const res = await getDistricts(cityCode)
        districts.value = res?.data?.data || []
    } catch (error) {
        console.error('获取区县列表失败', error)
        ElMessage.error('获取区县列表失败，请稍后再试')
    }
}

// 获取房源详情
const fetchHomestayDetail = async () => {
    if (!isEdit.value) {
        // 如果不是编辑模式，尝试加载草稿
        const draftData = localStorage.getItem('homestayDraft');
        if (draftData) {
            try {
                const parsedDraft = JSON.parse(draftData);

                // 检查草稿是否过期（超过7天）
                const lastSavedTime = parsedDraft._lastSaved ? new Date(parsedDraft._lastSaved) : null;
                const now = new Date();
                const isExpired = lastSavedTime && ((now.getTime() - lastSavedTime.getTime()) > 7 * 24 * 60 * 60 * 1000);

                // 格式化上次保存时间
                const lastSavedText = lastSavedTime ?
                    lastSavedTime.toLocaleString() :
                    '未知时间';

                const confirmMessage = isExpired ?
                    `发现一个较旧的房源草稿（保存于${lastSavedText}），是否仍要加载？` :
                    `发现您有未完成的房源草稿（保存于${lastSavedText}），是否加载？`;

                // 提示用户是否加载草稿
                ElMessageBox.confirm(confirmMessage, '提示', {
                    confirmButtonText: '加载草稿',
                    cancelButtonText: '不加载',
                    type: isExpired ? 'warning' : 'info'
                }).then(() => {
                    // 应用草稿数据到表单
                    Object.assign(homestayForm, parsedDraft);

                    // 设置最后保存时间
                    if (lastSavedTime) {
                        lastSaved.value = lastSavedTime;
                    }

                    ElMessage.success('草稿加载成功');

                    // 加载关联数据
                    if (parsedDraft.provinceCode) {
                        fetchCities(parsedDraft.provinceCode);
                    }
                    if (parsedDraft.cityCode) {
                        fetchDistricts(parsedDraft.cityCode);
                    }
                }).catch(() => {
                    // 用户选择不加载
                    localStorage.removeItem('homestayDraft');
                    ElMessage.info('已忽略旧草稿');
                });
            } catch (error) {
                console.error('解析草稿数据失败:', error);
                ElMessage.error('草稿数据已损坏，无法加载');
                localStorage.removeItem('homestayDraft');
            }
        }
        return;
    }

    try {
        loading.value = true
        console.log('正在获取房源详情，ID:', homestayId.value)
        const res = await getHomestayById(homestayId.value)
        console.log('获取到的房源详情数据:', res)

        // 检查amenities数据
        if (res?.data?.amenities) {
            console.log('获取到的设施数据:', JSON.stringify(res.data.amenities));
            console.log('设施数量:', res.data.amenities.length);

            // 记录每个设施的详细信息
            res.data.amenities.forEach((amenity: any, index: number) => {
                console.log(`设施 ${index + 1}:`,
                    `value=${amenity.value}`,
                    `label=${amenity.label}`,
                    `icon=${amenity.icon || '无图标'}`);
            });
        } else {
            console.warn('API响应中没有设施数据');
        }

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
            homestayForm.provinceCode = homestay.provinceCode || '';
            homestayForm.cityCode = homestay.cityCode || '';
            homestayForm.districtCode = homestay.districtCode || '';
            homestayForm.addressDetail = homestay.addressDetail || '';
            homestayForm.description = homestay.description || '';
            homestayForm.coverImage = homestay.coverImage || '';
            homestayForm.featured = !!homestay.featured;

            // 尝试从描述中提取亮点和周边环境（实际应该由后端提供单独字段）
            const descParts = homestayForm.description.split(/\n\n/);
            if (descParts.length > 1) {
                // 简单处理，假设描述格式包含"房源亮点："和"周边环境："
                const highlightsMatch = homestayForm.description.match(/房源亮点：\n([\s\S]*?)(?=\n\n|$)/);
                if (highlightsMatch && highlightsMatch[1]) {
                    homestayForm.highlights = highlightsMatch[1].trim();
                }

                const surroundingsMatch = homestayForm.description.match(/周边环境：\n([\s\S]*?)(?=\n\n|$)/);
                if (surroundingsMatch && surroundingsMatch[1]) {
                    homestayForm.surroundings = surroundingsMatch[1].trim();
                }

                // 清理原始描述中的这些部分
                homestayForm.description = homestayForm.description
                    .replace(/房源亮点：\n[\s\S]*?(?=\n\n|$)/, '')
                    .replace(/周边环境：\n[\s\S]*?(?=\n\n|$)/, '')
                    .trim();
            }

            // 确保amenities是数组
            if (homestay.amenities && Array.isArray(homestay.amenities)) {
                console.log('设置amenities数据，原始数据:', JSON.stringify(homestay.amenities));
                homestayForm.amenities = [...homestay.amenities];
                console.log('设置后的amenities数据:', JSON.stringify(homestayForm.amenities));

                // 打印每个设施的详细信息
                homestayForm.amenities.forEach((amenity: any, index: number) => {
                    console.log(`表单中的设施 ${index + 1}:`,
                        typeof amenity === 'string' ? amenity :
                            `value=${amenity.value || '无value'}, label=${amenity.label || '无label'}`);
                });
            } else {
                homestayForm.amenities = [];
                console.warn('房源没有amenities数组或格式不正确, 原始值:', homestay.amenities);
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
            if (homestay.provinceCode) {
                console.log('加载城市列表，省份代码:', homestay.provinceCode);
                await fetchCities(homestay.provinceCode);
            }
            if (homestay.cityCode) {
                console.log('加载区县列表，城市代码:', homestay.cityCode);
                await fetchDistricts(homestay.cityCode);
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
const uploadingCover = ref(false)
const uploadingGallery = ref(false)
const coverUploadRef = ref<HTMLInputElement | null>(null)
const galleryUploadRef = ref<HTMLInputElement | null>(null)

// 自定义处理上传函数
const handleCustomUpload = (type: 'cover' | 'gallery') => {
    return (options: any) => {
        const { file, onSuccess, onError } = options;

        // 设置加载状态
        if (type === 'cover') {
            uploadingCover.value = true;
        } else {
            uploadingGallery.value = true;
        }

        // 保存最后上传的文件信息用于重试
        lastUploadType.value = type;
        lastUploadFile.value = file;

        // 验证文件
        if (!beforeUpload(file)) {
            resetUploadState(type);
            return;
        }

        // 显示加载状态
        ElMessage.info(`正在上传${type === 'cover' ? '封面' : '图片集'}图片，请稍候...`);

        // 使用API函数上传图片
        let uploadPromise;

        // 如果有房源ID，使用新的上传API
        if (homestayId.value) {
            uploadPromise = uploadHomestayImage(file, type, Number(homestayId.value));
        } else {
            uploadPromise = uploadHomestayImage(file, type);
        }

        uploadPromise
            .then(response => {
                console.log('图片上传成功:', response);

                // 根据响应结构处理成功情况
                if (response.data && (response.data.status === 'success' || response.data.success)) {
                    let imageUrl = '';

                    // 处理不同的响应格式
                    if (response.data.data) {
                        if (typeof response.data.data === 'object') {
                            imageUrl = response.data.data.url || response.data.data.imageUrl || response.data.data.downloadUrl;
                        } else if (typeof response.data.data === 'string') {
                            imageUrl = response.data.data;
                        }
                    } else if (response.data.downloadUrl) {
                        imageUrl = response.data.downloadUrl;
                    }

                    if (!imageUrl) {
                        console.error('无法获取上传图片URL:', response.data);
                        ElMessage.error('图片上传成功，但无法获取URL');
                        resetUploadState(type);
                        if (onError) onError(new Error('无法获取上传图片URL'));
                        return;
                    }

                    // 保存图片URL
                    if (type === 'cover') {
                        homestayForm.coverImage = imageUrl;
                        ElMessage.success('封面图片上传成功');
                    } else {
                        if (!homestayForm.images) {
                            homestayForm.images = [];
                        }
                        homestayForm.images.push(imageUrl);
                        ElMessage.success('图片上传成功');
                    }

                    if (onSuccess) onSuccess(response);
                } else {
                    console.error('图片上传失败:', response);
                    ElMessage.error('图片上传失败: ' + (response.data?.message || '未知错误'));
                    if (onError) onError(new Error('上传失败'));
                }
            })
            .catch(error => {
                console.error('图片上传异常:', error);
                ElMessage.error(`图片上传失败: ${error.message || '网络错误'}`);
                if (onError) onError(error);
            })
            .finally(() => {
                resetUploadState(type);
            });
    };
};

// 重置上传状态
const resetUploadState = (type: 'cover' | 'gallery', fileInput?: HTMLInputElement) => {
    if (type === 'cover') {
        uploadingCover.value = false;
    } else {
        uploadingGallery.value = false;
    }

    // 清除文件选择
    if (fileInput) {
        fileInput.value = '';
    }
};

// 重试上传函数
const retryLastUpload = async () => {
    if (lastUploadType.value && lastUploadFile.value) {
        // 清除错误状态
        clearUploadError();

        // 直接调用handleCustomUpload处理上传
        const uploadHandler = handleCustomUpload(lastUploadType.value);
        uploadHandler({
            file: lastUploadFile.value,
            onSuccess: () => {
                // 重置最后上传信息
                lastUploadType.value = null;
                lastUploadFile.value = null;
            },
            onError: (error: any) => {
                console.error('重试上传失败:', error);
            }
        });
    } else {
        ElMessage.warning('没有可重试的上传任务');
    }
};

// 在组件挂载时设置自动保存定时器
onMounted(async () => {
    console.log('HomestayForm组件加载，正在初始化...');

    // 打印当前角色信息便于调试
    const userInfo = localStorage.getItem('userInfo');
    if (userInfo) {
        try {
            const userObj = JSON.parse(userInfo);
            console.log('当前登录用户信息:', {
                id: userObj.id,
                username: userObj.username,
                role: userObj.role
            });
        } catch (e) {
            console.error('解析用户信息失败', e);
        }
    } else {
        console.warn('本地存储中没有用户信息');
    }

    // 添加权限检查，确保用户是HOST或LANDLORD角色
    // 注意：后端角色可能包含ROLE_前缀
    const hasPermission = await ensureUserLoggedIn(['HOST', 'LANDLORD', 'ROLE_HOST', 'ROLE_LANDLORD']);
    if (!hasPermission) {
        console.error('用户没有房东权限，无法编辑房源');
        ElMessage.error('您需要房东权限才能编辑房源信息');
        // 3秒后重定向到主页
        setTimeout(() => {
            router.push('/');
        }, 3000);
        return; // 权限检查函数会自动处理重定向
    }

    console.log('角色权限检查通过，继续初始化');

    // 编辑模式：还需检查房源所有权
    if (isEdit.value) {
        console.log(`编辑模式，房源ID: ${homestayId.value}`);

        // 导入所有权检查函数
        const { checkHomestayOwnership } = await import('@/api/homestay');

        // 检查用户是否是该房源的拥有者
        try {
            const isOwner = await checkHomestayOwnership(homestayId.value);
            if (!isOwner) {
                ElMessage.error('您不是该房源的所有者，无法编辑');
                // 3秒后返回房源列表
                setTimeout(() => {
                    router.push('/host/homestays');
                }, 3000);
                return;
            }
            console.log('所有权检查通过，用户可以编辑此房源');
        } catch (error) {
            console.error('检查房源所有权失败:', error);
            ElMessage.error('验证房源所有权失败，请稍后再试');
            return;
        }
    }

    // 获取下拉选项数据
    try {
        await fetchOptions();
        console.log('选项数据加载成功');
    } catch (error) {
        console.error('获取选项数据失败:', error);
        ElMessage.warning('加载基础数据失败，部分功能可能受限');
    }

    // 编辑模式：加载现有房源数据
    if (isEdit.value) {
        try {
            console.log(`开始加载房源ID:${homestayId.value}的详细信息`);
            await initEditForm();
            console.log('房源数据加载成功');
        } catch (error) {
            console.error('加载房源数据失败:', error);
            ElMessage.error('加载房源数据失败，请稍后再试');
        }
    } else {
        // 新增模式：加载草稿或设置默认值
        console.log('新增模式，设置默认值');
        loadDraft();
    }

    // 设置自动保存
    autoSaveInterval.value = window.setInterval(autoSaveDraft, 3 * 60 * 1000);
    console.log('初始化完成，已设置自动保存');
});

// 在组件卸载时清除定时器
onUnmounted(() => {
    if (autoSaveInterval.value) {
        clearInterval(autoSaveInterval.value)
    }
})

// 计算上次保存时间文本
const lastSavedText = computed(() => {
    if (!lastSaved.value) return '未保存';

    const now = new Date();
    const diff = now.getTime() - lastSaved.value.getTime();

    // 如果小于1分钟
    if (diff < 60 * 1000) {
        return '刚刚保存';
    }

    // 如果小于1小时
    if (diff < 60 * 60 * 1000) {
        const minutes = Math.floor(diff / (60 * 1000));
        return `${minutes}分钟前保存`;
    }

    // 否则显示具体时间
    return `${lastSaved.value.toLocaleTimeString()}保存`;
});

// 清除上传错误
const clearUploadError = () => {
    uploadError.value = false
    uploadErrorTitle.value = ''
    uploadErrorMessage.value = ''
    uploadErrorDetails.value = null
}

// 显示/隐藏调试信息
const toggleDebugInfo = () => {
    showDebugInfo.value = !showDebugInfo.value
}

// 移除图库图片
const removeGalleryImage = (index: number) => {
    if (homestayForm.images && index >= 0 && index < homestayForm.images.length) {
        homestayForm.images.splice(index, 1);
        ElMessage.success('已移除图片');
    }
}

// 自动保存草稿
const autoSaveDraft = () => {
    if (formCompletionPercentage.value > 0) {
        // 将当前表单数据保存为草稿
        const draftData = {
            ...homestayForm,
            status: 'DRAFT', // 设置状态为草稿
            _lastSaved: new Date().toISOString()
        };

        // 保存到本地存储
        localStorage.setItem('homestayDraft', JSON.stringify(draftData));
        lastSaved.value = new Date();
        console.log('自动保存草稿完成:', new Date().toLocaleTimeString());
    }
}

// 添加重新加载位置数据函数
const reloadLocationData = async () => {
    try {
        // 显示加载状态
        loadingLocationData.value = true;
        locationDataError.value = '';

        // 清除现有数据
        provinces.value = [];
        cities.value = [];
        districts.value = [];

        // 重新加载省份数据
        const provinceRes = await getProvinces();
        console.log('省份数据响应:', provinceRes);

        if (provinceRes?.data?.data && Array.isArray(provinceRes.data.data)) {
            provinces.value = provinceRes.data.data;
            ElMessage.success(`成功加载 ${provinces.value.length} 个省份`);
            locationDataError.value = '';
        } else {
            locationDataError.value = '省份数据格式不正确，请联系管理员';
            ElMessage.error('省份数据格式不正确');
            console.error('省份数据格式不正确:', provinceRes);
        }
    } catch (error) {
        console.error('重新加载位置数据失败:', error);
        locationDataError.value = '位置数据加载失败，请稍后再试';
        ElMessage.error('位置数据加载失败');
    } finally {
        loadingLocationData.value = false;
    }
};

// 添加认证检查相关状态
const checkingAuth = ref(false);

// 检查认证状态的辅助函数
const checkAuthentication = async () => {
    checkingAuth.value = true;
    try {
        // 调用API中的认证检查函数
        await checkAuthAPI();

        // 显示成功消息
        ElMessage.success('认证检查完成，请查看控制台日志获取详细信息');
    } catch (error: any) {
        console.error('认证检查失败:', error);
        ElMessage.error('认证检查失败: ' + (error.message || '未知错误'));
    } finally {
        checkingAuth.value = false;
    }
};

// 初始化房源编辑表单
const initEditForm = async () => {
    try {
        console.log(`初始化房源编辑表单, ID: ${homestayId.value}`);
        loading.value = true;
        const response = await getHomestayById(Number(homestayId.value));

        // --- 调试日志：检查 API 返回的原始数据 ---
        console.log('API 返回的房源详情原始数据:', JSON.stringify(response.data, null, 2));

        const homestayData = response.data;

        if (homestayData) {
            Object.assign(homestayForm, homestayData);

            // --- 添加日志：检查加载后的 type 值 ---
            console.log('[initEditForm] After Object.assign, homestayForm.type:', homestayForm.type);
            // --- 日志结束 ---

            // --- 调试日志：检查待回显的 Code --- 
            console.log('待回显的地址编码:', {
                provinceCode: homestayData.provinceCode,
                cityCode: homestayData.cityCode,
                districtCode: homestayData.districtCode
            });

            if (homestayData.provinceCode && homestayData.cityCode) {
                const codes = [homestayData.provinceCode, homestayData.cityCode];
                if (homestayData.districtCode) {
                    codes.push(homestayData.districtCode);
                }
                selectedAreaCodes.value = codes;
                // --- 调试日志：检查设置给 Cascader 的值 ---
                console.log('已设置 selectedAreaCodes.value:', JSON.stringify(selectedAreaCodes.value));
            } else {
                selectedAreaCodes.value = [];
                console.log('省份或城市编码缺失，清空 selectedAreaCodes.value');
            }

            // ... (其他字段处理保持不变) ...
            homestayForm.price = String(homestayData.price || 0);
            if (homestayData.amenities && Array.isArray(homestayData.amenities)) { homestayForm.amenities = [...homestayData.amenities]; } else { homestayForm.amenities = []; }
            if (homestayData.images && Array.isArray(homestayData.images)) { const validImages = homestayData.images.filter((url: any) => url != null && url !== ''); homestayForm.images = [...validImages]; fileList.value = validImages.map((url: string) => ({ name: url.substring(url.lastIndexOf('/') + 1), url })); } else { homestayForm.images = []; fileList.value = []; }

        } else {
            throw new Error('未获取到房源数据');
        }

    } catch (error) {
        console.error('初始化房源编辑表单失败:', error);
        ElMessage.error('加载房源数据失败，请稍后再试');
    } finally {
        loading.value = false;
    }
}

// 初始化位置级联选择器
const initLocationCascader = async () => {
    try {
        loading.value = true;
        // 获取省份数据
        const provinceRes = await getProvinces();
        if (provinceRes?.data?.data) {
            provinces.value = provinceRes.data.data;
        }

        // ... existing code ...
    } catch (error) {
        // ... existing code ...
    } finally {
        loading.value = false;
    }
};

// 添加加载草稿函数
const loadDraft = () => {
    console.log('尝试加载草稿数据');
    try {
        const draftData = localStorage.getItem('homestayFormDraft'); // 或者 homestayDraft ?
        if (draftData) {
            const parsed = JSON.parse(draftData);
            console.log('找到表单草稿数据:', parsed);

            // 合并草稿数据到表单
            Object.assign(homestayForm, parsed);

            // **处理草稿地址回显**
            if (parsed.provinceCode && parsed.cityCode) {
                const codes = [parsed.provinceCode, parsed.cityCode];
                if (parsed.districtCode) {
                    codes.push(parsed.districtCode);
                }
                selectedAreaCodes.value = codes; // 设置级联选择器的值
                console.log('草稿地址回显，设置 selectedAreaCodes:', codes);
            } else {
                selectedAreaCodes.value = []; // 清空
            }

            lastSaved.value = new Date(parsed._lastSaved || Date.now()); // 注意草稿保存的时间字段名
            ElMessage.success('已恢复上次编辑的草稿');
        } else {
            console.log('没有找到草稿数据');
            // 设置默认值
            homestayForm.status = 'INACTIVE';
            homestayForm.maxGuests = 1;
            homestayForm.minNights = 1;
        }
    } catch (e) {
        console.error('加载草稿失败:', e);
        ElMessage.warning('加载草稿失败，将使用默认值');
    }

    // 每3分钟自动保存一次草稿
    autoSaveInterval.value = window.setInterval(autoSaveDraft, 3 * 60 * 1000);
};

// 创建获取房源类型文本的函数 (使用动态获取的类型列表)
const getHomestayTypeText = (typeCode: string | undefined): string => {
    // console.log('getHomestayTypeText input typeCode:', typeCode);
    if (!typeCode) return '未知类型';

    // 从已获取的 homestayTypes 列表中查找
    // 假设 homestayTypes.value 是一个数组，每个元素有 code 和 name/label 属性
    // 注意：需要确认 homestayTypes.value 的确切结构
    const foundType = homestayTypes.value.find(t => t.code === typeCode || t.value === typeCode);

    if (foundType) {
        // 优先使用 name，其次 label，最后是 code 本身
        return foundType.name || foundType.label || typeCode;
    }

    // 如果在列表中找不到，返回未知类型或原始代码
    console.warn(`未能在 homestayTypes 列表中找到类型代码: ${typeCode}`);
    return '未知类型';
};

// 获取位置文本的辅助函数 (修改为使用 codeToText)
const getLocationText = () => {
    const parts = [];
    if (homestayForm.provinceCode && codeToText[homestayForm.provinceCode]) {
        parts.push(codeToText[homestayForm.provinceCode]);
    }
    if (homestayForm.cityCode && codeToText[homestayForm.cityCode]) {
        parts.push(codeToText[homestayForm.cityCode]);
    }
    // 区县编码可能是可选的，并且可能没有对应的文本（比如直辖市的市辖区可能只有两级）
    if (homestayForm.districtCode && codeToText[homestayForm.districtCode]) {
        parts.push(codeToText[homestayForm.districtCode]);
    }
    // 添加详细地址，确保它存在且不为空
    if (homestayForm.addressDetail && homestayForm.addressDetail.trim()) {
        parts.push(homestayForm.addressDetail.trim());
    }

    // 如果没有任何部分，返回默认文本
    if (parts.length === 0) {
        return '位置未指定';
    }

    // 用空格连接各个部分
    return parts.join(' ');
};

/**
 * 处理设施数据
 * @param amenitiesData 设施数据数组
 * @returns 标准化后的设施数据
 */
const processAmenities = (amenitiesData: any[]): { value: string, label?: string }[] => {
    console.log('原始设施数据:', amenitiesData);

    if (!amenitiesData || !Array.isArray(amenitiesData)) {
        console.warn('设施数据无效，返回空数组');
        return [];
    }

    const result = amenitiesData
        .map(item => {
            // 如果是字符串，转换为对象格式
            if (typeof item === 'string') {
                return { value: item.trim() };
            }

            // 如果是对象，提取value和label
            if (item && typeof item === 'object') {
                // 确保有value属性
                const value = item.value || item.code || '';
                if (!value) {
                    console.warn('跳过无效的设施项:', item);
                    return null;
                }

                return {
                    value: String(value).trim(),
                    label: item.label || item.name || value
                };
            }

            // 其他情况跳过
            console.warn('无法处理的设施项:', item);
            return null;
        })
        .filter(Boolean); // 过滤空值

    console.log('处理后的设施数据:', result);
    return result;
};

// 处理省份变更
const handleProvinceChange = async (value: string) => {
    console.log('省份变更:', value);
    homestayForm.cityCode = '';
    homestayForm.districtCode = '';
    cities.value = [];
    districts.value = [];

    if (!value) return;

    try {
        loadingLocationData.value = true;
        const response = await getCities(value);
        if (response?.data?.data) {
            cities.value = response.data.data;
            console.log(`加载城市成功, 数量: ${cities.value.length}`);
        }
    } catch (error) {
        console.error('加载城市数据失败:', error);
        ElMessage.error('加载城市数据失败');
    } finally {
        loadingLocationData.value = false;
    }
};

// 处理城市变更
const handleCityChange = async (value: string) => {
    console.log('城市变更:', value);
    homestayForm.districtCode = '';
    districts.value = [];

    if (!value) return;

    try {
        loadingLocationData.value = true;
        const response = await getDistricts(value);
        if (response?.data?.data) {
            districts.value = response.data.data;
            console.log(`加载区县成功, 数量: ${districts.value.length}`);
        }
    } catch (error) {
        console.error('加载区县数据失败:', error);
        ElMessage.error('加载区县数据失败');
    } finally {
        loadingLocationData.value = false;
    }
};

// 移除封面图片
const removeCoverImage = () => {
    homestayForm.coverImage = '';
    ElMessage.success('已移除封面图片');
};

// 计算表单完成度百分比
const formCompletionPercentage = computed(() => {
    let completedFields = 0;
    let totalFields = 0;

    // 必填字段
    const requiredFields = [
        'title', 'type', 'price', 'provinceCode', 'addressDetail', 'maxGuests', 'minNights', 'coverImage', 'description'
    ];

    // 计算必填字段完成情况
    for (const field of requiredFields) {
        totalFields++;
        if (homestayForm[field]) {
            completedFields++;
        }
    }

    // 额外字段
    if (homestayForm.amenities && homestayForm.amenities.length > 0) {
        completedFields++;
    }
    totalFields++;

    if (homestayForm.images && homestayForm.images.length > 0) {
        completedFields++;
    }
    totalFields++;

    // 计算百分比并四舍五入
    return Math.round((completedFields / totalFields) * 100);
});

// 添加验证上传文件的函数
const beforeUpload = (file: File) => {
    // 检查文件类型
    const isImage = file.type.startsWith('image/');
    if (!isImage) {
        ElMessage.error('只能上传图片文件!');
        return false;
    }

    // 检查文件大小
    const isLt5M = file.size / 1024 / 1024 < 5;
    if (!isLt5M) {
        ElMessage.error('图片大小不能超过5MB!');
        return false;
    }

    // 获取图片扩展名
    const extension = file.name.substring(file.name.lastIndexOf('.') + 1).toLowerCase();
    const validExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp'];

    // 验证扩展名
    if (!validExtensions.includes(extension)) {
        ElMessage.error(`仅支持以下格式: ${validExtensions.join(', ')}`);
        return false;
    }

    return true;
};

// 添加保存草稿的函数引用
const saveFormAsDraft = async () => {
    try {
        const processedData = preprocessFormData();
        processedData.status = 'DRAFT';

        let result;
        if (isEdit.value && homestayId.value) {
            // 更新现有房源
            result = await updateHomestayApi(homestayId.value, processedData);
        } else {
            // 创建新房源
            result = await createHomestayApi(processedData);
        }

        if (result && result.success) {
            lastSaved.value = new Date();
            console.log('表单已自动保存为草稿');

            // 如果是新创建的房源且有id返回，更新当前状态
            if (!isEdit.value && result.data && result.data.id) {
                console.log('新房源已创建，ID:', result.data.id);

                // 不直接修改computed值，而是使用路由导航
                router.replace(`/host/homestay/edit/${result.data.id}`);
            }
        }
    } catch (error) {
        console.error('自动保存草稿出错:', error);
        // 静默处理错误，不显示消息
    }
};

// 添加这个函数到onMounted之前的位置
// 检查当前用户是否有权限编辑此房源
const checkEditPermission = async (homestayId: number) => {
    try {
        console.log(`检查用户是否有权限编辑房源，ID：${homestayId}`);

        // 获取房源详情
        const { getHomestayById } = await import('@/api/homestay');
        const response = await getHomestayById(homestayId);

        if (!response || !response.data) {
            ElMessage.error('找不到此房源信息');
            router.push('/host/homestay');
            return false;
        }

        // 获取当前用户
        let currentUser;
        try {
            currentUser = JSON.parse(localStorage.getItem('userInfo') || '{}');
        } catch (e) {
            console.error('解析用户信息失败', e);
            ElMessage.error('无法验证用户信息，请重新登录');
            router.push('/login');
            return false;
        }

        // 检查当前用户是否是房源所有者
        if (response.data.ownerUsername !== currentUser.username) {
            console.error(`权限不匹配: 当前用户=${currentUser.username}, 房源所有者=${response.data.ownerUsername}`);
            ElMessage.error('您没有权限编辑此房源');
            router.push('/host/homestay');
            return false;
        }

        console.log('权限验证通过，允许编辑');
        return true;
    } catch (error) {
        console.error('权限检查失败:', error);
        ElMessage.error('权限验证失败，请重新登录');
        return false;
    }
};

// 修改onMounted函数的开头部分，添加权限检查
onMounted(async () => {
    // 初始化数据和选项
    await fetchOptions()

    // 如果是编辑模式则获取房源详情
    if (isEdit.value && homestayId.value) {
        // 检查编辑权限
        const hasPermission = await checkEditPermission(Number(homestayId.value));
        if (!hasPermission) {
            return; // 如果没有权限，停止加载
        }

        // 有权限，继续加载数据
        try {
            loading.value = true
            const { getHomestayById } = await import('@/api/homestay')
            const res = await getHomestayById(Number(homestayId.value))

            // 将API数据填充到表单
            Object.assign(homestayForm, res.data);

            // **处理地址回显**
            if (res.data.provinceCode && res.data.cityCode) {
                const codes = [res.data.provinceCode, res.data.cityCode];
                if (res.data.districtCode) {
                    codes.push(res.data.districtCode);
                }
                selectedAreaCodes.value = codes; // 设置级联选择器的值
            }

            // 检查设施数组是否为空，如果为空则尝试单独加载设施
            if (!homestayForm.amenities || homestayForm.amenities.length === 0) {
                console.log('房源设施数据为空，尝试单独加载设施');
                try {
                    // 导入设施相关API
                    const { getHomestayAmenitiesApi } = await import('@/api/amenities');

                    // 使用获取指定房源设施的API
                    const amenitiesResponse = await getHomestayAmenitiesApi(Number(homestayId.value));
                    console.log('单独加载设施响应:', amenitiesResponse);

                    if (amenitiesResponse.data && Array.isArray(amenitiesResponse.data)) {
                        console.log('单独加载设施成功:', amenitiesResponse.data);
                        homestayForm.amenities = amenitiesResponse.data;
                    } else if (amenitiesResponse.data && amenitiesResponse.data.data && Array.isArray(amenitiesResponse.data.data)) {
                        console.log('从data字段加载设施成功:', amenitiesResponse.data.data);
                        homestayForm.amenities = amenitiesResponse.data.data;
                    } else {
                        console.warn('单独加载设施返回的数据格式不正确');
                    }
                } catch (amenitiesError) {
                    console.error('单独加载设施失败:', amenitiesError);
                }
            }

            // --- 移除对 getProvinces 的残留调用 --- 
            // const provinceRes = await getProvinces(); 
            // console.log('省份数据响应:', provinceRes);
            // if (provinceRes?.data?.data && Array.isArray(provinceRes.data.data)) {
            //     provinces.value = provinceRes.data.data;
            //     console.log('加载省份成功:', provinces.value.length);
            // }
            // --- 移除结束 ---

            console.log('房源数据加载成功');
            loading.value = false;
            console.log('初始化完成，已设置自动保存');
            // 设置自动保存
            if (!autoSaveInterval.value) {
                autoSaveInterval.value = window.setInterval(autoSaveDraft, 3 * 60 * 1000);
            }

        } catch (error) {
            console.error('加载房源数据失败:', error);
            ElMessage.error('加载房源数据失败，请稍后再试');
            loading.value = false;
        }
    } else {
        // 新增模式：加载草稿或设置默认值
        console.log('新增模式，设置默认值');
        loadDraft();
    }

    // 设置自动保存 (这段逻辑可能重复了，可以考虑整合)
    if (!autoSaveInterval.value) {
        autoSaveInterval.value = window.setInterval(autoSaveDraft, 3 * 60 * 1000);
        console.log('已设置自动保存 (onMounted 末尾)');
    }
});

// 监听级联选择器的变化，更新 homestayForm 中的编码字段
watch(selectedAreaCodes, (newVal) => {
    if (newVal && newVal.length === 3) {
        homestayForm.provinceCode = newVal[0];
        homestayForm.cityCode = newVal[1];
        homestayForm.districtCode = newVal[2];
    } else {
        // 如果选择不完整或清空，也清空对应 code
        homestayForm.provinceCode = '';
        homestayForm.cityCode = '';
        homestayForm.districtCode = '';
    }
    // 手动触发表单项验证，确保清除 "请选择所在地区" 的提示
    // 使用 nextTick 确保在 DOM 更新和数据赋值完成后执行
    nextTick(() => {
        if (formRef.value) {
            formRef.value.validateField('provinceCode', () => { }); // 添加空回调避免控制台警告
        }
    });
});

// 级联选择器 change 事件处理 (可选，如果需要在选择后立即做某事)
const handleAreaChange = (value: string[]) => {
    console.log('地区选择变化:', value);
    // 这里通常不需要额外操作，watch 已经处理了 code 的更新
    // 如果需要根据编码获取文本，可以在这里使用 codeToText
};
</script>

<style scoped>
.homestay-form-container {
    padding: 20px;
    display: flex;
    gap: 20px;
}

.form-card {
    margin-bottom: 20px;
    flex: 1;
}

.preview-panel {
    width: 300px;
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    padding: 15px;
    position: sticky;
    top: 20px;
    max-height: calc(100vh - 40px);
    overflow-y: auto;
}

.card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.form-progress {
    margin-bottom: 30px;
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
    display: flex;
    align-items: center;
}

.section-subtitle {
    font-size: 14px;
    color: #909399;
    font-weight: normal;
    margin-left: 10px;
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
    height: 60px;
    line-height: 60px;
    margin: 0 auto;
}

.upload-text {
    color: #606266;
    font-size: 14px;
    text-align: center;
    margin-bottom: 15px;
}

.upload-image {
    width: 178px;
    height: 178px;
    display: block;
    object-fit: cover;
}

.form-step-actions {
    display: flex;
    justify-content: space-between;
    margin-top: 30px;
    padding-top: 20px;
    border-top: 1px solid #ebeef5;
    align-items: center;
}

.upload-wrapper {
    position: relative;
}

.preview-container {
    position: relative;
}

.preview-image {
    width: 240px;
    height: 160px;
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
    width: 160px;
    height: 160px;
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

.field-tips {
    margin-top: 8px;
    font-size: 13px;
    color: #606266;
    background-color: #f5f7fa;
    padding: 10px;
    border-radius: 4px;
}

.tip-title {
    font-weight: bold;
    margin-bottom: 5px;
}

.field-tips ul {
    margin: 5px 0;
    padding-left: 20px;
}

.examples {
    margin-top: 8px;
    font-style: italic;
}

.example-item {
    color: #409EFF;
}

.price-input-container {
    display: flex;
    align-items: center;
}

.price-symbol {
    font-size: 16px;
    margin-right: 5px;
    color: #606266;
}

.price-assistant {
    margin-top: 10px;
    font-size: 13px;
    background-color: #f0f9eb;
    padding: 10px;
    border-radius: 4px;
}

.market-insight {
    margin-bottom: 8px;
}

.insight-title {
    font-weight: bold;
    color: #67c23a;
}

.pricing-tips ul {
    margin: 5px 0;
    padding-left: 20px;
}

.guests-nights-container {
    display: flex;
    gap: 20px;
}

.half-width {
    width: 50%;
}

.selector-description {
    font-size: 12px;
    color: #909399;
    margin-top: 5px;
}

.field-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 5px;
}

.description-guide {
    margin-bottom: 15px;
    padding: 10px;
    background-color: #ecf5ff;
    border-radius: 4px;
    font-size: 14px;
    color: #409EFF;
}

.description-tips {
    margin-bottom: 8px;
    font-size: 13px;
    color: #606266;
}

.tips-title {
    font-weight: bold;
}

.form-completion {
    margin-top: 20px;
}

.completion-text {
    font-size: 14px;
    margin-bottom: 5px;
    color: #606266;
}

.preview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 15px;
    padding-bottom: 10px;
}

.preview-image img {
    width: 100%;
    height: 150px;
    object-fit: cover;
    border-radius: 8px;
}

.preview-info {
    padding: 10px 0;
}

.preview-info h4 {
    margin: 0 0 8px 0;
    font-size: 16px;
}

.preview-type {
    font-size: 13px;
    color: #909399;
    margin-bottom: 5px;
}

.preview-price {
    font-size: 16px;
    font-weight: bold;
    color: #f56c6c;
    margin-bottom: 8px;
}

.preview-location {
    font-size: 13px;
    color: #606266;
    margin-bottom: 8px;
}

.preview-highlights {
    font-size: 13px;
    color: #606266;
    background-color: #f5f7fa;
    padding: 8px;
    border-radius: 4px;
}

.amenities-tip {
    margin-bottom: 15px;
    font-size: 14px;
    color: #606266;
}

.upload-guide {
    background-color: #f5f7fa;
    padding: 10px;
    border-radius: 4px;
    margin-bottom: 15px;
}

.upload-guide ul {
    margin: 5px 0;
    padding-left: 20px;
}

.cover-upload-guide {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 20px;
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    width: 240px;
}

.image-upload-guide {
    margin-bottom: 15px;
    font-size: 14px;
    color: #606266;
}

.save-status {
    margin-left: auto;
    font-size: 12px;
    color: #909399;
    display: flex;
    align-items: center;
}

.save-status i {
    margin-right: 4px;
}

.select-empty-text {
    padding: 10px 0;
    text-align: center;
    color: #909399;
    font-size: 14px;
}

.upload-error-actions {
    display: flex;
    gap: 10px;
    margin-top: 10px;
}

.debug-info {
    margin-top: 10px;
    padding: 10px;
    background-color: #f5f7fa;
    border-radius: 4px;
    max-height: 300px;
    overflow-y: auto;
    font-family: monospace;
}

.debug-info pre {
    white-space: pre-wrap;
    word-break: break-all;
}

.debug-section h4 {
    margin-top: 0;
    margin-bottom: 8px;
    color: #606266;
    font-size: 14px;
}

.step-title {
    cursor: pointer;
    /* 添加鼠标指针样式 */
    user-select: none;
    /* 防止文本被选中 */
}

.step-title:hover {
    color: var(--el-color-primary);
    /* 鼠标悬停时改变颜色 */
}
</style>