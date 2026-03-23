<template>
    <div class="homestay-list-container">
        <!-- 页面标题 -->
        <div class="page-header">
            <h1 class="page-title">{{ pageTitle }}</h1>
            <div v-if="route.query.keyword || route.query.featured || route.query.status" class="breadcrumb">
                <el-button link @click="router.push('/')">返回首页</el-button>
            </div>
        </div>

        <!-- 搜索栏 -->
        <SearchBar @search="handleSearchBarSearch" @reset="handleSearchBarReset" :loading="loading" />

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-container">
            <el-skeleton :rows="3" animated />
            <el-skeleton :rows="3" animated />
            <el-skeleton :rows="3" animated />
        </div>

        <!-- 空状态 -->
        <div v-else-if="homestays.length === 0" class="empty-container">
            <el-empty description="没有找到符合条件的民宿" />
        </div>

        <!-- 民宿列表 -->
        <div v-else class="homestay-list">
            <el-row :gutter="20">
                <el-col v-for="homestay in homestays" :key="homestay.id" :xs="24" :sm="12" :md="8" :lg="6">
                    <HomestayCard :homestay="homestay" :homestay-types="homestayTypes" />
                </el-col>
            </el-row>
        </div>

        <div class="pagination-container">
            <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total"
                :page-sizes="[12, 24, 36, 48]" layout="total, sizes, prev, pager, next" @size-change="handleSizeChange"
                @current-change="handleCurrentChange" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { useFavoritesStore } from '@/stores/favorites';
import { useUserStore } from '@/stores/user';
import request from '@/utils/request';
import { getActiveHomestays, getHomestayTypes, searchHomestays, type HomestaySearchRequest } from '@/api/homestay';
import { getPopularHomestaysPage, getRecommendedHomestaysPage, getPersonalizedRecommendations } from '@/api/recommendation';
import HomestayCard from '@/components/homestay/HomestayCard.vue';
import SearchBar from '@/components/SearchBar.vue';
import { codeToText } from 'element-china-area-data';

// 类型定义
interface Homestay {
    id: number;
    title: string;
    description?: string;
    pricePerNight: number;
    price?: string;
    maxGuests: number;
    bedrooms: number;
    beds: number;
    bathrooms: number;
    amenities: string[];
    images: string[];
    rating?: number;
    reviewCount?: number;
    type: string;
    status: string;
    featured?: boolean;
    propertyType?: string;
    distanceFromCenter?: number;
    latitude: number;
    longitude: number;
    hostName: string;
    hostId: number;
    coverImage?: string;
    provinceCode?: string;
    cityCode?: string;
    districtCode?: string;
    addressDetail?: string;
}

const router = useRouter();
const route = useRoute();
const favoritesStore = useFavoritesStore();
const userStore = useUserStore();

// 页面标题计算
const pageTitle = computed(() => {
    if (route.query.type === 'popular') return '热门民宿';
    if (route.query.featured === 'true') return '推荐民宿';
    if (route.query.personalized === 'true') return '猜你喜欢';
    if (route.query.status === 'ACTIVE') return '最新上架';
    if (route.query.keyword) return `"${route.query.keyword}" 的搜索结果`;
    if (route.query.region) {
        // 将地区代码转换为地名
        const regionString = route.query.region as string;
        if (regionString.includes(',')) {
            // 多级地区：省,市,区
            const regionCodes = regionString.split(',');
            const regionNames = regionCodes.map(code => codeToText[code] || code).filter(Boolean);
            return `${regionNames.join('')} 的民宿`;
        } else {
            // 单个地区代码
            const regionName = codeToText[regionString] || regionString;
            return `${regionName} 的民宿`;
        }
    }
    return '全部民宿';
});

// 搜索相关数据
const searchParams = ref({
    keyword: '',
    selectedRegion: [] as string[],
    checkIn: null as string | null,
    checkOut: null as string | null,
    guestCount: 1
});

// 分页相关
const currentPage = ref(1);
const pageSize = ref(12);
const total = ref(0);

// 加载状态
const loading = ref(false);

// 房源列表数据
const homestays = ref<Homestay[]>([]);

// 搜索结果缓存（用于前端分页）
const allSearchResults = ref<Homestay[]>([]);

// 房源类型数据
const homestayTypes = ref<any[]>([]);

// 检查是否有搜索条件
const hasSearchConditions = () => {
    return route.query.keyword ||
        route.query.region ||
        route.query.checkIn ||
        route.query.checkOut ||
        route.query.guestCount ||
        route.query.minPrice ||
        route.query.maxPrice ||
        route.query.amenities ||
        searchParams.value.keyword ||
        searchParams.value.selectedRegion.length > 0 ||
        searchParams.value.checkIn ||
        searchParams.value.checkOut ||
        searchParams.value.guestCount > 1;
};

