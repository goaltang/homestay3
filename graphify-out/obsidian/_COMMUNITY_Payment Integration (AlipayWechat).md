---
type: community
cohesion: 0.02
members: 117
---

# Payment Integration (Alipay/Wechat)

**Cohesion:** 0.02 - loosely connected
**Members:** 117 nodes

## Members
- [[.checkPaymentStatus()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\PaymentController.java
- [[.checkPaymentStatus()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\PaymentService.java
- [[.checkPaymentStatus()_3]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImpl.java
- [[.checkPaymentStatus_GatewayQueryFailed()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.checkPaymentStatus_GatewayThrowsException()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.checkPaymentStatus_NoRecord()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.checkPaymentStatus_PaidViaGateway()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.checkPaymentStatus_PaidViaRecord()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.checkPaymentStatus_Unpaid()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.createNotifyResult()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.createPayment()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\PaymentController.java
- [[.createPayment()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\gateway\PaymentGateway.java
- [[.findByOrderId()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\RefundRecordRepository.java
- [[.findByOrderIdAndRefundStatus()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\RefundRecordRepository.java
- [[.findByOutTradeNo()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\PaymentRecordRepository.java
- [[.findByOutTradeNo()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\RefundRecordRepository.java
- [[.findByRefundTradeNo()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\RefundRecordRepository.java
- [[.findByTransactionId()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\PaymentRecordRepository.java
- [[.findTopByOrderIdAndStatusOrderByCreatedAtDesc()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\PaymentRecordRepository.java
- [[.findTopByOrderIdOrderByCreatedAtDesc()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\PaymentRecordRepository.java
- [[.findTopByOrderIdOrderByCreatedAtDesc()_3]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\RefundRecordRepository.java
- [[.generateOutTradeNo()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImpl.java
- [[.generatePaymentQRCode()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\PaymentService.java
- [[.generatePaymentQRCode()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImpl.java
- [[.generatePaymentQRCode_CancelledOrder()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.generatePaymentQRCode_CompletedOrder()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.generatePaymentQRCode_GatewayError()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.generatePaymentQRCode_OrderNotFound()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.generatePaymentQRCode_QRCodeFailed()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.generatePaymentQRCode_QRCodeSuccess()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.generatePaymentQRCode_UpdatesOrderStatus()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.generateRequestId()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\util\RedisLock.java
- [[.handleNotify()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\gateway\PaymentGateway.java
- [[.handlePaymentNotify()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\PaymentService.java
- [[.handlePaymentNotify()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImpl.java
- [[.handlePaymentNotify_DuplicateSkipped()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.handlePaymentNotify_OptimisticLockConflict()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.handlePaymentNotify_RecordNotFound()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.handlePaymentNotify_Success()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.mockPaymentSuccess()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\PaymentController.java
- [[.mockRedisLockSuccess()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.mockSuccessPayment()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\PaymentService.java
- [[.mockSuccessPayment()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImpl.java
- [[.mockSuccessPayment_AlreadyPaid()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.mockSuccessPayment_CancelledOrder()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.mockSuccessPayment_CompletedOrder()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.mockSuccessPayment_OrderNotFound()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.mockSuccessPayment_Success()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.notify()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AlipayCallbackController.java
- [[.onCreate()_6]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\entity\RefundRecord.java
- [[.processRefund()_2]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\gateway\PaymentGateway.java
- [[.processRefund()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\PaymentService.java
- [[.processRefund()_3]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImpl.java
- [[.processRefund_GatewayFailed()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.processRefund_NoPaymentRecord()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.processRefund_NotPaid()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.processRefund_OrderNotFound()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.processRefund_RefundPendingStatus()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.processRefund_Success()]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.queryPayment()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\gateway\PaymentGateway.java
- [[.savePaymentRecord()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImpl.java
- [[.selectPaymentGateway()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImpl.java
- [[.setUp()_8]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[.tryLock()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\util\RedisLock.java
- [[.unlock()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\util\RedisLock.java
- [[.updateOrderPaymentStatus()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImpl.java
- [[.verifyNotify()_1]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\gateway\PaymentGateway.java
- [[AlipayCallbackController]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AlipayCallbackController.java
- [[AlipayCallbackController.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\AlipayCallbackController.java
- [[AlipayConfig]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\config\PaymentConfig.java
- [[AlipayGateway.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\gateway\AlipayGateway.java
- [[CheckPaymentStatusTests]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[GeneratePaymentQRCodeTests]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[HandlePaymentNotifyTests]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[MockSuccessPaymentTests]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[PaymentConfig]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\config\PaymentConfig.java
- [[PaymentConfig.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\config\PaymentConfig.java
- [[PaymentController]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\PaymentController.java
- [[PaymentController.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\controller\PaymentController.java
- [[PaymentGateway]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\gateway\PaymentGateway.java
- [[PaymentGateway.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\gateway\PaymentGateway.java
- [[PaymentNotifyResult]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\payment\PaymentNotifyResult.java
- [[PaymentNotifyResult.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\payment\PaymentNotifyResult.java
- [[PaymentRecord]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\entity\PaymentRecord.java
- [[PaymentRecord.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\entity\PaymentRecord.java
- [[PaymentRecordRepository]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\PaymentRecordRepository.java
- [[PaymentRecordRepository.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\PaymentRecordRepository.java
- [[PaymentRequest]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\payment\PaymentRequest.java
- [[PaymentRequest.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\payment\PaymentRequest.java
- [[PaymentResponse]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\payment\PaymentResponse.java
- [[PaymentResponse.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\payment\PaymentResponse.java
- [[PaymentService]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\PaymentService.java
- [[PaymentService.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\PaymentService.java
- [[PaymentServiceImpl]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImpl.java
- [[PaymentServiceImpl.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImpl.java
- [[PaymentServiceImplTest]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[PaymentServiceImplTest.java]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[PaymentStatus Enum]] - code - homestay-backend/docs/支付系统改造计划.md
- [[PaymentStatusResponse]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\payment\PaymentStatusResponse.java
- [[PaymentStatusResponse.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\payment\PaymentStatusResponse.java
- [[ProcessRefundTests]] - code - homestay-backend\src\test\java\com\homestay3\homestaybackend\service\impl\PaymentServiceImplTest.java
- [[RedisLock]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\util\RedisLock.java
- [[RedisLock.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\util\RedisLock.java
- [[RefundRecord]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\entity\RefundRecord.java
- [[RefundRecord.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\entity\RefundRecord.java
- [[RefundRecordRepository]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\RefundRecordRepository.java
- [[RefundRecordRepository.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\repository\RefundRecordRepository.java
- [[RefundRequest]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\refund\RefundRequest.java
- [[RefundRequest.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\refund\RefundRequest.java
- [[RefundResponse]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\refund\RefundResponse.java
- [[RefundResponse.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\dto\refund\RefundResponse.java
- [[RefundStatus()]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\model\RefundStatus.java
- [[RefundStatus.java]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\model\RefundStatus.java
- [[WechatCallbackController]] - code - homestay-backend/docs/支付系统改造详细步骤.md
- [[WechatConfig]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\config\PaymentConfig.java
- [[WechatGateway Implementation]] - code - homestay-backend/docs/支付系统改造计划.md
- [[getDescription()_6]] - code - homestay-backend\src\main\java\com\homestay3\homestaybackend\model\RefundStatus.java

## Live Query (requires Dataview plugin)

```dataview
TABLE source_file, type FROM #community/Payment_Integration_(Alipay/Wechat)
SORT file.name ASC
```

## Connections to other communities
- 36 edges to [[_COMMUNITY_Admin Controller Layer]]
- 1 edge to [[_COMMUNITY_Community 68]]

## Top bridge nodes
- [[AlipayGateway.java]] - degree 10, connects to 2 communities
- [[PaymentServiceImplTest.java]] - degree 28, connects to 1 community
- [[PaymentServiceImpl.java]] - degree 24, connects to 1 community
- [[PaymentService.java]] - degree 13, connects to 1 community
- [[RefundRequest.java]] - degree 11, connects to 1 community