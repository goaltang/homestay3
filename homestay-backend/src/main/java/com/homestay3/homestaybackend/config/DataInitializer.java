package com.homestay3.homestaybackend.config;

import com.homestay3.homestaybackend.model.Homestay;
import com.homestay3.homestaybackend.repository.HomestayRepository;
import com.homestay3.homestaybackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final HomestayRepository homestayRepository;
    private final UserRepository userRepository;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            // 只有在没有数据时才初始化
            if (homestayRepository.count() == 0) {
                log.info("初始化民宿数据...");
                initHomestays();
                log.info("民宿数据初始化完成");
            }
        };
    }

    private void initHomestays() {
        // 树屋1
        Homestay treehouse1 = Homestay.builder()
                .title("浪漫树屋 - 森林中的宁静")
                .description("这座树屋位于茂密的森林中，提供了一个远离城市喧嚣的宁静环境。享受大自然的声音，在树梢间放松身心。")
                .location("云南省大理市")
                .city("大理")
                .country("中国")
                .pricePerNight(new BigDecimal("688"))
                .maxGuests(2)
                .bedrooms(1)
                .beds(1)
                .bathrooms(1)
                .amenities(Arrays.asList("WiFi", "空调", "厨房", "洗衣机", "阳台"))
                .images(Arrays.asList(
                        "/images/treehouse1_1.jpg",
                        "/images/treehouse1_2.jpg",
                        "/images/treehouse1_3.jpg"
                ))
                .rating(4.92)
                .reviewCount(128)
                .latitude(25.6065)
                .longitude(100.2679)
                .hostName("李明")
                .featured(true)
                .propertyType("树屋")
                .distanceFromCenter(5.2)
                .build();

        // 树屋2
        Homestay treehouse2 = Homestay.builder()
                .title("山顶树屋 - 俯瞰美景")
                .description("位于山顶的树屋，可以俯瞰整个山谷的美景。晚上可以看星星，早上被鸟鸣声唤醒。")
                .location("浙江省杭州市临安区")
                .city("杭州")
                .country("中国")
                .pricePerNight(new BigDecimal("788"))
                .maxGuests(4)
                .bedrooms(2)
                .beds(2)
                .bathrooms(1)
                .amenities(Arrays.asList("WiFi", "空调", "烧烤架", "观景台", "停车位"))
                .images(Arrays.asList(
                        "/images/treehouse2_1.jpg",
                        "/images/treehouse2_2.jpg",
                        "/images/treehouse2_3.jpg"
                ))
                .rating(4.85)
                .reviewCount(95)
                .latitude(30.2336)
                .longitude(119.7242)
                .hostName("张伟")
                .featured(true)
                .propertyType("树屋")
                .distanceFromCenter(12.5)
                .build();

        // 海景房
        Homestay seasideHouse = Homestay.builder()
                .title("蓝色海岸 - 无敌海景房")
                .description("直面大海的豪华公寓，宽敞的阳台可以欣赏日出日落。步行5分钟即可到达沙滩。")
                .location("海南省三亚市亚龙湾")
                .city("三亚")
                .country("中国")
                .pricePerNight(new BigDecimal("1288"))
                .maxGuests(6)
                .bedrooms(3)
                .beds(3)
                .bathrooms(2)
                .amenities(Arrays.asList("WiFi", "空调", "厨房", "游泳池", "健身房", "停车位"))
                .images(Arrays.asList(
                        "/images/seaside1_1.jpg",
                        "/images/seaside1_2.jpg",
                        "/images/seaside1_3.jpg"
                ))
                .rating(4.95)
                .reviewCount(210)
                .latitude(18.2208)
                .longitude(109.6313)
                .hostName("王芳")
                .featured(true)
                .propertyType("海景房")
                .distanceFromCenter(3.8)
                .build();

        // 小木屋
        Homestay cabin = Homestay.builder()
                .title("雪山小木屋 - 冬季滑雪胜地")
                .description("位于雪山脚下的温馨小木屋，冬季是滑雪爱好者的天堂，夏季可以徒步登山。")
                .location("吉林省长白山")
                .city("白山")
                .country("中国")
                .pricePerNight(new BigDecimal("588"))
                .maxGuests(4)
                .bedrooms(2)
                .beds(2)
                .bathrooms(1)
                .amenities(Arrays.asList("WiFi", "暖气", "壁炉", "厨房", "烧烤架"))
                .images(Arrays.asList(
                        "/images/cabin1_1.jpg",
                        "/images/cabin1_2.jpg",
                        "/images/cabin1_3.jpg"
                ))
                .rating(4.88)
                .reviewCount(156)
                .latitude(42.1354)
                .longitude(128.1108)
                .hostName("赵强")
                .featured(false)
                .propertyType("小木屋")
                .distanceFromCenter(15.3)
                .build();

        // 湖边别墅
        Homestay lakehouse = Homestay.builder()
                .title("湖畔别墅 - 宁静度假")
                .description("坐落在湖边的现代别墅，拥有私人码头和花园。可以划船、钓鱼或者在湖中游泳。")
                .location("江苏省苏州市太湖")
                .city("苏州")
                .country("中国")
                .pricePerNight(new BigDecimal("1688"))
                .maxGuests(8)
                .bedrooms(4)
                .beds(4)
                .bathrooms(3)
                .amenities(Arrays.asList("WiFi", "空调", "厨房", "花园", "烧烤架", "停车位", "私人码头"))
                .images(Arrays.asList(
                        "/images/lakehouse1_1.jpg",
                        "/images/lakehouse1_2.jpg",
                        "/images/lakehouse1_3.jpg"
                ))
                .rating(4.97)
                .reviewCount(89)
                .latitude(31.1891)
                .longitude(120.2686)
                .hostName("刘洋")
                .featured(true)
                .propertyType("湖景房")
                .distanceFromCenter(8.7)
                .build();

        // 城市公寓
        Homestay cityApartment = Homestay.builder()
                .title("现代都市公寓 - 繁华商圈")
                .description("位于市中心的豪华公寓，周边有众多购物中心、餐厅和景点。适合商务旅行或城市探索。")
                .location("上海市静安区南京西路")
                .city("上海")
                .country("中国")
                .pricePerNight(new BigDecimal("988"))
                .maxGuests(3)
                .bedrooms(2)
                .beds(2)
                .bathrooms(1)
                .amenities(Arrays.asList("WiFi", "空调", "厨房", "洗衣机", "健身房", "电梯"))
                .images(Arrays.asList(
                        "/images/apartment1_1.jpg",
                        "/images/apartment1_2.jpg",
                        "/images/apartment1_3.jpg"
                ))
                .rating(4.82)
                .reviewCount(245)
                .latitude(31.2304)
                .longitude(121.4737)
                .hostName("陈静")
                .featured(false)
                .propertyType("公寓")
                .distanceFromCenter(0.5)
                .build();

        homestayRepository.saveAll(List.of(treehouse1, treehouse2, seasideHouse, cabin, lakehouse, cityApartment));
    }
} 