<template>
    <div class="homestay-list-container">
        <el-card class="filter-card">
            <div class="filter-section">
                <el-form :inline="true" :model="filterForm" class="filter-form">
                    <el-form-item label="关键词">
                        <el-input v-model="filterForm.keyword" placeholder="搜索房源名称" />
                    </el-form-item>
                    <el-form-item label="价格区间">
                        <div class="price-range-container">
                            <el-input-number v-model="filterForm.minPrice" :min="0" placeholder="最低价" :controls="false"
                                class="price-input" />
                            <span class="separator">-</span>
                            <el-input-number v-model="filterForm.maxPrice" :min="0" placeholder="最高价" :controls="false"
                                class="price-input" />
                        </div>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="handleSearch">搜索</el-button>
                        <el-button @click="resetFilter">重置</el-button>
                    </el-form-item>
                </el-form>
            </div>
        </el-card>

        <div class="homestay-list">
            <el-row :gutter="20">
                <el-col v-for="homestay in homestays" :key="homestay.id" :xs="24" :sm="12" :md="8" :lg="6">
                    <el-card class="homestay-card" :body-style="{ padding: '0px' }" @click="viewDetail(homestay.id)">
                        <img :src="homestay.coverImage || defaultImage" class="image" />
                        <div class="homestay-info">
                            <h3>{{ homestay.name }}</h3>
                            <div class="price">¥{{ homestay.price }}/晚</div>
                            <div class="location">
                                <el-icon>
                                    <Location />
                                </el-icon>
                                {{ homestay.location }}
                            </div>
                            <div class="tags">
                                <el-tag v-for="tag in homestay.tags" :key="tag" size="small">{{ tag }}</el-tag>
                            </div>
                        </div>
                    </el-card>
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
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Location } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const router = useRouter();
const defaultImage = 'https://via.placeholder.com/300x200';

// 过滤表单
const filterForm = ref({
    keyword: '',
    minPrice: null,
    maxPrice: null
});

// 分页相关
const currentPage = ref(1);
const pageSize = ref(12);
const total = ref(0);

// 房源列表数据
const homestays = ref([
    {
        id: 1,
        name: '示例房源1',
        price: 299,
        location: '北京市朝阳区',
        coverImage: 'https://via.placeholder.com/300x200',
        tags: ['独立卫浴', 'WiFi']
    },
    // 更多示例数据...
]);

// 搜索处理
const handleSearch = () => {
    // TODO: 实现搜索逻辑
    ElMessage.info('搜索功能开发中');
};

// 重置过滤器
const resetFilter = () => {
    filterForm.value = {
        keyword: '',
        minPrice: null,
        maxPrice: null
    };
};

// 查看详情
const viewDetail = (id: number) => {
    router.push(`/homestays/${id}`);
};

// 分页处理
const handleSizeChange = (val: number) => {
    pageSize.value = val;
    // TODO: 重新加载数据
};

const handleCurrentChange = (val: number) => {
    currentPage.value = val;
    // TODO: 重新加载数据
};

// 初始化
onMounted(() => {
    // TODO: 加载初始数据
});
</script>

<style scoped>
.homestay-list-container {
    padding: 20px;
    max-width: 1200px;
    margin: 0 auto;
}

.filter-card {
    margin-bottom: 20px;
}

.filter-form {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
}

.price-range-container {
    display: flex;
    align-items: center;
}

.price-input {
    width: 100px;
}

.separator {
    margin: 0 8px;
}

.homestay-list {
    margin-top: 20px;
}

.homestay-card {
    margin-bottom: 20px;
    cursor: pointer;
    transition: transform 0.3s;
}

.homestay-card:hover {
    transform: translateY(-5px);
}

.image {
    width: 100%;
    height: 200px;
    object-fit: cover;
}

.homestay-info {
    padding: 14px;
}

.homestay-info h3 {
    margin: 0;
    font-size: 16px;
    color: #333;
}

.price {
    margin-top: 8px;
    color: #f56c6c;
    font-size: 18px;
    font-weight: bold;
}

.location {
    margin-top: 8px;
    color: #666;
    font-size: 14px;
    display: flex;
    align-items: center;
    gap: 4px;
}

.tags {
    margin-top: 8px;
    display: flex;
    gap: 4px;
    flex-wrap: wrap;
}

.pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: center;
}
</style>