// 数据加载方法
const loadHomestays = async () => {
    loading.value = true;
    console.log('loadHomestays 调用 - 路由参数:', route.query);
    console.log('loadHomestays 调用 - 搜索参数:', searchParams.value);
    console.log('是否有搜索条件:', hasSearchConditions());

    try {
        let response;

        // 优先检查搜索标记或搜索条件
        if (route.query.search === 'true' || hasSearchConditions()) {
            // 搜索模式：使用搜索API
            // 根据搜索条件加载 - 参考首页的实现方式
            const searchRequest: any = {
                keyword: route.query.keyword || null,
                propertyType: route.query.type || null,
                minPrice: route.query.minPrice ? Number(route.query.minPrice) : null,
                maxPrice: route.query.maxPrice ? Number(route.query.maxPrice) : null,
                requiredAmenities: route.query.amenities ? (route.query.amenities as string).split(',') : null,
                page: currentPage.value - 1,  // 后端分页从0开始
                size: pageSize.value,
                sortBy: 'id',
                sortDirection: 'desc'
            };

            // 处理地区选择 - 参考首页的处理方式
            if (route.query.region) {
                const regionString = route.query.region as string;
                const regionCodes = regionString.includes(',') ? regionString.split(',') : [regionString];

                if (regionCodes.length >= 1) {
                    searchRequest.provinceCode = regionCodes[0];
                }
                if (regionCodes.length >= 2) {
                    searchRequest.cityCode = regionCodes[1];
                }
                if (regionCodes.length >= 3) {
                    searchRequest.districtCode = regionCodes[2];
                }
            }

            // 处理入住和退房日期
            if (route.query.checkIn) {
                searchRequest.checkInDate = route.query.checkIn as string;
            }
            if (route.query.checkOut) {
                searchRequest.checkOutDate = route.query.checkOut as string;
            }

            // 处理客人数量
            if (route.query.guestCount) {
                searchRequest.minGuests = Number(route.query.guestCount);
            }

            console.log('发送搜索请求 (参考首页实现):', searchRequest);
            response = await request.post('/api/homestays/search', searchRequest);
            console.log('搜索响应:', response);
        } else if (route.query.type === 'popular') {
            // 加载热门民宿（使用分页API）
            response = await getPopularHomestaysPage({
                page: currentPage.value - 1,
                size: pageSize.value
            });
        } else if (route.query.featured === 'true') {
            // 加载推荐民宿（使用分页API）
            response = await getRecommendedHomestaysPage({
                page: currentPage.value - 1,
                size: pageSize.value
            });
        } else if (route.query.personalized === 'true') {
            // 加载个性化推荐民宿
            // 注意：个性化推荐目前没有分页API，使用简单版本
            response = await getPersonalizedRecommendations(
                Number(userStore.userInfo?.id || 0),
                50 // 获取更多结果用于前端分页
            );
        } else if (route.query.status === 'ACTIVE') {
            // 加载最新上架 - 使用正确的API函数
            response = await getActiveHomestays({
                page: currentPage.value - 1,  // 后端分页从0开始
                size: pageSize.value
            });
        } else {
            // 默认情况：加载所有活跃房源
            response = await getActiveHomestays({
                page: currentPage.value - 1,
                size: pageSize.value
            });
        }

        // 处理不同API返回的数据结构
        if (route.query.search === 'true' || hasSearchConditions()) {
            // 搜索API返回数组，需要前端分页
            const allResults = response.data || [];

            // 只有在新搜索时才重新缓存，分页时使用缓存
            if (currentPage.value === 1 || allSearchResults.value.length === 0) {
                allSearchResults.value = allResults;
            }

            total.value = allSearchResults.value.length;

            // 前端分页
            const startIndex = (currentPage.value - 1) * pageSize.value;
            const endIndex = startIndex + pageSize.value;
            homestays.value = allSearchResults.value.slice(startIndex, endIndex);

            console.log(`搜索结果处理: 总共${allSearchResults.value.length}条，当前页显示${homestays.value.length}条，页码${currentPage.value}`);
        } else if (route.query.type === 'popular' || route.query.featured === 'true') {
            // 推荐API现在返回分页对象
            if (response.data && response.data.content) {
                homestays.value = response.data.content;
                total.value = response.data.totalElements || 0;
                console.log(`推荐民宿分页处理: 第${currentPage.value}页，共${total.value}条，当前显示${homestays.value.length}条`);
            } else {
                homestays.value = response.data || [];
                total.value = homestays.value.length;
            }
        } else if (route.query.personalized === 'true') {
            // 个性化推荐返回数组，需要前端分页
            const allResults = response.data || [];

            // 缓存全部结果
            if (currentPage.value === 1) {
                allSearchResults.value = allResults;
            }

            total.value = allSearchResults.value.length;

            // 前端分页
            const startIndex = (currentPage.value - 1) * pageSize.value;
            const endIndex = startIndex + pageSize.value;
            homestays.value = allSearchResults.value.slice(startIndex, endIndex);

            console.log(`个性化推荐分页处理: 总共${total.value}条，当前页显示${homestays.value.length}条，页码${currentPage.value}`);
        } else if (route.query.status === 'ACTIVE') {
            // 分页API返回分页对象 - 使用新的数据格式
            if (response.data && response.data.data) {
                homestays.value = response.data.data;
                total.value = response.data.total || 0;
                console.log(`ACTIVE状态分页处理: 第${currentPage.value}页，共${total.value}条，当前显示${homestays.value.length}条`);
            } else {
                homestays.value = response.data || [];
                total.value = homestays.value.length;
            }
        } else {
            // 默认情况：分页API - 使用新的数据格式
            if (response.data && response.data.data) {
                homestays.value = response.data.data;
                total.value = response.data.total || 0;
                console.log(`默认分页处理: 第${currentPage.value}页，共${total.value}条，当前显示${homestays.value.length}条`);
            } else {
                homestays.value = response.data || [];
                total.value = homestays.value.length;
            }
        }

    } catch (error) {
        console.error('加载民宿数据失败:', error);

        // 使用模拟数据
        const mockData: Homestay[] = [
            {
                id: 1,
                title: '精美湖畔小屋',
                description: '美丽的湖畔小屋，宁静舒适',
                pricePerNight: 488,
                maxGuests: 2,
                bedrooms: 1,
                beds: 1,
                bathrooms: 1,
                amenities: ['WiFi', '空调', '厨房', '停车位'],
                images: ['https://picsum.photos/800/600?random=1'],
                rating: 4.9,
                reviewCount: 128,
                type: 'SHARED',
                status: 'ACTIVE',
                featured: true,
                propertyType: '小屋',
                distanceFromCenter: 1.5,
                latitude: 30.2636,
                longitude: 120.1686,
                hostName: '张三',
                hostId: 1001,
                coverImage: 'https://picsum.photos/800/600?random=1',
                provinceCode: '330000',
                cityCode: '330100',
                districtCode: '330106',
                addressDetail: '西湖区某街道'
            },
            {
                id: 2,
                title: '海景公寓',
                description: '一线海景，舒适公寓',
                pricePerNight: 688,
                maxGuests: 4,
                bedrooms: 2,
                beds: 2,
                bathrooms: 1,
                amenities: ['WiFi', '空调', '厨房', '停车位', '游泳池'],
                images: ['https://picsum.photos/800/600?random=2'],
                rating: 4.8,
                reviewCount: 92,
                type: 'ENTIRE',
                status: 'ACTIVE',
                featured: true,
                propertyType: '公寓',
                distanceFromCenter: 2.0,
                latitude: 24.4459,
                longitude: 118.0657,
                hostName: '李四',
                hostId: 1002,
                coverImage: 'https://picsum.photos/800/600?random=2',
                provinceCode: '350000',
                cityCode: '350200',
                districtCode: '350203',
                addressDetail: '思明区某街道'
            },
            {
                id: 3,
                title: '山间木屋',
                description: '清新的山间小木屋，远离城市喧嚣',
                pricePerNight: 588,
                maxGuests: 3,
                bedrooms: 1,
                beds: 2,
                bathrooms: 1,
                amenities: ['WiFi', '空调', '厨房', '停车位', '烧烤架'],
                images: ['https://picsum.photos/800/600?random=3'],
                rating: 4.7,
                reviewCount: 78,
                type: 'PRIVATE',
                status: 'ACTIVE',
                featured: false,
                propertyType: '木屋',
                distanceFromCenter: 5.0,
                latitude: 30.5603,
                longitude: 119.8808,
                hostName: '王五',
                hostId: 1003,
                coverImage: 'https://picsum.photos/800/600?random=3',
                provinceCode: '330000',
                cityCode: '330500',
                districtCode: '330523',
                addressDetail: '德清县某街道'
            }
        ];

        homestays.value = mockData;
        total.value = mockData.length;
    } finally {
        loading.value = false;
    }
};

