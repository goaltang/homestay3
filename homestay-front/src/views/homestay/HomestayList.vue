<template>
    <div class="homestay-list">
        <!-- 页面标题 -->
        <div class="header">
            <h2>房源列表</h2>
        </div>

        <!-- 筛选条件 -->
        <el-card shadow="never" class="filter-card">
            <el-form :inline="true" :model="filterForm" class="filter-form">
                <el-form-item label="房源类型">
                    <el-select v-model="filterForm.type" placeholder="全部类型" clearable>
                        <el-option label="整套" value="ENTIRE" />
                        <el-option label="独立房间" value="PRIVATE" />
                    </el-select>
                </el-form-item>
                <el-form-item label="价格区间">
                    <el-input-number v-model="filterForm.minPrice" :min="0" placeholder="最低价" />
                    <span class="separator">-</span>
                    <el-input-number v-model="filterForm.maxPrice" :min="0" placeholder="最高价" />
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" @click="handleFilter">筛选</el-button>
                    <el-button @click="resetFilter">重置</el-button>
                </el-form-item>
            </el-form>
        </el-card>

        <!-- 房源列表展示 -->
        <div class="homestay-grid" v-loading="loading">
            <el-row :gutter="20">
                <el-col :span="6" v-for="homestay in homestays" :key="homestay.id">
                    <el-card class="homestay-card" shadow="hover" @click="handleHomestayClick(homestay.id)">
                        <el-image :src="homestay.coverImage" class="cover-image" fit="cover" />
                        <div class="homestay-info">
                            <h3 class="title">{{ homestay.title }}</h3>
                            <div class="type">
                                <el-tag size="small">{{ homestay.type === 'ENTIRE' ? '整套' : '独立房间' }}</el-tag>
                            </div>
                            <div class="price">¥{{ homestay.price }}/晚</div>
                        </div>
                    </el-card>
                </el-col>
            </el-row>
        </div>

        <!-- 分页器 -->
        <div class="pagination">
            <el-pagination v-model:currentPage="currentPage" v-model:page-size="pageSize" :page-sizes="[12, 24, 36, 48]"
                layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
                @current-change="handleCurrentChange" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import type { HomestayBasic } from '@/types';

const router = useRouter();
const loading = ref(false);
const homestays = ref<HomestayBasic[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(12);

// 筛选表单
const filterForm = ref({
    type: '',
    minPrice: null as number | null,
    maxPrice: null as number | null
});

// 获取房源列表数据
const fetchHomestays = async () => {
    // TODO: 实现获取房源列表的API调用
};

// 筛选处理
const handleFilter = () => {
    currentPage.value = 1;
    fetchHomestays();
};

// 重置筛选条件
const resetFilter = () => {
    filterForm.value = {
        type: '',
        minPrice: null,
        maxPrice: null
    };
    handleFilter();
};

// 页码变化处理
const handleCurrentChange = (page: number) => {
    currentPage.value = page;
    fetchHomestays();
};

// 每页数量变化处理
const handleSizeChange = (size: number) => {
    pageSize.value = size;
    fetchHomestays();
};

// 点击房源卡片跳转到详情页
const handleHomestayClick = (id: number) => {
    router.push(`/homestays/${id}`);
};

onMounted(() => {
    fetchHomestays();
});
</script>

<style scoped>
.homestay-list {
    padding: 20px;
}

.header {
    margin-bottom: 20px;
}

.filter-card {
    margin-bottom: 20px;
}

.filter-form {
    display: flex;
    flex-wrap: wrap;
}

.separator {
    margin: 0 8px;
}

.homestay-grid {
    margin-bottom: 20px;
}

.homestay-card {
    margin-bottom: 20px;
    cursor: pointer;
}

.cover-image {
    width: 100%;
    height: 200px;
}

.homestay-info {
    padding: 10px;
}

.title {
    margin: 0 0 10px;
    font-size: 16px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.type {
    margin-bottom: 10px;
}

.price {
    color: #f56c6c;
    font-size: 18px;
    font-weight: bold;
}

.pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
}
</style>