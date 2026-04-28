#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""为 host2 生成测试订单和评价数据"""

import pymysql
import random
from datetime import datetime, timedelta

DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '111111',
    'database': 'homestay_db',
    'charset': 'utf8mb4'
}

def get_connection():
    return pymysql.connect(**DB_CONFIG)

def main():
    conn = get_connection()
    cursor = conn.cursor()

    # 获取 host2 的房源
    cursor.execute('SELECT id, price FROM homestays WHERE owner_id = 2')
    host2_homestays = cursor.fetchall()
    host2_ids = [h[0] for h in host2_homestays]
    print(f'Host2 has {len(host2_ids)} homestays')

    # 获取普通用户作为 guest
    cursor.execute("SELECT id FROM users WHERE role = 'ROLE_USER' LIMIT 10")
    guest_ids = [r[0] for r in cursor.fetchall()]
    if not guest_ids:
        cursor.execute('SELECT id FROM users WHERE id != 2 LIMIT 10')
        guest_ids = [r[0] for r in cursor.fetchall()]
    print(f'Guest users: {guest_ids}')

    # 订单状态
    statuses = ['PENDING', 'CONFIRMED', 'PAID', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'CANCELLED_BY_USER']
    status_weights = [3, 2, 5, 2, 8, 2, 1]

    # 生成 30 个订单
    orders_created = 0
    for i in range(30):
        homestay_id = random.choice(host2_ids)
        cursor.execute('SELECT price FROM homestays WHERE id = %s', (homestay_id,))
        price = float(cursor.fetchone()[0])

        guest_id = random.choice(guest_ids)
        nights = random.randint(1, 5)
        total_amount = round(price * nights * random.uniform(0.9, 1.1), 2)

        days_offset = random.randint(-30, 30)
        check_in = datetime.now().date() + timedelta(days=days_offset)
        check_out = check_in + timedelta(days=nights)

        status = random.choices(statuses, weights=status_weights)[0]
        if status == 'PENDING':
            payment_status = 'UNPAID'
        elif status in ['CANCELLED', 'CANCELLED_BY_USER']:
            payment_status = 'REFUNDED'
        else:
            payment_status = 'PAID'

        order_number = 'HS' + check_in.strftime('%Y%m%d') + str(random.randint(10000, 99999))

        cursor.execute('''
            INSERT INTO orders (order_number, homestay_id, guest_id, check_in_date, check_out_date,
                nights, guest_count, price, total_amount, status, payment_status, guest_phone, created_at, updated_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
        ''', (order_number, homestay_id, guest_id, check_in, check_out, nights,
              random.randint(1,4), price, total_amount, status, payment_status,
              f'138{random.randint(10000000,99999999)}'))
        orders_created += 1

    conn.commit()
    print(f'Created {orders_created} orders for host2')

    # 生成 15 条评价
    review_contents = [
        '房间非常干净整洁，房东人很好，回复很快！',
        '位置很方便，离景点很近，下次还会再来。',
        '性价比很高，设施齐全，住得很舒服。',
        '风景太美了，早上在阳台看日出很享受。',
        '房东很热情，还推荐了很多当地美食。',
        '房间比照片上还要好，非常温馨。',
        '交通很便利，周边吃喝玩乐都很方便。',
        '床品很舒服，睡眠质量很高。',
        '房东准备的水果和零食很贴心。',
        '装修风格很有特色，拍照很出片。',
        '卫生间很干净，热水供应充足。',
        '周边环境很安静，适合休息。',
        '房东提供的旅游攻略非常实用。',
        '入住流程很顺畅，自助入住很方便。',
        '整体体验非常棒，强烈推荐！'
    ]

    cursor.execute('''
        SELECT o.id, o.homestay_id, o.guest_id
        FROM orders o
        JOIN homestays h ON o.homestay_id = h.id
        WHERE h.owner_id = 2
        ORDER BY o.created_at DESC
        LIMIT 20
    ''')
    host2_orders = cursor.fetchall()

    reviews_created = 0
    for order in host2_orders[:15]:
        order_id, homestay_id, user_id = order
        rating = random.randint(3, 5)
        content = random.choice(review_contents)
        created_at = datetime.now() - timedelta(days=random.randint(1, 30))

        cursor.execute('''
            INSERT INTO reviews (content, rating, homestay_id, order_id, user_id, created_at, updated_at, is_public, deleted)
            VALUES (%s, %s, %s, %s, %s, %s, %s, 1, 0)
        ''', (content, rating, homestay_id, order_id, user_id, created_at, created_at))
        reviews_created += 1

    conn.commit()
    print(f'Created {reviews_created} reviews for host2')

    # 统计 host2 数据
    cursor.execute('SELECT COUNT(*) FROM homestays WHERE owner_id = 2')
    print(f'\nHost2 total homestays: {cursor.fetchone()[0]}')

    cursor.execute('''
        SELECT COUNT(*) FROM orders o
        JOIN homestays h ON o.homestay_id = h.id
        WHERE h.owner_id = 2
    ''')
    print(f'Host2 total orders: {cursor.fetchone()[0]}')

    cursor.execute('''
        SELECT COUNT(*) FROM reviews r
        JOIN homestays h ON r.homestay_id = h.id
        WHERE h.owner_id = 2
    ''')
    print(f'Host2 total reviews: {cursor.fetchone()[0]}')

    cursor.close()
    conn.close()

if __name__ == '__main__':
    main()