// 获取房源类型数据
const loadHomestayTypes = async () => {
    try {
        const response = await getHomestayTypes();
        homestayTypes.value = response || [];
    } catch (error) {
        console.error('获取房源类型失败:', error);
    }
};

// 搜索栏搜索处理
const handleSearchBarSearch = (params: any) => {
    console.log('执行搜索，参数:', params);

    // 清除所有特殊标记，强制进入搜索模式
    const newQuery: any = {
        // 移除特殊标记
        featured: undefined,
        type: undefined,
        status: undefined,
        // 设置搜索参数
        keyword: params.keyword || undefined,
        region: params.selectedRegion?.length ? params.selectedRegion.join(',') : undefined,
        checkIn: params.checkIn || undefined,
        checkOut: params.checkOut || undefined,
        guestCount: params.guestCount && params.guestCount > 1 ? params.guestCount : undefined,
        // 添加搜索标记
        search: 'true'
    };

    // 更新本地搜索参数
    searchParams.value.keyword = params.keyword || '';
    searchParams.value.selectedRegion = params.selectedRegion || [];
    searchParams.value.checkIn = params.checkIn || null;
    searchParams.value.checkOut = params.checkOut || null;
    searchParams.value.guestCount = params.guestCount || 1;

    // 重置分页
    currentPage.value = 1;

    console.log('设置新的路由参数:', newQuery);

    // 更新路由并立即加载数据
    router.replace({
        path: '/homestays',
        query: newQuery
    }).then(() => {
        console.log('路由更新完成，立即加载搜索数据');
        loadHomestays();
    });
};

