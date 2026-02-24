import { ref, computed } from "vue";
import { getHomestayReviews, getHomestayReviewStats } from "@/api/review";
import type { Review, ReviewStatItem } from "@/types/homestay";

export function useReviews() {
  const reviews = ref<Review[]>([]);
  const reviewStats = ref<ReviewStatItem[]>([]);
  const totalReviewCount = ref(0);
  const reviewsPage = ref(1);
  const reviewsPageSize = 5;
  const loading = ref(false);

  const formattedRating = computed(() => {
    // 优先使用评价统计中的平均评分
    if (reviewStats.value.length > 0) {
      const avgRating =
        reviewStats.value.reduce((sum, stat) => sum + stat.score, 0) /
        reviewStats.value.length;
      return avgRating.toFixed(1);
    }

    // 如果有评价但没有评价统计，根据评价数量给出默认评分
    if (totalReviewCount.value > 0 || reviews.value.length > 0) {
      if (reviews.value.length > 0) {
        const ratingsWithScores = reviews.value.filter(
          (review) => review.rating && review.rating > 0
        );
        if (ratingsWithScores.length > 0) {
          const avgRating =
            ratingsWithScores.reduce((sum, review) => sum + review.rating, 0) /
            ratingsWithScores.length;
          return avgRating.toFixed(1);
        }
      }
      return "4.5"; // 默认好评评分
    }

    return "暂无评分";
  });

  const formattedReviewCount = computed(() => {
    if (totalReviewCount.value > 0) {
      return totalReviewCount.value;
    }
    if (reviews.value.length > 0) {
      return reviews.value.length;
    }
    return 0;
  });

  const numericRating = computed(() => {
    const rating = formattedRating.value;
    if (rating === "暂无评分") {
      return 0;
    }
    return parseFloat(rating);
  });

  const fetchReviewsAndStats = async (homestayId: number) => {
    loading.value = true;
    try {
      // 获取评价列表
      const reviewsResponse = await getHomestayReviews(homestayId, {
        page: reviewsPage.value - 1,
        size: reviewsPageSize,
      });

      if (reviewsResponse?.data?.content) {
        if (reviewsPage.value === 1) {
          reviews.value = reviewsResponse.data.content;
        } else {
          reviews.value.push(...reviewsResponse.data.content);
        }

        if (reviewsResponse.data.totalElements !== undefined) {
          totalReviewCount.value = reviewsResponse.data.totalElements;
        }
      }

      // 获取评价统计
      const statsResponse = await getHomestayReviewStats(homestayId);
      if (statsResponse?.data) {
        reviewStats.value = [
          { name: "清洁度", score: statsResponse.data.cleanlinessRating || 0 },
          { name: "准确性", score: statsResponse.data.accuracyRating || 0 },
          { name: "沟通", score: statsResponse.data.communicationRating || 0 },
          { name: "位置", score: statsResponse.data.locationRating || 0 },
          { name: "入住", score: statsResponse.data.checkInRating || 0 },
          { name: "性价比", score: statsResponse.data.valueRating || 0 },
        ].filter((stat) => stat.score > 0);
      }
    } catch (error) {
      console.error("获取评价数据失败:", error);
    } finally {
      loading.value = false;
    }
  };

  const loadMoreReviews = (homestayId: number) => {
    if (reviews.value.length < totalReviewCount.value) {
      reviewsPage.value++;
      fetchReviewsAndStats(homestayId);
      return true;
    }
    return false;
  };

  const resetReviews = () => {
    reviews.value = [];
    reviewStats.value = [];
    totalReviewCount.value = 0;
    reviewsPage.value = 1;
    loading.value = false;
  };

  return {
    reviews,
    reviewStats,
    totalReviewCount,
    loading,
    formattedRating,
    formattedReviewCount,
    numericRating,
    fetchReviewsAndStats,
    loadMoreReviews,
    resetReviews,
  };
}