// 搜索栏重置处理
const handleSearchBarReset = () => {
    console.log('重置搜索');

    // 重置搜索参数
    searchParams.value = {
        keyword: '',
        selectedRegion: [],
        checkIn: null,
        checkOut: null,
        guestCount: 1
    };

    // 清空搜索缓存
    allSearchResults.value = [];

    // 重置分页
    currentPage.value = 1;

    // 清空路由查询参数并加载数据
    router.replace({ path: '/homestays' });

    // 立即加载数据
    loadHomestays();
};

// 这些方法已不再需要，因为搜索功能已集成到 SearchBar 组件中

// 分页处理
const handleSizeChange = (val: number) => {
    pageSize.value = val;
    currentPage.value = 1; // 重置到第一页
    loadHomestays();
};

const handleCurrentChange = (val: number) => {
    currentPage.value = val;
    console.log('页码变化到:', val);

    // 搜索模式下只需要重新分页，不重新请求；其他模式需要重新请求数据
    if (route.query.search === 'true' || hasSearchConditions()) {
        // 搜索模式：仅重新分页，使用缓存数据
        const startIndex = (currentPage.value - 1) * pageSize.value;
        const endIndex = startIndex + pageSize.value;
        homestays.value = allSearchResults.value.slice(startIndex, endIndex);
        console.log(`搜索分页: 第${currentPage.value}页，显示${homestays.value.length}条`);
    } else {
        // 其他模式（包括推荐和热门）：重新请求数据
        loadHomestays();
    }
};

// 初始化路由参数
const initializeFromRoute = () => {
    if (route.query.keyword) {
        searchParams.value.keyword = route.query.keyword as string;
    }
    if (route.query.region) {
        searchParams.value.selectedRegion = (route.query.region as string).split(',');
    }
    if (route.query.checkIn) {
        searchParams.value.checkIn = route.query.checkIn as string;
    }
    if (route.query.checkOut) {
        searchParams.value.checkOut = route.query.checkOut as string;
    }
    if (route.query.guestCount) {
        searchParams.value.guestCount = Number(route.query.guestCount);
    }
};

// 监听路由变化
watch(() => route.query, (newQuery, oldQuery) => {
    console.log('路由参数变化:', { newQuery, oldQuery });
    // 只有当查询参数真正变化时才重新加载
    if (JSON.stringify(newQuery) !== JSON.stringify(oldQuery)) {
        initializeFromRoute();
        loadHomestays();
    }
}, { deep: true });

// 初始化
onMounted(() => {
    initializeFromRoute();
    loadHomestayTypes();
    loadHomestays();
    console.log('路由查询参数:', route.query);
});
</script>

<style scoped>
.homestay-list-container {
    padding: 20px;
    max-width: 1200px;
    margin: 0 auto;
}

.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #e4e7ed;
}

.page-title {
    margin: 0;
    font-size: 28px;
    font-weight: 600;
    color: #222;
}

.breadcrumb {
    color: #717171;
}

/* 过滤相关样式已移除，现在使用 SearchBar 组件 */

.loading-container {
    margin: 40px 0;
}

.empty-container {
    margin: 60px 0;
    text-align: center;
}

.homestay-list {
    margin-top: 20px;
}

/* HomestayCard 组件已包含所有卡片样式 */

.pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: center;
}
</style>